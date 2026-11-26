import SwiftUI

/// Brand mark stays violet regardless of the selected accent — same as the
/// launcher icon and vinyl glyph on Android.
struct VinylGlyph: View {
    var side: CGFloat = 48

    var body: some View {
        ZStack {
            Circle().fill(Color(uiColor: Appearance.brand))
            Circle()
                .stroke(Color(uiColor: Appearance.brand).opacity(0.35), lineWidth: 1)
                .padding(side * 0.12)
            Circle()
                .stroke(Color(uiColor: Appearance.brand).opacity(0.25), lineWidth: 1)
                .padding(side * 0.24)
            Circle()
                .fill(Color(uiColor: UIColor(hex: 0x0E0B14)))
                .frame(width: side * 0.28, height: side * 0.28)
        }
        .frame(width: side, height: side)
    }
}

struct ArtworkView: View {
    let data: Data?
    let side: CGFloat
    var cornerRadius: CGFloat = 6

    var body: some View {
        Group {
            if let data, let image = UIImage(data: data) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFill()
            } else {
                VinylGlyph(side: side)
            }
        }
        .frame(width: side, height: side)
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
    }
}

struct ProgressRail: View {
    let value: Double
    let duration: Double
    let onSeek: (Double) -> Void

    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        GeometryReader { geometry in
            let width = max(1, geometry.size.width)
            let ratio = duration > 0 ? min(max(0, value / duration), 1) : 0
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color(uiColor: appearance.palette.surfaceHigh))
                Capsule()
                    .fill(Color(uiColor: appearance.palette.accent))
                    .frame(width: width * ratio)
            }
            .frame(height: 4)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0, coordinateSpace: .local)
                    .onEnded { gesture in
                        let position = min(max(0, gesture.location.x / width), 1)
                        onSeek(position * duration)
                    }
            )
        }
        .frame(height: 4)
    }
}
