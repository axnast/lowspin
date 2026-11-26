import Foundation

/// Plain, sendable metadata produced by the background scanner. Converted into
/// a SwiftData Track on the main actor.
struct TrackDraft: Sendable {
    let id: String
    let relativePath: String
    let folderBookmark: Data
    let title: String
    let artist: String
    let album: String
    let duration: Double
    let artworkData: Data?
    let dominantColor: Int
}
