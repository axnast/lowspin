import SwiftUI

struct SongRow: View {
    let track: Track
    let queue: [Track]
    let index: Int
    var playlists: [Playlist] = []

    @ObservedObject private var appearance = Appearance.shared
    @ObservedObject private var player = AudioEngine.shared

    private var isCurrent: Bool {
        player.currentTrack?.id == track.id
    }

    var body: some View {
        HStack(spacing: 12) {
            ArtworkView(data: track.artworkData, side: 44)
            VStack(alignment: .leading, spacing: 3) {
                Text(track.title)
                    .font(.body)
                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    .lineLimit(1)
                Text(track.artist)
                    .font(.caption)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    .lineLimit(1)
            }
            Spacer()
            if track.isLiked {
                Image(systemName: "heart.fill")
                    .font(.caption)
                    .foregroundColor(Color(uiColor: appearance.palette.accent))
            }
            if isCurrent && player.isPlaying {
                Image(systemName: "waveform")
                    .font(.caption)
                    .foregroundColor(Color(uiColor: appearance.palette.accent))
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            player.play(track: track, queue: queue, index: index)
        }
        .contextMenu {
            Button {
                track.isLiked.toggle()
            } label: {
                Label(
                    track.isLiked ? "Remove from Liked" : "Add to Liked",
                    systemImage: track.isLiked ? "heart.slash" : "heart"
                )
            }
            if !playlists.isEmpty {
                Menu("Add to Playlist") {
                    ForEach(playlists) { playlist in
                        Button {
                            playlist.add(track.id)
                        } label: {
                            Label(playlist.name, systemImage: "music.note.list")
                        }
                    }
                }
            }
            Button {
                track.isHidden.toggle()
            } label: {
                Label(
                    track.isHidden ? "Unhide Song" : "Hide Song",
                    systemImage: track.isHidden ? "eye" : "eye.slash"
                )
            }
        }
    }
}
