import SwiftData
import SwiftUI

struct LikedView: View {
    @Query(filter: #Predicate<Track> { $0.isLiked }, sort: \Track.title)
    private var tracks: [Track]
    @Query private var playlists: [Playlist]

    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        NavigationStack {
            Group {
                if tracks.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "heart")
                            .font(.system(size: 42))
                            .foregroundColor(Color(uiColor: appearance.palette.accent))
                        Text("No liked songs")
                            .font(.headline)
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        Text("Long-press a song and choose Add to Liked.")
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    }
                    .padding(32)
                } else {
                    List {
                        ForEach(Array(tracks.enumerated()), id: \.element.id) { index, track in
                            SongRow(track: track, queue: tracks, index: index, playlists: playlists)
                                .listRowBackground(Color(uiColor: appearance.palette.surface))
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color(uiColor: appearance.palette.background).ignoresSafeArea())
            .navigationTitle("Liked")
        }
    }
}
