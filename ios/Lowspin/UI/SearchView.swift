import SwiftData
import SwiftUI

struct SearchView: View {
    @Query(sort: \Track.title) private var tracks: [Track]
    @Query private var playlists: [Playlist]
    @State private var query = ""
    @ObservedObject private var appearance = Appearance.shared

    private var results: [Track] {
        let term = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !term.isEmpty else { return tracks }
        return tracks.filter {
            $0.title.lowercased().contains(term)
                || $0.artist.lowercased().contains(term)
                || $0.album.lowercased().contains(term)
        }
    }

    var body: some View {
        NavigationStack {
            List {
                ForEach(Array(results.enumerated()), id: \.element.id) { index, track in
                    SongRow(track: track, queue: results, index: index, playlists: playlists)
                        .listRowBackground(Color(uiColor: appearance.palette.surface))
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Color(uiColor: appearance.palette.background).ignoresSafeArea())
            .navigationTitle("Search")
            .searchable(text: $query, prompt: "Songs, artists, albums")
        }
    }
}
