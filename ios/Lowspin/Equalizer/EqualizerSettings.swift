import Foundation

/// The exact five-band table agreed for Lowspin, shared with the Android app.
/// Stored locally; no network, no account.
@MainActor
final class EqualizerSettings: ObservableObject {
    static let shared = EqualizerSettings()

    static let frequencies: [Int] = [60, 230, 910, 3600, 14000]

    static let builtIn: [String] = ["classical", "heavym", "dance", "rock"]

    static let table: [String: [Int]] = [
        "flat": [0, 0, 0, 0, 0],
        "classical": [3, 2, -1, 2, 4],
        "heavym": [5, 2, -4, 4, 5],
        "dance": [7, 1, -2, 3, 6],
        "rock": [5, 3, -2, 4, 4]
    ]

    @Published var enabled: Bool {
        didSet { UserDefaults.standard.set(enabled, forKey: "eq.enabled") }
    }

    @Published var presetId: String {
        didSet { UserDefaults.standard.set(presetId, forKey: "eq.preset") }
    }

    /// One edit-in-place custom slot, same model as the Android app.
    @Published var customLevels: [Int] {
        didSet {
            UserDefaults.standard.set(
                customLevels.map(String.init).joined(separator: ","),
                forKey: "eq.custom"
            )
        }
    }

    @Published private var hasCustom: Bool {
        didSet { UserDefaults.standard.set(hasCustom, forKey: "eq.custom.exists") }
    }

    private init() {
        let defaults = UserDefaults.standard
        enabled = defaults.bool(forKey: "eq.enabled")
        presetId = defaults.string(forKey: "eq.preset") ?? "flat"
        hasCustom = defaults.bool(forKey: "eq.custom.exists")
        let parsed = (defaults.string(forKey: "eq.custom") ?? "")
            .split(separator: ",")
            .compactMap { Int($0) }
        customLevels = parsed.count == 5 ? parsed : [0, 0, 0, 0, 0]
    }

    nonisolated static func name(_ id: String) -> String {
        switch id {
        case "classical": return "Classical"
        case "heavym": return "Heavy M"
        case "dance": return "Dance"
        case "rock": return "Rock"
        case "custom": return "Custom"
        default: return "Flat"
        }
    }

    func levels(for bandCount: Int) -> [Int] {
        let source: [Int]
        if presetId == "custom" {
            source = customLevels
        } else {
            source = Self.table[presetId] ?? Self.table["flat"]!
        }
        guard bandCount == 5 else {
            // AVAudioUnitEQ is always configured with five bands in this app.
            // Fall back to repeating the last gain if a caller asks otherwise.
            return (0..<bandCount).map { index in
                source[min(index, source.count - 1)]
            }
        }
        return source
    }

    func select(_ id: String) {
        if id == "custom" && !hasCustom {
            customLevels = levels(for: 5)
            hasCustom = true
        }
        presetId = id
    }

    func setBand(_ index: Int, gain: Int) {
        guard index >= 0, index < 5 else { return }
        if presetId != "custom" {
            // Start editing from what is currently on screen, not a stale slot.
            customLevels = levels(for: 5)
            hasCustom = true
            presetId = "custom"
        }
        customLevels[index] = max(-12, min(12, gain))
    }
}
