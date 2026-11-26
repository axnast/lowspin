import Foundation
import SwiftData

@Model
final class Track: Identifiable {
    /// Stable across rescans: the absolute file path at import time.
    @Attribute(.unique)
    var id: String

    /// Path relative to the imported folder root.
    var relativePath: String

    /// Security-scoped bookmark of the imported folder root.
    var folderBookmark: Data

    var title: String
    var artist: String
    var album: String
    var duration: Double
    var artworkData: Data?
    var dominantColor: Int
    var dateAdded: Date
    var isLiked: Bool
    var isHidden: Bool

    init(
        id: String,
        relativePath: String,
        folderBookmark: Data,
        title: String,
        artist: String,
        album: String,
        duration: Double,
        artworkData: Data?,
        dominantColor: Int,
        dateAdded: Date = Date(),
        isLiked: Bool = false,
        isHidden: Bool = false
    ) {
        self.id = id
        self.relativePath = relativePath
        self.folderBookmark = folderBookmark
        self.title = title
        self.artist = artist
        self.album = album
        self.duration = duration
        self.artworkData = artworkData
        self.dominantColor = dominantColor
        self.dateAdded = dateAdded
        self.isLiked = isLiked
        self.isHidden = isHidden
    }

    convenience init(draft: TrackDraft) {
        self.init(
            id: draft.id,
            relativePath: draft.relativePath,
            folderBookmark: draft.folderBookmark,
            title: draft.title,
            artist: draft.artist,
            album: draft.album,
            duration: draft.duration,
            artworkData: draft.artworkData,
            dominantColor: draft.dominantColor
        )
    }
}
