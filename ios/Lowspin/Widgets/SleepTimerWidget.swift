import ActivityKit
import SwiftUI
import WidgetKit

@main
struct LowspinWidgetBundle: WidgetBundle {
    var body: some Widget {
        SleepTimerWidget()
    }
}

struct SleepTimerWidget: Widget {
    private let violet = Color(red: 0.65, green: 0.55, blue: 0.98)

    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SleepTimerAttributes.self) { context in
            VStack(alignment: .leading, spacing: 6) {
                Label("Sleep timer", systemImage: "moon.zzz")
                    .font(.caption.weight(.semibold))
                Text(context.state.label)
                    .font(.headline)
                Text(context.state.endDate, style: .timer)
                    .font(.system(size: 30, weight: .semibold, design: .rounded))
                    .monospacedDigit()
            }
            .padding()
            .activityBackgroundTint(Color(red: 0.10, green: 0.08, blue: 0.14))
            .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label("Sleep", systemImage: "moon.zzz")
                        .font(.caption)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.state.endDate, style: .timer)
                        .monospacedDigit()
                        .font(.caption.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.label)
                        .font(.caption)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("Lowspin pauses when the timer ends")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            } compactLeading: {
                Image(systemName: "moon.zzz")
                    .foregroundStyle(violet)
            } compactTrailing: {
                Text(context.state.endDate, style: .timer)
                    .monospacedDigit()
                    .font(.caption2.weight(.semibold))
            } minimal: {
                Image(systemName: "moon.zzz")
                    .foregroundStyle(violet)
            }
            .widgetURL(URL(string: "lowspin://timer"))
            .keylineTint(violet)
        }
    }
}
