# Lowspin — Complete Architecture & Implementation Plan

> **Status**: Planning Document Only (no codebase files generated yet)  
> **Target Directory**: `D:\oc1`  
> **App Name**: Lowspin (`com.lowspin.app`)  
> **Goal**: 100% offline local music player for Android (Kotlin + Jetpack Compose + Material 3), built cleanly in verifiable stages.

---

## 0. Brand Override (supersedes earlier Spotify-inspired design notes)

- App name **Lowspin**, package `com.lowspin.app`, wordmark **lowspin** (lowercase, medium weight).
- Tagline (About only): **"Your music. No signal."**
- Dark-only palette: background `#0E0B14`, surface `#1A1625`, surfaceHigh `#262033`, accent `#A78BFA`, accentPressed `#7C5CD6`, onAccent `#0E0B14`, textPrimary `#FFFFFF`, textSecondary `#A8A8B3`, error `#FF6B6B`. No Spotify green, no pure black.
- Accent is restricted to play/pause, active bottom-nav item, seek progress, liked heart, selected chips, and shuffle/repeat active state.
- Flat vinyl logo (launcher + notification), SplashScreen API, slow 3 s rotating vinyl for loading and missing art, ease-out 200–300 ms motion with 0.96 press scale.
- Font remains the bundled Plus Jakarta Sans with sans-serif fallback; weights 400 body / 600 titles / 800 large headings.
- Earlier references to Spotify colors/layout in this document are historical and no longer apply.

---

## 1. Hard Constraints & Non-Negotiables

1. **100% Offline Only**
   - **Zero** `INTERNET` or `ACCESS_NETWORK_STATE` permissions in `AndroidManifest.xml`.
   - Enforce with `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` and `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />` in `AndroidManifest.xml` so no transitive dependency can ever inject them during manifest merging.
   - **Zero** network libraries (no Retrofit, OkHttp, Ktor, Coil network fetchers, analytics SDKs, crash reporters, WebViews, remote URLs, or login screens).
   - Custom local image & thumbnail pipeline (`LruCache` + local `cacheDir`) instead of third-party image loaders.
2. **SDK & Tooling Targets**
   - `minSdk = 26` (Android 8.0)
   - `targetSdk = 34` (Android 14)
   - `compileSdk = 34`
   - **JDK 17** required for AGP `8.5.2` + Gradle `8.7` (or let Android Studio auto-update wrapper if prompted; avoid hand-editing wrapper hashes unnecessarily).
   - **No deprecated APIs** on modern targets (isolate API `< 29` fallbacks cleanly behind `Build.VERSION.SDK_INT` checks).
   - **No placeholder TODOs**—every delivered file in each stage must compile and function completely.
3. **Branding & Assets**
   - App name: **Lowspin**.
   - Original adaptive vector launcher icon (geometric pulse/disc motif in `#1DB954` on `#121212`).
   - UI icons strictly from `androidx.compose.material.icons` (`material-icons-extended`).
   - **Zero** Spotify logos, names, or trademarked assets.

---

## 2. Tech Stack & Pinned Versions (Manual DI, Zero Hilt)

### Dependency Versions (`gradle/libs.versions.toml`)
- **AGP**: `8.5.2`
- **Kotlin**: `2.0.21` (with `org.jetbrains.kotlin.plugin.compose` matching Kotlin `2.0.21`)
- **KSP**: `2.0.21-1.0.28` *(used **only** by Room; exact Kotlin 2.0.21 match prevents KSP codegen build failures)*
- **Compose BOM**: `2024.09.03` (`ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`, `foundation`, `animation`)
- **Core KTX**: `1.13.1`
- **Activity Compose**: `1.9.3`
- **Lifecycle Runtime / ViewModel Compose**: `2.8.6`
- **Navigation Compose**: `2.8.3`
- **Media3**: `1.4.1` (`media3-exoplayer`, `media3-session`, `media3-common`)
- **Room**: `2.6.1` (`room-runtime`, `room-ktx`, `room-compiler` via `ksp`)
- **Palette KTX**: `1.0.0` (`androidx.palette:palette-ktx`)
- **Coroutines Android**: `1.9.0` (`kotlinx-coroutines-android`, `kotlinx-coroutines-guava` for Media3 `ListenableFuture` bridging)

### Dependency Injection Strategy: Manual `AppContainer`
- **No Hilt / Dagger**: Avoids double KSP codegen complexity and Kotlin/Hilt version mismatches.
- `LowspinApplication` creates a single `AppContainer(applicationContext)` holding lazy/singleton instances of:
  - `LowspinDatabase` (Room)
  - `MediaStoreScanner` & `Id3Enricher`
  - `AlbumArtRepository` (memory `LruCache` + disk cache + `Palette` extractor)
  - `MusicRepository`
  - `SleepTimerManager`
  - `PlaybackController` (connects to `PlaybackService` via `MediaController.Builder` + manages queue/state sync)
- ViewModels receive dependencies through clean `ViewModelProvider.Factory` definitions (or a lightweight `AppViewModelFactory` helper) accessed via Compose `viewModel(factory = ...)`.

---

## 3. Compile-Safe Typography (`assets/fonts/` + Runtime Fallback)

To achieve Spotify's Circular-style geometric sans-serif look **without breaking compilation** if binary `.ttf` files are absent:
- Target font family: **Plus Jakarta Sans** (SIL Open Font License — geometric proportions closely matching Circular / Spotify Mix).
- Location: `app/src/main/assets/fonts/`
  - `PlusJakartaSans-Regular.ttf` (`FontWeight.Normal`)
  - `PlusJakartaSans-Medium.ttf` (`FontWeight.Medium`)
  - `PlusJakartaSans-SemiBold.ttf` (`FontWeight.SemiBold`)
  - `PlusJakartaSans-Bold.ttf` (`FontWeight.Bold`)
  - `PlusJakartaSans-ExtraBold.ttf` (`FontWeight.ExtraBold`)
- **Why `assets/fonts/` instead of `res/font/`?**
  - `R.font.*` references fail compilation if `.ttf` files are missing on disk.
  - Loading from `context.assets` via `androidx.compose.ui.text.font.Font(path, assetManager, weight)` is never validated at compile time.
- **Implementation rule in `Typography.kt`**:
  - During Stage 1 setup, attempt to download the OFL `Plus Jakarta Sans` `.ttf` files into `app/src/main/assets/fonts/` (developer setup step—the compiled app itself stays 100% offline).
  - At runtime, `resolveLowspinFontFamily(context)` checks whether the expected `.ttf` files exist in `context.assets.list("fonts")`:
    - If present: builds the `FontFamily` from the bundled `.ttf` assets.
    - If missing: safely falls back to `FontFamily.SansSerif` with zero crashes and zero build failures.

---

## 4. Permissions & Manifest Architecture (`AndroidManifest.xml`)

### Permissions
- `<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />` *(Android 13+ / API 33+)*
- `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />` *(Android 8.0–12L / API 26–32)*
- `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` *(Optional on API 33+ for shade/lock-screen visibility; never blocks playback or scanning if denied)*
- `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
- `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />` *(Mandatory for Android 14 / `targetSdk 34`)*
- `<uses-permission android:name="android.permission.WAKE_LOCK" />`
- Explicit removal locks:
  - `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />`
  - `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />`

### `<service>` Declaration (Critical for Android 14)
```xml
<service
    android:name=".media.PlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

### Permission Flow UX
- Dedicated dark Spotify-styled **PermissionScreen** shown when audio permission (`READ_MEDIA_AUDIO` on API 33+, `READ_EXTERNAL_STORAGE` on API 26–32) is not granted.
- Tracks 3 states cleanly:
  1. Initial prompt (`Grant Access` button)
  2. Rationale state (after single denial, explains offline-only scanning)
  3. Permanently denied state (`Open App Settings` button opening `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`)
- Once granted, optionally prompts for `POST_NOTIFICATIONS` on API 33+ (dismissible/non-blocking) and immediately kicks off the initial MediaStore library scan.

---

## 5. Local Media Scanning, ID3 Tags & Art Caching (Thousands of Songs)

### A. Two-Phase Paged Scanner
1. **Phase 1 — Fast Chunked `MediaStore` Scan (`MediaStoreScanner`)**
   - Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with selection `IS_MUSIC != 0` (+ optional duration threshold `>= 10_000 ms`, configurable in Settings).
   - Columns: `_ID`, `TITLE`, `ARTIST`, `ARTIST_ID`, `ALBUM`, `ALBUM_ID`, `DURATION`, `TRACK`, `YEAR`, `DATE_ADDED`, `DATE_MODIFIED`, `MIME_TYPE`, `DISPLAY_NAME`, plus `RELATIVE_PATH` & `BUCKET_DISPLAY_NAME` on API 29+ (isolated legacy `DATA` folder extraction only on API 26–28 behind `Build.VERSION.SDK_INT < Q` so API 29+ never touches deprecated columns).
   - Reads the `Cursor` in chunks of **500 rows**, emitting scan progress (`scannedCount / totalCount`) and batch-upserting into Room while preserving user state (`isLiked`, `playCount`, `lastPlayedAt`) and pruning deleted tracks (`mediaStoreId` no longer present).
2. **Phase 2 — Background Incremental ID3 Enrichment (`Id3Enricher`)**
   - Runs at low I/O concurrency (`Dispatchers.IO.limitedParallelism(2)`) for tracks where `metadataEnriched == false` or `dateModified` changed.
   - Uses `MediaMetadataRetriever.setDataSource(context, contentUri)` to normalize:
     - Clean `title` (fallback to stripped filename if missing/unknown)
     - Clean `artist` & `album`
     - `trackNumber` (handles `"3/12"` -> `3`, and `1003` -> disc `1`, track `3`)
     - `discNumber` & `year`
   - Persists `metadataEnriched = true` in Room so future rescans skip unchanged files.

### B. Album Art, Thumbnails, Palette & Gradient Placeholders (`AlbumArtRepository`)
- **Two decode sizes**:
  - `THUMB` (`256 × 256` px) for song rows, grids, shelves, and Mini Player
  - `HERO` (`800 × 800` px) for Now Playing and collapsing detail headers
- **Lookup chain**:
  1. In-memory `LruCache<ArtCacheKey, ImageBitmap>` (sized to ~1/8th available app memory)
  2. Disk cache in `context.cacheDir/art_cache/{albumId}_{songId}_{size}.jpg`
  3. On API 29+: `contentResolver.loadThumbnail(albumUri / songUri, Size(px, px), cancellationSignal)`
  4. Fallback / API 26–28: `MediaMetadataRetriever.embeddedPicture` decoded with `BitmapFactory.Options` (`inJustDecodeBounds` + power-of-2 `inSampleSize`)
  5. **No embedded art**: Deterministic two-tone dark gradient generated from a stable hash of `album.ifBlank { title }` (HSL hues with controlled saturation/lightness so white text always stays legible).
- **Dominant Color (`Palette` API)**:
  - Runs `Palette.from(thumbnailBitmap).generate()` once per album/track, selecting `darkVibrantSwatch` -> `vibrantSwatch` -> `darkMutedSwatch` -> `dominantSwatch` (with luminance clamping for dark UI contrast), falling back to the deterministic placeholder's primary color when art is absent.
  - Stores `dominantColorArgb` in Room / memory cache for zero-flicker Mini Player & header tinting.

---

## 6. Room Database Schema (`LowspinDatabase`)

| Table / Entity | Key Columns | Purpose |
|---|---|---|
| `songs` (`SongEntity`) | `id` (MediaStore ID PK), `title`, `artist`, `artistId`, `album`, `albumId`, `durationMs`, `trackNumber`, `discNumber`, `year`, `folderPath`, `folderName`, `contentUri`, `mimeType`, `dateAdded`, `dateModified`, `isLiked`, `playCount`, `lastPlayedAt`, `dominantColorArgb`, `metadataEnriched` | Master catalog + user stats. Indexed on `albumId`, `artistId`, `title`, `playCount`, `lastPlayedAt`, `isLiked`, `folderName`. |
| `playlists` (`PlaylistEntity`) | `id` (auto PK), `name`, `createdAt`, `updatedAt` | User-created offline playlists. |
| `playlist_songs` (`PlaylistSongCrossRef`) | `playlistId`, `songId`, `position`, `addedAt` (composite PK `playlistId, songId`) | Ordered tracks per playlist; foreign-key cascades on delete. |
| `queue_items` (`QueueItemEntity`) | `id` (auto PK), `songId`, `position`, `originalIndex`, `addedAt` | Persisted active playback queue across app kills/reboots. |
| `playback_state` (`PlaybackStateEntity`) | `id` (`1`), `currentQueuePosition`, `positionMs`, `shuffleEnabled`, `repeatMode` (`OFF/ALL/ONE`), `playbackSpeed`, `sourceLabel` | Persists exact playback cursor, speed, shuffle, repeat, and "Playing from …" context. |
| `recent_searches` (`RecentSearchEntity`) | `query` (PK), `searchedAt` | Local-only recent search history. |

*Important Room upsert rule*: When rescanning `MediaStore`, update metadata columns via targeted `@Query` or merge helper so `isLiked`, `playCount`, `lastPlayedAt`, and playlist memberships are **never wiped** during a rescan.

---

## 7. Media3 Audio Engine (`PlaybackService : MediaSessionService`)

### Core `ExoPlayer` Setup
- Built inside `PlaybackService`:
  - `AudioAttributes`: `C.USAGE_MEDIA`, `C.AUDIO_CONTENT_TYPE_MUSIC`
  - `setAudioAttributes(attrs, /* handleAudioFocus = */ true)` -> automatic pause on call/audio focus loss & smooth ducking on navigation prompts
  - `setHandleAudioBecomingNoisy(true)` -> auto-pause on headphone unplug / Bluetooth disconnect
  - `setWakeMode(C.WAKE_MODE_LOCAL)` -> reliable playback with screen off
- `MediaSession.Builder(this, player)` with custom `MediaSession.Callback` for:
  - Media button / Bluetooth AVRCP next/prev/play/pause
  - `onPlaybackResumption` restoring persisted Room queue + position if triggered externally
  - Automatic `MediaStyle` lock-screen & notification controls (custom `DefaultMediaNotificationProvider` using Lowspin icon)

### Playback Features & Persistence
- **Smart Shuffle (No Immediate Repeats)**:
  - Tracks a recent-history window (`min(10, queueSize / 2)`) and applies a custom shuffle permutation / `ShuffleOrder` when toggling shuffle or wrapping around `REPEAT_ALL`, guaranteeing the next song never immediately repeats the last played song(s) (except when queue size is 1).
- **Repeat Modes**: Cycles `OFF` -> `ALL` -> `ONE` -> `OFF` synced cleanly with `ExoPlayer.repeatMode`.
- **Playback Speed**: `0.5f`, `0.75f`, `1.0f`, `1.25f`, `1.5f`, `2.0f` applied via `player.setPlaybackSpeed(speed)` and saved to `playback_state`.
- **Sleep Timer (`SleepTimerManager`)**:
  - Options: `Off`, `5 min`, `15 min`, `30 min`, `45 min`, `60 min`, or `End of current track`.
  - Survives screen off / UI close (runs in app/service scope), updates countdown `StateFlow` for UI display, and applies a smooth **3-second volume fade-out** before pausing playback and restoring volume to `1.0f`.
- **Play Count & Recently Played Recording**:
  - Listener tracks cumulative played time on the current track: records `lastPlayedAt` after 3 seconds of active play and increments `playCount` once the user listens to `>= 10 seconds` (or `>= 50%` for tracks under 20s) so rapid skipping doesn't skew "Most played".
- **Periodic State Persistence**:
  - Saves playback `positionMs` on pause, track transition, service `onTaskRemoved`/`onDestroy`, and every 5 seconds while playing.

---

## 8. UI & UX Specification (Pixel-Faithful Spotify Dark Theme)

### Design Tokens
- **Background**: `#121212`
- **Card / Shelf Surface**: `#181818`
- **Elevated Surface / Chips / Bottom Sheets**: `#282828`
- **Pressed / Highlight Surface**: `#333333`
- **Accent Green**: `#1DB954` (Pressed bright green: `#1ED760`)
- **Primary Text**: `#FFFFFF`
- **Secondary Text**: `#B3B3B3`
- **Muted / Disabled**: `#727272`
- **Destructive / Error**: `#E22134`
- **System Bars**: Edge-to-edge (`enableEdgeToEdge`) with dark transparent system bars and proper `statusBarsPadding()` / `navigationBarsPadding()`.
- **Micro-interactions**: Press-scale modifier (`0.96f` spring on cards/tiles/buttons), marquee scrolling (`basicMarquee()`) for long titles in Mini Player & Now Playing, `animateColorAsState` for dominant-color transitions.

### Persistent Navigation Shell
- **3 Bottom Tabs**: `Home`, `Search`, `Your Library` (Spotify-style vertical gradient backdrop fading from transparent to `#F2121212`, filled active icons vs outlined inactive icons).
- **Docked Mini Player** (sits directly above the bottom nav bar whenever a queue is loaded):
  - `8.dp` horizontal margin, `8.dp` corner radius, dynamic darkened dominant-color background from active track's album art.
  - `42.dp` rounded album art, 1-line title + 1-line artist, Heart toggle, Play/Pause button, `2.dp` progress bar along bottom edge.
  - **Horizontal swipe** left/right on the Mini Player skips Next / Previous track.
  - **Tap or drag up** smoothly expands into the full-screen **Now Playing** overlay (shared-element style animated container / synchronized vertical drag offset + corner-radius/art interpolation).

### Screens Breakdown
1. **Home Screen**
   - Top dominant-color radial glow fading into `#121212`.
   - Top row: Time-of-day greeting (*Good morning* / *Good afternoon* / *Good evening* / *Good night*) + History shortcut icon + Settings gear icon.
   - **2-column Quick-Access Grid** (6–8 compact `56.dp` cards): pinned *Liked Songs* (purple gradient square + white heart) + most recent albums/playlists.
   - **Horizontal Shelves (`LazyRow`)**:
     - *Recently played*
     - *Most played*
     - *Your albums*
     - *Your artists* (circular thumbnails)
   - Sensible empty state if the phone has 0 songs (with "Rescan library" button).
2. **Search Screen**
   - Sticky search bar (*"What do you want to listen to?"*) with clear button.
   - Idle state: Recent local searches (removable) + **"Browse all"** 2-column grid of vibrant colorful cards with tilted bottom-right album thumbnails:
     - **Artists**
     - **Albums**
     - **Songs**
     - **Playlists**
     - **Folders**
     - **Recently added**
   - Typing state: Live debounced search across Songs, Albums, Artists, Playlists, and Folders with filter chips (`All`, `Songs`, `Artists`, `Albums`, `Playlists`) and clickable result rows + three-dot menus.
3. **Your Library Screen**
   - Header: App badge + *"Your Library"*, in-library search icon, and `+` **Create Playlist** button (opens modal dialog).
   - Filter chips: `Playlists`, `Albums`, `Artists`, `Songs` (active chip styled in `#1DB954` with `#000000` text).
   - Sub-header: Sort selector (*Recent*, *Recently played*, *Alphabetical*, *Artist*, *Most played*) + Grid/List layout toggle.
   - Pinned **Liked Songs** item at top showing dynamic song count.
   - Smooth `LazyColumn` / `LazyVerticalGrid` tuned with stable keys (`song.id`, `album.id`, etc.) for 5,000+ item libraries.
4. **Detail Screens (`Album`, `Artist`, `Playlist`, `Liked Songs`, `Folder`, `Recently Added`)**
   - **Parallax Collapsing Header**: Large square/circular cover art shrinks and fades as user scrolls while the top bar cross-fades to solid darkened dominant color with centered title.
   - Vertical background gradient from dominant color into `#121212`.
   - Metadata block (artist/folder/year/track count/total duration).
   - Action row: Heart/Like or Rename/Delete (for user playlists), **Shuffle toggle**, and large circular `#1DB954` **Play/Pause** button.
   - Song rows: Track number (or thumbnail), title (tinted `#1DB954` with animated equalizer/play indicator when active), artist, duration, and **three-dot menu** bottom sheet:
     - *Like / Remove from Liked Songs*
     - *Add to playlist* (opens playlist picker sheet + inline quick-create)
     - *Add to queue* / *Play next*
     - *Go to album*
     - *Go to artist*
     - *Song details* (duration, year, folder path, format, play count)
5. **Full-Screen Now Playing Sheet**
   - Vertical gradient derived from active track's album art dominant & muted colors.
   - Top bar: Down-chevron dismiss, `"PLAYING FROM <SOURCE>"`, three-dot options menu.
   - **Swipe-down to dismiss** gesture with spring physics smoothly collapsing back into the Mini Player.
   - **Swipeable album art** (left/right horizontal pager synced with skip next/prev) with subtle elevation.
   - Title + clickable Artist (navigates to Artist screen) + animated bouncy Heart button.
   - Custom-styled Spotify seek bar (thin track that expands slightly while dragging) + `mm:ss` elapsed and total time labels.
   - Controls row: **Shuffle** (green + dot indicator), **Previous**, **Large white circular Play/Pause**, **Next**, **Repeat** (`Off` / `All` green + dot / `One` green + `1` badge).
   - Utility footer: **Speed pill** (`1.0x`), **Sleep Timer** button (shows live countdown badge when active), and **Queue** icon button.
6. **Queue Screen**
   - Accessed from Now Playing footer.
   - Sections: **Now Playing** card + **Next Up** list with *Clear upcoming* action.
   - **Drag-to-reorder** drag handles updating both Room `queue_items` and `ExoPlayer` media item order without interrupting current playback.
   - **Swipe-to-remove** (`SwipeToDismissBox`) on upcoming tracks.
7. **Settings Screen**
   - Accessed from Home top-right gear icon.
   - **Rescan library** button with live progress bar + total counts (`Songs • Albums • Artists • Folders`).
   - **Ignore short audio files (< 10s)** toggle (filters ringtones/notification clips).
   - **Playback speed default / current** & **Clear album art cache** (displays current cache MB size).
   - **Offline Guarantee** summary card (verifies zero internet permissions).

---

## 9. Planned Project File Map (`D:\oc1`)

```text
D:\oc1\
├── PLAN.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle\
│   ├── libs.versions.toml
│   └── wrapper\
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
└── app\
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src\main\
        ├── AndroidManifest.xml
        ├── assets\fonts\                         # Optional PlusJakartaSans-*.ttf (safe fallback if empty)
        ├── res\
        │   ├── drawable\ic_launcher_foreground.xml
        │   ├── drawable\ic_notification_music.xml
        │   ├── mipmap-anydpi-v26\ic_launcher.xml
        │   ├── mipmap-anydpi-v26\ic_launcher_round.xml
        │   └── values\
        │       ├── colors.xml
        │       ├── strings.xml
        │       └── themes.xml
        └── java\com\lowspin\player\
            ├── LowspinApplication.kt
            ├── MainActivity.kt
            ├── di\
            │   └── AppContainer.kt
            ├── data\
            │   ├── db\
            │   │   ├── LowspinDatabase.kt
            │   │   ├── Entities.kt
            │   │   ├── Daos.kt
            │   │   └── Models.kt                 # AlbumSummary, ArtistSummary, FolderSummary, PlaylistWithSongs
            │   ├── scanner\
            │   │   ├── MediaStoreScanner.kt
            │   │   └── Id3Enricher.kt
            │   ├── art\
            │   │   ├── AlbumArtRepository.kt
            │   │   └── GradientPlaceholder.kt
            │   └── repository\
            │       └── MusicRepository.kt
            ├── media\
            │   ├── PlaybackService.kt
            │   ├── PlaybackController.kt
            │   ├── SmartShuffleManager.kt
            │   └── SleepTimerManager.kt
            ├── ui\
            │   ├── theme\
            │   │   ├── Color.kt
            │   │   ├── Theme.kt
            │   │   └── Type.kt
            │   ├── navigation\
            │   │   ├── Routes.kt
            │   │   └── LowspinNavHost.kt
            │   ├── components\
            │   │   ├── AlbumArtImage.kt
            │   │   ├── PressScaleClickable.kt
            │   │   ├── MiniPlayer.kt
            │   │   ├── BottomNavBar.kt
            │   │   ├── SongRow.kt
            │   │   ├── SongOptionsSheet.kt
            │   │   └── PlaylistPickerSheet.kt
            │   └── screens\
            │       ├── permission\PermissionScreen.kt
            │       ├── home\HomeScreen.kt & HomeViewModel.kt
            │       ├── search\SearchScreen.kt & SearchViewModel.kt
            │       ├── library\LibraryScreen.kt & LibraryViewModel.kt
            │       ├── detail\DetailScreens.kt & DetailViewModel.kt
            │       ├── nowplaying\NowPlayingSheet.kt & NowPlayingViewModel.kt
            │       ├── queue\QueueScreen.kt & QueueViewModel.kt
            │       └── settings\SettingsScreen.kt & SettingsViewModel.kt
            └── util\
                └── Formatters.kt
```

---

## 10. Staged Build & Real-Device Verification Plan

Each stage must **compile cleanly and run on your physical Android phone** before starting the next stage.

### Stage 1 — Foundation, Scanner, Room, Playback Service & Core Player
- **Scope**:
  - Gradle setup (`AGP 8.5.2`, `Kotlin 2.0.21`, `KSP 2.0.21-1.0.28`, `compileSdk/targetSdk 34`, `minSdk 26`, JDK 17).
  - `AndroidManifest.xml` (zero `INTERNET`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `android:foregroundServiceType="mediaPlayback"` on `PlaybackService`).
  - Manual `AppContainer`, Lowspin dark theme, adaptive icon, compile-safe `assets/fonts/` Plus Jakarta Sans loader + fallback.
  - Permission onboarding screen.
  - Chunked `MediaStoreScanner` + incremental `Id3Enricher` + Room database + `AlbumArtRepository` (thumbnails, Palette, deterministic gradient placeholders).
  - `PlaybackService` (`MediaSessionService` + `ExoPlayer` + audio focus + noisy-audio pause + Bluetooth/lock-screen media controls + persisted queue/position + Smart Shuffle + Repeat).
  - Stage 1 usable UI: Clean all-songs starter list + scan progress banner + docked **Mini Player** + expandable **Now Playing** screen.
- **Stage 1 Phone Test Checklist**:
  1. App installs via Android Studio (`Run 'app'`) or APK sideload with zero internet permissions shown in Android App Info.
  2. Grant audio permission -> scans local MP3s cleanly with progress indicator.
  3. Tap a song -> plays audio, Mini Player updates with album art (or deterministic gradient) & dominant color tint.
  4. Turn screen off / press Home -> audio keeps playing; lock screen and notification shade show artwork, title, artist, play/pause, next/prev.
  5. Disconnect Bluetooth/wired headphones -> playback pauses automatically.
  6. Force-close and reopen app -> queue and track position restore accurately.

### Stage 1.5 — Recording Filter, Playlists, Search, Folder Manager & smoothness pass
- **Scope** (added after Stage 1 device feedback):
   - `TrackFilter` rejects known recorder folders/filenames/titles at scan time; `ScanState.Complete.filteredCount` reports the count in the UI.
   - Room **migration 1 -> 2** adds `excluded_folders` (non-destructive: likes, play counts, playlists and saved queue are preserved).
   - "Manage folders" screen with include/exclude switches; hiding a folder triggers an automatic rescan.
   - Playlists: Songs/Playlists tabs, create/rename/delete, add/remove songs, 4-art collage hero, Play/Shuffle, long-press drag reorder persisted via `position`, pinned **Liked Songs**.
   - Song three-dot/long-press sheet: Like, Play next, Add to queue, Add to playlist, Remove from playlist (context-aware), New playlist.
   - Global search: debounced live Songs + Playlists results, recent searches, tap-to-play with results as queue.
   - Navigation foundation: `navigation-compose` with `library`, `search`, `playlist/{id}`, `liked`, `folders` routes; Mini Player hoisted into a shared `Scaffold` so it stays docked on every screen.
   - Smoothness: capped parallel album-art decodes (`Semaphore(4)`), stable callback identities, `animateItem()`, stable list keys/content types, reduced per-frame work in the root composition.
- **Stage 1.5 Phone Test Checklist**:
   1. Upgrade installs over Stage 1 without data loss; call/voice recordings are gone and "N recordings hidden" is shown.
   2. Manage folders: hide and unhide a folder -> songs disappear/reappear after rescan and the choice survives a restart.
   3. Create a playlist, add songs, drag-reorder, play/shuffle, remove a track, rename, delete; all persist across restart and rescan.
   4. Search finds songs and playlists instantly; playback queue matches the result list; recent searches persist.
   5. Likes, play counts and the saved queue survive the migration.
   6. Scrolling a large library and switching screens feels smooth; album art still appears in notifications/lock screen.

### Stage 2 — Spotify UI Recreation (Home, Search, Your Library & Detail Screens)
- **Scope**:
  - 3-tab bottom navigation (`Home`, `Search`, `Your Library`) with edge-to-edge translucent bar & proper system insets.
  - `HomeScreen` (dynamic top gradient, time greeting, 2-column quick-access grid, 4 horizontal `LazyRow` shelves).
  - `SearchScreen` (live search bar, filter pills, recent searches, 2-column colorful *Browse all* category tiles with tilted thumbnails).
  - `LibraryScreen` (filter chips, sort modal, grid/list toggle, pinned *Liked Songs*, Create Playlist modal).
  - Detail screens (`Album`, `Artist`, `Playlist`, `Liked Songs`, `Folder`, `Recently Added`) with parallax collapsing headers, dominant-color gradients, Shuffle + green Play buttons, and full three-dot action bottom sheet + Playlist Picker sheet.
  - Smooth shared-element / drag-synchronized transition between Mini Player and Now Playing.
- **Stage 2 Phone Test Checklist**:
  1. Smooth 60fps scrolling across thousands of songs, albums, and artists without jank or OOM.
  2. Create a playlist, add/remove songs, like/unlike songs, navigate between *Go to Album* / *Go to Artist*.
  3. Verify edge-to-edge insets on gesture navigation and 3-button navigation bars.

### Stage 3 — Queue, Extras, Settings & Polish
- **Scope**:
  - `QueueScreen` (Now Playing + Next Up with drag-to-reorder and swipe-to-remove synced to `ExoPlayer` and Room).
  - `SleepTimerManager` + bottom sheet UI (`5m`–`60m` + *End of track* + 3s volume fade-out).
  - Playback speed selector (`0.5x`–`2.0x`) persisted across restarts.
  - `SettingsScreen` (manual *Rescan library*, ignore short clips `< 10s` toggle, thumbnail cache size + clear button, offline guarantee card).
  - Empty-state polish, edge-case guards, release/debug APK build instructions.
- **Stage 3 Phone Test Checklist**:
  1. Reorder and swipe-remove items in Queue -> next-track playback matches new order immediately and survives app restart.
  2. Set Sleep Timer -> countdown displays, volume fades smoothly and pauses playback at 0.
  3. Change playback speed -> persists across tracks and restarts.
  4. Rescan library after adding/deleting an MP3 -> library updates without losing liked songs, playlists, or play counts.

---

## 11. Manual User Setup & Build Reference (For Execution Phase)

1. **Android Studio Setup**:
   - Install **Android Studio Ladybug (or newer)** with **Android SDK Platform 34** (`API 34`) and **Android SDK Build-Tools 34.x**.
   - Ensure Gradle JDK is set to **JDK 17** (`File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`).
   - If Android Studio prompts to upgrade the Gradle wrapper or AGP minor version, accept rather than editing wrapper hashes manually.
2. **Optional Font Drop-in**:
   - Place `PlusJakartaSans-Regular.ttf`, `Medium.ttf`, `SemiBold.ttf`, `Bold.ttf`, `ExtraBold.ttf` in `app/src/main/assets/fonts/` (will be fetched automatically during Stage 1 if available, otherwise falls back cleanly to system sans-serif).
3. **Building & Installing APK on Phone**:
   - **Via USB Debugging**: Enable *Developer Options* -> *USB Debugging* on your phone, connect via USB, click **Run** in Android Studio.
   - **Via Standalone APK**: `Build > Build Bundle(s) / APK(s) > Build APK(s)` (or `./gradlew assembleDebug` / `assembleRelease`), transfer `app-debug.apk` to phone, enable *Install unknown apps* for your file manager/browser, and tap to install.
