import SwiftData
import SwiftUI

struct HiddenSongsView: View {
    @Query(filter: #Predicate<Track> { $0.isHidden }, sort: \Track.title)
    private var tracks: [Track]
    @Query private var playlists: [Playlist]
    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()

            if tracks.isEmpty {
                Text("No hidden songs")
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
            } else {
                List {
                    ForEach(Array(tracks.enumerated()), id: \.element.id) { index, track in
                        VStack(alignment: .leading) {
                            SongRow(track: track, queue: tracks, index: index, playlists: playlists)
                            Button("Unhide") { track.isHidden = false }
                                .font(.caption)
                                .foregroundColor(Color(uiColor: appearance.palette.accent))
                        }
                        .listRowBackground(Color(uiColor: appearance.palette.surface))
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
        .navigationTitle("Hidden Songs")
        .navigationBarTitleDisplayMode(.inline)
    }
}
