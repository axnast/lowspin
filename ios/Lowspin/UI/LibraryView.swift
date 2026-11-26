import SwiftData
import SwiftUI
import UniformTypeIdentifiers

struct LibraryView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \Track.title) private var tracks: [Track]
    @Query private var playlists: [Playlist]

    private var visibleTracks: [Track] {
        tracks.filter { !$0.isHidden }
    }

    @StateObject private var store = LibraryStore()
    @State private var showImporter = false
    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        NavigationStack {
            Group {
                if visibleTracks.isEmpty {
                    VStack(spacing: 14) {
                        VinylGlyph(side: 72)
                        Text("No music yet")
                            .font(.headline)
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        Text("Import a folder from Files to get started. Your music never leaves this iPhone.")
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                        Button("Import Folder") { showImporter = true }
                            .buttonStyle(.borderedProminent)
                            .tint(Color(uiColor: appearance.palette.accent))
                    }
                    .padding(32)
                } else {
                    List {
                        ForEach(Array(visibleTracks.enumerated()), id: \.element.id) { index, track in
                            SongRow(track: track, queue: visibleTracks, index: index, playlists: playlists)
                                .listRowBackground(Color(uiColor: appearance.palette.surface))
                        }
                    }
                    .listStyle(.plain)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color(uiColor: appearance.palette.background).ignoresSafeArea())
            .navigationTitle("Library")
            .toolbar {
                Button {
                    showImporter = true
                } label: {
                    Image(systemName: "folder.badge.plus")
                }
            }
            .fileImporter(
                isPresented: $showImporter,
                allowedContentTypes: [.folder],
                allowsMultipleSelection: true
            ) { result in
                if case .success(let urls) = result {
                    Task { await store.importFolders(from: urls, context: context) }
                }
            }
            .overlay {
                if store.isScanning {
                    ZStack {
                        Color.black.opacity(0.55).ignoresSafeArea()
                        VStack(spacing: 12) {
                            ProgressView()
                                .tint(Color(uiColor: appearance.palette.accent))
                            Text(store.status)
                                .font(.subheadline)
                                .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        }
                        .padding(24)
                        .background(Color(uiColor: appearance.palette.surfaceHigh))
                        .cornerRadius(16)
                    }
                }
            }
            .alert(
                "Import failed",
                isPresented: Binding(
                    get: { store.lastError != nil },
                    set: { if !$0 { store.lastError = nil } }
                )
            ) {
                Button("OK", role: .cancel) {}
            } message: {
                Text(store.lastError ?? "")
            }
        }
    }
}
