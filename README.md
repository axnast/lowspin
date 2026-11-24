# Lowspin

A fully offline music player for Android. No account, no internet, no ads. Lowspin plays the local MP3 files already on your phone inside a clean, Spotify-inspired interface.

## Features

- **100% Offline:** Plays local MP3 files directly from device storage with zero network permissions required.
- **Spotify-Inspired UI:** Clean navigation across Home, Library, Search, and a full-screen Now Playing view.
- **Full Playback Controls:** Play/pause, next/previous, seek, shuffle, and repeat.
- **Background Playback:** Persistent notification and lock-screen media controls.
- **Library Organization:** Custom playlists and playback queue management.
- **Metadata Support:** Automatically reads song title, artist, album, and cover art from ID3 tags.

## Download & Install (Android APK)

Lowspin is distributed exclusively through GitHub Releases and is not on the Google Play Store.

1. Download the latest `lowspin.apk` from the [Releases](https://github.com/axnast/lowspin/releases) page.
2. Open the APK file on your Android device and allow **Install from unknown sources** if prompted.
3. Launch **Lowspin** and grant permission to access your local audio/music files.
4. Your local MP3 library will load automatically.

## Build from Source
Only needed if you want to compile the APK yourself on a PC—regular users can grab it from Releases.
### Requirements
- JDK 17+
- Android SDK Command-Line Tools (with `ANDROID_HOME` set)

### Steps

```bash
git clone [https://github.com/axnast/lowspin.git](https://github.com/axnast/lowspin.git)
cd lowspin
```

Build the APK using the Gradle wrapper:

**Windows:**
```cmd
gradlew.bat assembleDebug
```

**macOS / Linux:**
```bash
./gradlew assembleDebug
```

The generated APK will be in `app/build/outputs/apk/debug/` (use `assembleRelease` for a release build).

## Tech Stack

- **Platform:** Native Android (Kotlin, Gradle Kotlin DSL)
- **Audio:** Local storage scanning & ID3 tag parsing
- **Development:** Co-developed with DeepSeek

## Project Structure

```text
lowspin/
├── app/                   # Android application source & resources
├── gradle/                # Gradle wrapper and version catalog
├── build.gradle.kts       # Top-level Gradle build configuration
├── settings.gradle.kts    # Project module settings
├── gradle.properties      # Gradle environment settings
├── PLAN.md                # Development notes and planning
└── README.md              # Project documentation
```

## Roadmap

- [ ] Equalizer
- [ ] Sleep timer
- [ ] Lyrics support (`.lrc` files)
- [ ] Custom themes and accent colors
- [ ] Folder-based browsing

## Disclaimer

Lowspin is an independent personal project and is not affiliated with, endorsed by, or connected to Spotify.

## License

MIT — see the [LICENSE](LICENSE) file for details.

## Author

Built by [axnast](https://github.com/axnast).
