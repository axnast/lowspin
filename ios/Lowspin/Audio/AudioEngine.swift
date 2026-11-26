import AVFoundation
import Combine
import Foundation
import MediaPlayer
import SwiftData
import UIKit

enum PlaybackError: LocalizedError {
    case folderAccessDenied

    var errorDescription: String? {
        switch self {
        case .folderAccessDenied:
            return "The imported folder is no longer accessible. Import it again."
        }
    }
}

/// Single audio graph for the whole app:
///   AVAudioPlayerNode -> 5-band AVAudioUnitEQ -> main mixer -> output
/// Wiring MPNowPlayingInfoCenter + MPRemoteCommandCenter is what makes iOS
/// render the Now Playing module in the Dynamic Island and on the Lock Screen.
@MainActor
final class AudioEngine: ObservableObject {
    static let shared = AudioEngine()

    @Published private(set) var currentTrack: Track?
    @Published private(set) var isPlaying = false
    @Published private(set) var elapsed: TimeInterval = 0
    @Published private(set) var duration: TimeInterval = 0
    @Published private(set) var errorMessage: String?

    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let equalizer = AVAudioUnitEQ(numberOfBands: 5)

    private var audioFile: AVAudioFile?
    private var queue: [Track] = []
    private var queueIndex = 0
    private var startFrame: AVAudioFramePosition = 0
    private var activeFolderURL: URL?
    /// Invalidates completion handlers from a segment we stopped or seeked away from.
    private var playbackToken = 0
    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()

    private init() {
        configureSession()
        buildGraph()
        configureRemoteCommands()
        observeInterruptions()

        EqualizerSettings.shared.objectWillChange
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                Task { @MainActor in self?.applyEqualizer() }
            }
            .store(in: &cancellables)

        startTicker()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
        timer?.invalidate()
    }

    // MARK: - Setup

    private func configureSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [])
            try session.setActive(true)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func buildGraph() {
        let sampleRate = AVAudioSession.sharedInstance().sampleRate
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 2)
            ?? AVAudioFormat(standardFormatWithSampleRate: 48_000, channels: 2)!

        engine.attach(player)
        engine.attach(equalizer)
        engine.connect(player, to: equalizer, format: format)
        engine.connect(equalizer, to: engine.mainMixerNode, format: format)
        applyEqualizer()

        do {
            try engine.start()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func applyEqualizer() {
        let settings = EqualizerSettings.shared
        let gains = settings.levels(for: 5)
        for (index, band) in equalizer.bands.enumerated() {
            band.filterType = .parametric
            band.frequency = Float(EqualizerSettings.frequencies[index])
            band.bandwidth = 1.0
            band.gain = Float(gains[index])
            band.bypass = !settings.enabled
        }
        equalizer.bypass = !settings.enabled
    }

    private func configureRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.resume() }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.pause() }
            return .success
        }
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.toggle() }
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.next() }
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            Task { @MainActor in self?.previous() }
            return .success
        }
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            Task { @MainActor in self?.seek(to: event.positionTime) }
            return .success
        }
    }

    private func observeInterruptions() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleInterruption(_:)),
            name: AVAudioSession.interruptionNotification,
            object: nil
        )
    }

    @objc private func handleInterruption(_ notification: Notification) {
        // Read the raw numbers so this works regardless of how Swift imports
        // the NS_ENUM raw value type (Int vs UInt) in a given SDK.
        guard let info = notification.userInfo,
              let type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.intValue
        else { return }

        if type == 1 { // began
            pause()
        } else if type == 2 { // ended
            let options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.intValue ?? 0
            if (options & 1) != 0 { // shouldResume
                try? AVAudioSession.sharedInstance().setActive(true)
                resume()
            }
        }
    }

    private func startTicker() {
        timer = Timer.scheduledTimer(withTimeInterval: 0.25, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.tick() }
        }
    }

    // MARK: - Playback

    func play(track: Track, queue: [Track], index: Int) {
        self.queue = queue
        queueIndex = index
        do {
            try start(track: track, at: 0)
            isPlaying = true
            publishNowPlaying()
        } catch {
            errorMessage = error.localizedDescription
            isPlaying = false
        }
    }

    private func start(track: Track, at time: TimeInterval) throws {
        player.stop()
        audioFile = nil
        releaseFolderAccess()

        var stale = false
        let folder = try URL(
            resolvingBookmarkData: track.folderBookmark,
            options: [.withSecurityScope],
            relativeTo: nil,
            bookmarkDataIsStale: &stale
        )
        guard folder.startAccessingSecurityScopedResource() else {
            throw PlaybackError.folderAccessDenied
        }
        activeFolderURL = folder

        let fileURL = folder.appendingPathComponent(track.relativePath)
        let file = try AVAudioFile(forReading: fileURL)
        let rate = file.fileFormat.sampleRate
        let firstFrame = AVAudioFramePosition(max(0, time * rate))
        let frameCount = AVAudioFrameCount(max(1, file.length - firstFrame))

        audioFile = file
        startFrame = firstFrame
        currentTrack = track
        duration = Double(file.length) / rate
        elapsed = time

        playbackToken += 1
        let token = playbackToken
        player.scheduleSegment(
            file,
            startingFrame: firstFrame,
            frameCount: frameCount,
            at: nil
        ) { [weak self] in
            Task { @MainActor in
                guard let self, self.playbackToken == token else { return }
                self.trackDidFinish()
            }
        }
        player.play()
    }

    private func trackDidFinish() {
        guard !queue.isEmpty else {
            isPlaying = false
            return
        }
        queueIndex = (queueIndex + 1) % queue.count
        do {
            try start(track: queue[queueIndex], at: 0)
            publishNowPlaying()
        } catch {
            errorMessage = error.localizedDescription
            isPlaying = false
        }
    }

    func toggle() {
        if isPlaying { pause() } else { resume() }
    }

    func pause() {
        guard isPlaying else { return }
        player.pause()
        isPlaying = false
        publishNowPlaying()
    }

    func resume() {
        guard audioFile != nil else { return }
        guard !isPlaying else { return }
        player.play()
        isPlaying = true
        publishNowPlaying()
    }

    func seek(to time: TimeInterval) {
        guard let track = currentTrack else { return }
        let clamped = min(max(0, time), duration)
        do {
            try start(track: track, at: clamped)
            publishNowPlaying()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func next() {
        guard !queue.isEmpty else { return }
        queueIndex = (queueIndex + 1) % queue.count
        do {
            try start(track: queue[queueIndex], at: 0)
            isPlaying = true
            publishNowPlaying()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func previous() {
        guard !queue.isEmpty else { return }
        // Restart the current track first, like every other player.
        if elapsed > 3 {
            seek(to: 0)
            return
        }
        queueIndex = (queueIndex - 1 + queue.count) % queue.count
        do {
            try start(track: queue[queueIndex], at: 0)
            isPlaying = true
            publishNowPlaying()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func stop() {
        playbackToken += 1
        player.stop()
        audioFile = nil
        currentTrack = nil
        isPlaying = false
        elapsed = 0
        duration = 0
        releaseFolderAccess()
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    private func releaseFolderAccess() {
        activeFolderURL?.stopAccessingSecurityScopedResource()
        activeFolderURL = nil
    }

    private func tick() {
        guard let file = audioFile,
              let nodeTime = player.lastRenderTime,
              let playerTime = player.playerTime(forNodeTime: nodeTime)
        else { return }
        let seconds = Double(startFrame + playerTime.sampleTime) / file.fileFormat.sampleRate
        elapsed = min(max(0, seconds), duration)
        if isPlaying { publishNowPlaying() }
    }

    // MARK: - Dynamic Island / Lock Screen

    private func publishNowPlaying() {
        guard let track = currentTrack else {
            MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
            return
        }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: track.title,
            MPMediaItemPropertyArtist: track.artist,
            MPMediaItemPropertyAlbumTitle: track.album,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: elapsed,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? 1.0 : 0.0,
            MPNowPlayingInfoPropertyMediaType: MPMediaType.audio.rawValue
        ]
        if let data = track.artworkData, let image = UIImage(data: data) {
            info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }
}
