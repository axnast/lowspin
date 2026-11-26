import AVFoundation
import Foundation
import UIKit

/// Runs off the main actor. AVFoundation metadata loading is async so a few
/// thousand files never block scrolling.
enum LibraryScanner {
    static let supportedExtensions: Set<String> = [
        "mp3", "m4a", "aac", "alac", "flac", "wav", "aiff", "aif", "caf", "mp4", "m4b"
    ]

    static func scan(folder: URL) async -> [TrackDraft] {
        guard folder.startAccessingSecurityScopedResource() else { return [] }
        defer { folder.stopAccessingSecurityScopedResource() }

        let files = audioFiles(in: folder)

        let bookmark: Data
        do {
            bookmark = try folder.bookmarkData(
                options: [.minimalBookmark],
                includingResourceValuesForKeys: nil,
                relativeTo: nil
            )
        } catch {
            return []
        }

        var drafts: [TrackDraft] = []
        drafts.reserveCapacity(files.count)
        for file in files {
            if let draft = await makeDraft(file: file, folder: folder, bookmark: bookmark) {
                drafts.append(draft)
            }
        }
        return drafts
    }

    /// Enumeration is synchronous so it stays off the Swift 6 async-domain
    /// warning and doesn't block the main actor (scanner runs on a detached task).
    private static func audioFiles(in folder: URL) -> [URL] {
        guard let enumerator = FileManager.default.enumerator(
            at: folder,
            includingPropertiesForKeys: [.isRegularFileKey],
            options: [.skipsHiddenFiles],
            errorHandler: nil
        ) else { return [] }

        var files: [URL] = []
        while let url = enumerator.nextObject() as? URL {
            if supportedExtensions.contains(url.pathExtension.lowercased()) {
                files.append(url)
            }
        }
        return files
    }

    private static func makeDraft(file: URL, folder: URL, bookmark: Data) async -> TrackDraft? {
        let asset = AVAsset(url: file)
        guard let time = try? await asset.load(.duration), time.seconds.isFinite else { return nil }

        var title = file.deletingPathExtension().lastPathComponent
        var artist = "Unknown Artist"
        var album = ""
        var artworkData: Data?

        if let metadata = try? await asset.load(.commonMetadata) {
            for item in metadata {
                guard let key = item.commonKey else { continue }
                switch key {
                case .commonKeyTitle:
                    if let value = try? await item.load(.stringValue), !value.isEmpty { title = value }
                case .commonKeyArtist:
                    if let value = try? await item.load(.stringValue), !value.isEmpty { artist = value }
                case .commonKeyAlbumName:
                    if let value = try? await item.load(.stringValue) { album = value }
                case .commonKeyArtwork:
                    if let value = try? await item.load(.dataValue) {
                        artworkData = ArtworkProcessor.thumbnail(from: value)
                    }
                default:
                    break
                }
            }
        }

        let relativePath = file.path.replacingOccurrences(of: folder.path + "/", with: "")
        return TrackDraft(
            id: file.path,
            relativePath: relativePath,
            folderBookmark: bookmark,
            title: title,
            artist: artist,
            album: album,
            duration: time.seconds,
            artworkData: artworkData,
            dominantColor: artworkData.flatMap { ArtworkProcessor.dominantColor(from: $0) } ?? 0
        )
    }
}
