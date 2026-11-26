import SwiftUI

struct RootView: View {
    @ObservedObject private var player = AudioEngine.shared
    @ObservedObject private var appearance = Appearance.shared
    @State private var showNowPlaying = false
    @State private var showEqualizer = false

    var body: some View {
        TabView {
            LibraryView()
                .tabItem { Label("Library", systemImage: "music.note.list") }
            SearchView()
                .tabItem { Label("Search", systemImage: "magnifyingglass") }
            LikedView()
                .tabItem { Label("Liked", systemImage: "heart") }
            PlaylistsView()
                .tabItem { Label("Playlists", systemImage: "music.note.list") }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
        .tint(Color(uiColor: appearance.palette.accent))
        .safeAreaInset(edge: .bottom) {
            if player.currentTrack != nil {
                MiniPlayer(onExpand: { showNowPlaying = true })
            }
        }
        .sheet(isPresented: $showNowPlaying) {
            NowPlayingView()
        }
        .fullScreenCover(isPresented: $showEqualizer) {
            EqualizerScreen()
                .safeAreaInset(edge: .bottom) {
                    if player.currentTrack != nil {
                        MiniPlayer(onExpand: { showEqualizer = false })
                    }
                }
        }
        .onReceive(NotificationCenter.default.publisher(for: .openEqualizer)) { _ in
            showEqualizer = true
        }
    }
}
