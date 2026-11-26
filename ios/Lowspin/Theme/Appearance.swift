import Foundation
import SwiftUI
import UIKit

struct AccentOption: Identifiable, Hashable {
    let id: Int
    let name: String
    let color: UIColor
}

struct LowspinPalette {
    let accent: UIColor
    let background: UIColor
    let surface: UIColor
    let surfaceHigh: UIColor
    let textPrimary: UIColor
    let textSecondary: UIColor
    let onAccent: UIColor
}

@MainActor
final class Appearance: ObservableObject {
    static let shared = Appearance()

    /// Violet stays the brand mark on the launcher and vinyl in both apps.
    static let brand = UIColor(hex: 0xA78BFA)

    static let options: [AccentOption] = [
        AccentOption(id: 0, name: "Violet", color: UIColor(hex: 0xA78BFA)),
        AccentOption(id: 1, name: "Blue", color: UIColor(hex: 0x4C6FFF)),
        AccentOption(id: 2, name: "Emerald", color: UIColor(hex: 0x4FD1A5)),
        AccentOption(id: 3, name: "Amber", color: UIColor(hex: 0xF0B86E))
    ]

    @Published var accentIndex: Int {
        didSet { UserDefaults.standard.set(accentIndex, forKey: "accent") }
    }

    @Published var highContrast: Bool {
        didSet { UserDefaults.standard.set(highContrast, forKey: "high_contrast") }
    }

    @Published var reduceMotion: Bool {
        didSet { UserDefaults.standard.set(reduceMotion, forKey: "reduce_motion") }
    }

    private init() {
        let defaults = UserDefaults.standard
        accentIndex = defaults.object(forKey: "accent") as? Int ?? 0
        highContrast = defaults.bool(forKey: "high_contrast")
        reduceMotion = defaults.bool(forKey: "reduce_motion")
    }

    var palette: LowspinPalette {
        let safeIndex = min(max(accentIndex, 0), Self.options.count - 1)
        let accent = Self.options[safeIndex].color
        let base = UIColor(hex: 0x0E0B14)
        return LowspinPalette(
            accent: accent,
            background: base.lerp(to: accent, 0.06),
            surface: UIColor(hex: 0x1A1625).lerp(to: accent, 0.10),
            surfaceHigh: UIColor(hex: 0x262033).lerp(to: accent, 0.08),
            textPrimary: .white,
            textSecondary: highContrast
                ? UIColor(hex: 0xD6D6DF)
                : UIColor(hex: 0xA8A8B3),
            onAccent: base
        )
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }

    /// ARGB integer cached alongside each scanned track.
    convenience init(argb: Int) {
        self.init(
            red: CGFloat((argb >> 16) & 0xFF) / 255,
            green: CGFloat((argb >> 8) & 0xFF) / 255,
            blue: CGFloat(argb & 0xFF) / 255,
            alpha: CGFloat((argb >> 24) & 0xFF) / 255
        )
    }

    /// Linear blend used for the whole-app accent tint.
    func lerp(to other: UIColor, _ fraction: CGFloat) -> UIColor {
        var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
        var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
        getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
        other.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
        let t = min(max(fraction, 0), 1)
        return UIColor(
            red: r1 + (r2 - r1) * t,
            green: g1 + (g2 - g1) * t,
            blue: b1 + (b2 - b1) * t,
            alpha: a1 + (a2 - a1) * t
        )
    }
}

extension UIImage {
    /// Cheap downsample used for artwork thumbnails and colour extraction.
    func scaled(to maxSide: CGFloat) -> UIImage {
        let longest = max(size.width, size.height)
        guard longest > maxSide, longest > 0 else { return self }
        let ratio = maxSide / longest
        let target = CGSize(width: size.width * ratio, height: size.height * ratio)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
