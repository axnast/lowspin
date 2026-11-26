import ActivityKit
import Foundation

/// Shared between the app and the widget extension so both can see the
/// Live Activity state that drives the Dynamic Island.
struct SleepTimerAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var endDate: Date
        var label: String
    }

    var startedAt: Date
}
