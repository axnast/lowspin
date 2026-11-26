import SwiftData
import SwiftUI

struct FoldersView: View {
    @Environment(\.modelContext) private var context
    @Query private var tracks: [Track]
    @StateObject private var store = LibraryStore()
    @ObservedObject private var appearance = Appearance.shared

    private struct FolderInfo: Identifiable {
        let bookmark: Data
        let name: String
        let count: Int
        var id: Data { bookmark }
    }

    private var folders: [FolderInfo] {
        Dictionary(grouping: tracks, by: \.folderBookmark)
            .map { FolderInfo(bookmark: $0.key, name: folderName(for: $0.key), count: $0.value.count) }
            .sorted { $0.name < $1.name }
    }

    private func resolvedURL(for bookmark: Data) -> URL? {
        var stale = false
        return try? URL(
            resolvingBookmarkData: bookmark,
            options: [.withSecurityScope],
            relativeTo: nil,
            bookmarkDataIsStale: &stale
        )
    }

    private func folderName(for bookmark: Data) -> String {
        resolvedURL(for: bookmark)?.lastPathComponent ?? "Unknown folder"
    }

    var body: some View {
        ZStack {
            Color(uiColor: appearance.palette.background).ignoresSafeArea()

            if folders.isEmpty {
                Text("No imported folders")
                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
            } else {
                List {
                    ForEach(folders) { folder in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Image(systemName: "folder.fill")
                                    .foregroundColor(Color(uiColor: appearance.palette.accent))
                                Text(folder.name)
                                    .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                                Spacer()
                                Text("\(folder.count) songs")
                                    .font(.caption)
                                    .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                            }
                            HStack(spacing: 16) {
                                Button("Rescan") {
                                    guard let url = resolvedURL(for: folder.bookmark) else { return }
                                    Task { await store.importFolders(from: [url], context: context) }
                                }
                                Button("Remove", role: .destructive) {
                                    for track in tracks where track.folderBookmark == folder.bookmark {
                                        context.delete(track)
                                    }
                                }
                            }
                            .font(.caption)
                        }
                        .listRowBackground(Color(uiColor: appearance.palette.surface))
                    }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
            }
        }
        .navigationTitle("Folders")
        .navigationBarTitleDisplayMode(.inline)
        .overlay {
            if store.isScanning {
                ZStack {
                    Color.black.opacity(0.5).ignoresSafeArea()
                    ProgressView(store.status)
                        .padding(20)
                        .background(Color(uiColor: appearance.palette.surfaceHigh))
                        .cornerRadius(14)
                }
            }
        }
    }
}
