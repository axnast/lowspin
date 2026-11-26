import SwiftUI

struct NowPlayingView: View {
    @ObservedObject private var player = AudioEngine.shared
    @ObservedObject private var appearance = Appearance.shared
    @Environment(\.dismiss) private var dismiss
    @State private var showLyrics = false
    @State private var showSleepTimer = false

    private func timeString(_ seconds: TimeInterval) -> String {
        let total = Int(max(0, seconds))
        return String(format: "%d:%02d", total / 60, total % 60)
    }

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background)
                .ignoresSafeArea()

            if let track = player.currentTrack {
                VStack(spacing: 20) {
                    HStack {
                        Button { dismiss() } label: {
                            Image(systemName: "xmark")
                                .font(.title3)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                                .frame(width: 40, height: 40)
                        }
                        .buttonStyle(.plain)
                        Spacer()
                        Text("Now Playing")
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        Spacer()
                        Color.clear.frame(width: 40, height: 40)
                    }

                    Spacer(minLength: 0)

                    ZStack {
                        if track.dominantColor != 0 {
                            Color(uiColor: UIColor(argb: track.dominantColor))
                                .opacity(0.35)
                                .blur(radius: 46)
                        }
                        ArtworkView(data: track.artworkData, side: 268, cornerRadius: 14)
                    }
                    .frame(height: 288)

                    VStack(spacing: 5) {
                        Text(track.title)
                            .font(.title3.weight(.semibold))
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                            .lineLimit(1)
                        Text(track.artist)
                            .font(.subheadline)
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            .lineLimit(1)
                    }

                    VStack(spacing: 6) {
                        ProgressRail(
                            value: player.elapsed,
                            duration: player.duration
                        ) { target in
                            player.seek(to: target)
                        }
                        HStack {
                            Text(timeString(player.elapsed))
                            Spacer()
                            Text(timeString(player.duration))
                        }
                        .font(.caption.monospacedDigit())
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    }

                    HStack(spacing: 34) {
                        Button { player.previous() } label: {
                            Image(systemName: "backward.fill")
                                .font(.title2)
                        }
                        Button { player.toggle() } label: {
                            Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                .font(.system(size: 56))
                                .foregroundColor(Color(uiColor: appearance.palette.accent))
                        }
                        Button { player.next() } label: {
                            Image(systemName: "forward.fill")
                                .font(.title2)
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))

                    HStack(spacing: 36) {
                        Button { showSleepTimer = true } label: {
                            Image(systemName: "moon.zzz")
                                .font(.title3)
                        }
                        Button {
                            dismiss()
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                                NotificationCenter.default.post(name: .openEqualizer, object: nil)
                            }
                        } label: {
                            Image(systemName: "slider.horizontal.3")
                                .font(.title3)
                        }
                        Button { showLyrics = true } label: {
                            Image(systemName: "quote.bubble")
                                .font(.title3)
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))

                    Text("Playing controls also appear in the Dynamic Island")
                        .font(.caption2)
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        .opacity(0.7)

                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 16)
            } else {
                VStack(spacing: 12) {
                    VinylGlyph(side: 80)
                    Text("Nothing playing")
                        .font(.headline)
                        .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    Button("Close") { dismiss() }
                        .buttonStyle(.borderedProminent)
                        .tint(Color(uiColor: appearance.palette.accent))
                }
            }
        }
        .sheet(isPresented: $showLyrics) {
            if let track = player.currentTrack {
                LyricsView(track: track)
            }
        }
        .sheet(isPresented: $showSleepTimer) {
            SleepTimerSheet()
        }
    }
}
