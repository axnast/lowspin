import SwiftData
import SwiftUI

struct PlaylistDetailView: View {
    let playlist: Playlist

    @Query(sort: \Track.title) private var allTracks: [Track]
    @Query private var playlists: [Playlist]
    @ObservedObject private var appearance = Appearance.shared

    private var tracks: [Track] {
        let byID = Dictionary(uniqueKeysWithValues: allTracks.map { ($0.id, $0) })
        return playlist.trackIDs.compactMap { byID[$0] }
    }

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()

            if tracks.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "music.note")
                        .font(.system(size: 40))
                        .foregroundColor(Color(uiColor: appearance.palette.accent))
                    Text("This playlist is empty")
                        .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    Text("Long-press a song in Library to add it here.")
                        .font(.caption)
                        .multilineTextAlignment(.center)
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                }
                .padding()
            } else {
                List {
                    ForEach(Array(tracks.enumerated()), id: \.element.id) { index, track in
                        SongRow(track: track, queue: tracks, index: index, playlists: playlists)
                            .listRowBackground(Color(uiColor: appearance.palette.surface))
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    playlist.remove(track.id)
                                } label: {
                                    Label("Remove", systemImage: "trash")
                                }
                            }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
        .navigationTitle(playlist.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
