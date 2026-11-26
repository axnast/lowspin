import ActivityKit
import Foundation

@MainActor
final class SleepTimer: ObservableObject {
    static let shared = SleepTimer()

    @Published private(set) var isActive = false
    @Published private(set) var endDate: Date?
    @Published private(set) var label = ""

    private var task: Task<Void, Never>?
    private var activity: Activity<SleepTimerAttributes>?

    func start(minutes: Double, label: String) {
        cancel()

        let end = Date().addingTimeInterval(minutes * 60)
        endDate = end
        self.label = label
        isActive = true

        if ActivityAuthorizationInfo().areActivitiesEnabled {
            Task { @MainActor in
                do {
                    let content = ActivityContent(
                        state: SleepTimerAttributes.ContentState(endDate: end, label: label),
                        staleDate: nil
                    )
                    activity = try await Activity.request(
                        attributes: SleepTimerAttributes(startedAt: Date()),
                        content: content,
                        pushType: nil
                    )
                } catch {
                    // Live Activities are optional; the audio timer still works.
                }
            }
        }

        task = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: UInt64(minutes * 60 * 1_000_000_000))
            } catch {
                return
            }
            guard !Task.isCancelled else { return }
            AudioEngine.shared.pause()
            finish()
        }
    }

    func cancel() {
        task?.cancel()
        task = nil
        finish()
    }

    private func finish() {
        isActive = false
        endDate = nil
        label = ""
        let current = activity
        activity = nil
        Task { @MainActor in
            await current?.end(nil, dismissalPolicy: .immediate)
        }
    }
}
