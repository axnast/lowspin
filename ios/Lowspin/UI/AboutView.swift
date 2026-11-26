import SwiftUI

struct AboutView: View {
    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()
            VStack(spacing: 14) {
                VinylGlyph(side: 88)
                Text("Lowspin")
                    .font(.title.weight(.semibold))
                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                Text("A quiet, offline music player")
                    .font(.subheadline)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))

                Divider().padding(.vertical, 8)

                Text("built by axnast")
                    .font(.footnote)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                Text("No network access. No analytics. Your music stays on this iPhone.")
                    .font(.caption)
                    .multilineTextAlignment(.center)
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    .padding(.horizontal, 32)
            }
            .navigationTitle("About")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
