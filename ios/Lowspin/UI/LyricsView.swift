import SwiftUI

struct LyricsView: View {
    let track: Track

    @ObservedObject private var player = AudioEngine.shared
    @ObservedObject private var appearance = Appearance.shared
    @Environment(\.dismiss) private var dismiss
    @State private var result: LyricsResult = .none
    @State private var activeLine = 0

    private func updateActive(_ time: TimeInterval) {
        guard case .synced(let lines) = result, !lines.isEmpty else { return }
        var found = 0
        for (index, line) in lines.enumerated() where line.time <= time { found = index }
        if found != activeLine { activeLine = found }
    }

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()

            VStack(spacing: 12) {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            .frame(width: 40, height: 40)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Text("Lyrics")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    Spacer()
                    Color.clear.frame(width: 40)
                }

                Text(track.title)
                    .font(.headline)
                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))

                ScrollViewReader { proxy in
                    ScrollView {
                        switch result {
                        case .synced(let lines):
                            LazyVStack(alignment: .leading, spacing: 16) {
                                ForEach(Array(lines.enumerated()), id: \.element) { index, line in
                                    Text(line.text)
                                        .font(index == activeLine ? .title3.weight(.semibold) : .body)
                                        .foregroundColor(index == activeLine
                                            ? Color(uiColor: appearance.palette.accent)
                                            : Color(uiColor: appearance.palette.textSecondary))
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .id(index)
                                }
                            }
                            .padding()
                            .onChange(of: activeLine) { _, new in
                                withAnimation(appearance.reduceMotion ? nil : .easeOut(duration: 0.22)) {
                                    proxy.scrollTo(new, anchor: .center)
                                }
                            }
                        case .plain(let text):
                            Text(text)
                                .font(.body)
                                .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding()
                        case .none:
                            VStack(spacing: 10) {
                                Image(systemName: "quote.bubble")
                                    .font(.system(size: 36))
                                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                                Text("No embedded lyrics in this file")
                                    .font(.subheadline)
                                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.top, 80)
                        }
                    }
                }
            }
            .padding(.horizontal, 12)
        }
        .onAppear { result = LyricsLoader.lyrics(for: track) }
        .onChange(of: player.elapsed) { _, newValue in updateActive(newValue) }
    }
}
