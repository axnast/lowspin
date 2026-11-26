import Foundation
import SwiftData

@Model
final class Playlist: Identifiable {
    @Attribute(.unique)
    var id: String

    var name: String
    var trackIDsData: Data
    var createdAt: Date

    /// Stable track IDs are stored as JSON so no many-to-many relationship
    /// ordering has to be reconstructed on every launch.
    var trackIDs: [String] {
        get {
            (try? JSONDecoder().decode([String].self, from: trackIDsData)) ?? []
        }
        set {
            trackIDsData = (try? JSONEncoder().encode(newValue)) ?? Data()
        }
    }

    init(id: String = UUID().uuidString, name: String, trackIDs: [String] = []) {
        self.id = id
        self.name = name
        self.trackIDsData = (try? JSONEncoder().encode(trackIDs)) ?? Data()
        self.createdAt = Date()
    }

    func add(_ trackID: String) {
        guard !trackIDs.contains(trackID) else { return }
        var ids = trackIDs
        ids.append(trackID)
        trackIDs = ids
    }

    func remove(_ trackID: String) {
        var ids = trackIDs
        ids.removeAll { $0 == trackID }
        trackIDs = ids
    }
}
