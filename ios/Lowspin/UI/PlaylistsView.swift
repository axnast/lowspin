import SwiftData
import SwiftUI

struct PlaylistsView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \Playlist.createdAt, order: .reverse) private var playlists: [Playlist]
    @State private var showingNewPlaylist = false
    @State private var newName = ""
    @ObservedObject private var appearance = Appearance.shared

    var body: some View {
        NavigationStack {
            Group {
                if playlists.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "music.note.list")
                            .font(.system(size: 42))
                            .foregroundColor(Color(uiColor: appearance.palette.accent))
                        Text("No playlists yet")
                            .font(.headline)
                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                        Text("Create one, then long-press songs to add them.")
                            .font(.subheadline)
                            .multilineTextAlignment(.center)
                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                    }
                    .padding(32)
                } else {
                    List {
                        ForEach(playlists) { playlist in
                            NavigationLink {
                                PlaylistDetailView(playlist: playlist)
                            } label: {
                                HStack {
                                    Image(systemName: "music.note.list")
                                        .foregroundColor(Color(uiColor: appearance.palette.accent))
                                    VStack(alignment: .leading) {
                                        Text(playlist.name)
                                            .foregroundColor(Color(uiColor: appearance.palette.textPrimary))
                                        Text("\(playlist.trackIDs.count) songs")
                                            .font(.caption)
                                            .foregroundColor(Color(uiColor: appearance.palette.textSecondary))
                                    }
                                }
                            }
                            .listRowBackground(Color(uiColor: appearance.palette.surface))
                        }
                        .onDelete { offsets in
                            for index in offsets { context.delete(playlists[index]) }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(Color(uiColor: appearance.palette.background).ignoresSafeArea())
            .navigationTitle("Playlists")
            .toolbar {
                Button { showingNewPlaylist = true } label: {
                    Image(systemName: "plus")
                }
            }
            .alert("New Playlist", isPresented: $showingNewPlaylist) {
                TextField("Name", text: $newName)
                Button("Cancel", role: .cancel) { newName = "" }
                Button("Create") {
                    context.insert(Playlist(name: newName.isEmpty ? "Untitled" : newName))
                    newName = ""
                }
            }
        }
    }
}
