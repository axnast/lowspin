import Foundation
import SwiftData

@MainActor
final class LibraryStore: ObservableObject {
    @Published var isScanning = false
    @Published var status = ""
    @Published var lastError: String?

    func importFolders(from urls: [URL], context: ModelContext) async {
        let folders = urls.filter { $0.startAccessingSecurityScopedResource() }
        guard !folders.isEmpty else { return }
        defer { folders.forEach { $0.stopAccessingSecurityScopedResource() } }

        isScanning = true
        defer { isScanning = false }

        do {
            let known = Set(try context.fetch(FetchDescriptor<Track>()).map(\.id))
            var seen = known

            for folder in folders {
                status = "Reading \(folder.lastPathComponent)…"
                let drafts = await Task.detached(priority: .userInitiated) {
                    await LibraryScanner.scan(folder: folder)
                }.value

                var added = 0
                for draft in drafts where !seen.contains(draft.id) {
                    seen.insert(draft.id)
                    context.insert(Track(draft: draft))
                    added += 1
                }
                try context.save()
                status = "\(drafts.count) found · \(added) new"
            }
        } catch {
            lastError = error.localizedDescription
            status = "Import failed"
        }
    }
}
