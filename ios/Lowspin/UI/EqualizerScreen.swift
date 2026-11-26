import SwiftUI

extension Notification.Name {
    static let openEqualizer = Notification.Name("lowspin.openEqualizer")
}

private enum EqualizerTab: Identifiable {
    case builtIn(String)
    case custom

    var id: String {
        switch self {
        case .builtIn(let name): return name
        case .custom: return "custom"
        }
    }

    var label: String {
        switch self {
        case .builtIn(let name): return EqualizerSettings.name(name).uppercased()
        case .custom: return "CUSTOM"
        }
    }
}

struct EqualizerScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var equalizer = EqualizerSettings.shared
    @ObservedObject private var appearance = Appearance.shared

    private var tabs: [EqualizerTab] {
        EqualizerSettings.builtIn.map { EqualizerTab.builtIn($0) } + [.custom]
    }

    private var gains: [Int] {
        equalizer.levels(for: 5)
    }

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()

            VStack(spacing: 0) {
                HStack {
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.title3)
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            .frame(width: 40, height: 40)
                    }
                    .buttonStyle(.plain)
                    Text("Equaliser")
                        .font(.title3.weight(.semibold))
                        .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                    Spacer()
                }
                .padding(.horizontal, 16)

                Button {
                    equalizer.enabled.toggle()
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: equalizer.enabled ? "checkmark.square.fill" : "square")
                            .font(.title3)
                            .foregroundColor(Color(uiColor: appearance.palette.accent))
                        Text("Equaliser")
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 0) {
                        ForEach(tabs) { tab in
                            VStack(spacing: 6) {
                                Text(tab.label)
                                    .font(.caption.weight(.semibold))
                                    .foregroundColor(
                                        equalizer.presetId == tab.id
                                            ? Color(uiColor: appearance.palette.textPrimary)
                                            : Color(uiColor: appearance.palette.textSecondary)
                                    )
                                    .padding(.horizontal, 12)
                                Rectangle()
                                    .fill(equalizer.presetId == tab.id
                                          ? Color(uiColor: appearance.palette.accent)
                                          : Color.clear)
                                    .frame(height: 2)
                            }
                            .contentShape(Rectangle())
                            .onTapGesture { equalizer.select(tab.id) }
                        }
                    }
                }
                .padding(.horizontal, 8)

                GeometryReader { geometry in
                    HStack(spacing: 0) {
                        ForEach(0..<5, id: \.self) { index in
                            VerticalBandSlider(
                                index: index,
                                gain: gains[index],
                                onChange: { equalizer.setBand(index, gain: $0) }
                            )
                            .frame(width: geometry.size.width / 5)
                        }
                    }
                    .padding(.horizontal, 4)
                }
                .padding(.top, 8)
            }
        }
    }
}

private struct VerticalBandSlider: View {
    let index: Int
    let gain: Int
    let onChange: (Int) -> Void

    @ObservedObject private var appearance = Appearance.shared

    private static let labels = [
        "60 Hz (Bass)",
        "230 Hz (Low-Mid)",
        "910 Hz (Mid)",
        "3.6 kHz (Presence)",
        "14 kHz (Treble)"
    ]

    var body: some View {
        VStack(spacing: 6) {
            GeometryReader { geometry in
                let height = geometry.size.height
                let top = height * 0.08
                let bottom = height * 0.92
                let fraction = CGFloat(gain + 12) / 24
                let thumbY = bottom - min(max(fraction, 0), 1) * (bottom - top)

                ZStack(alignment: .top) {
                    Canvas { context, size in
                        let center = size.width / 2
                        let guide = Color(uiColor: appearance.palette.textSecondary)

                        for (position, opacity) in [(0.0, 0.25), (0.5, 0.5), (1.0, 0.25)] {
                            let y = top + (bottom - top) * CGFloat(position)
                            var line = Path()
                            line.move(to: CGPoint(x: 8, y: y))
                            line.addLine(to: CGPoint(x: size.width - 8, y: y))
                            context.stroke(line, with: .color(guide.opacity(opacity)), lineWidth: 1)
                        }

                        var rail = Path()
                        rail.move(to: CGPoint(x: center, y: top))
                        rail.addLine(to: CGPoint(x: center, y: bottom))
                        context.stroke(rail, with: .color(guide.opacity(0.4)), lineWidth: 3)

                        context.fill(
                            Path(ellipseIn: CGRect(x: center - 11, y: thumbY - 11, width: 22, height: 22)),
                            with: .color(Color(uiColor: appearance.palette.accent))
                        )
                    }

                    Text("+12dB")
                        .font(.system(size: 9))
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        .offset(y: top - 7)
                    Text("0dB")
                        .font(.system(size: 9))
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        .offset(y: height * 0.5 - 7)
                    Text("-12dB")
                        .font(.system(size: 9))
                        .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        .offset(y: bottom - 7)
                }
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0, coordinateSpace: .local)
                        .onChanged { value in apply(value.location.y, height) }
                        .onEnded { value in apply(value.location.y, height) }
                )
            }

            Text(Self.labels[index])
                .font(.caption2)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)
                .lineLimit(2)
                .frame(height: 34)
                .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
        }
    }

    private func apply(_ y: CGFloat, _ height: CGFloat) {
        let top = height * 0.08
        let usable = height * 0.84
        let ratio = min(max(1 - (y - top) / usable, 0), 1)
        onChange(Int(round(-12 + ratio * 24)))
    }
}
