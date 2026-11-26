import SwiftData
import SwiftUI

@main
struct LowspinApp: App {
    private let container: ModelContainer

    init() {
        do {
            container = try ModelContainer(for: Track.self, Playlist.self)
        } catch {
            fatalError("Could not create the Lowspin library store: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
        .modelContainer(container)
    }
}
