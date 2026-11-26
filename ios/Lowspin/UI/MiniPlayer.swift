import SwiftUI

struct MiniPlayer: View {
    let onExpand: () -> Void

    @ObservedObject private var player = AudioEngine.shared
    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        HStack(spacing: 10) {
            ArtworkView(data: player.currentTrack?.artworkData, side: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(player.currentTrack?.title ?? "")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    .lineLimit(1)
                Text(player.currentTrack?.artist ?? "")
                    .font(.caption)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    .lineLimit(1)
            }
            Spacer(minLength: 4)
            Button {
                player.toggle()
            } label: {
                Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                    .font(.title3)
                    .foregroundColor(Color(uiColor: appearance.palette.accent))
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)

            Button(action: onExpand) {
                Image(systemName: "chevron.up")
                    .font(.footnote.weight(.semibold))
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    .frame(width: 24, height: 40)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color(uiColor: appearance.palette.surfaceHigh))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(Color(uiColor: appearance.palette.accent).opacity(0.25))
                .frame(height: 0.5)
        }
    }
}
