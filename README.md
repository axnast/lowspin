# Lowspin

A fully offline music player for Android. No account, no internet, no ads. It plays the MP3 files already on your phone, with a clean, Spotify-inspired interface.

<!-- Add screenshots here: ![Home](docs/home.png) ![Now Playing](docs/now-playing.png) -->

## Features

- Plays local MP3 files straight from your device storage
- 100% offline, no network permission needed
- Spotify-inspired UI: home, library, search, and a full-screen now-playing view
- Playback controls: play/pause, next/previous, seek, shuffle, repeat
- Background playback with notification and lock-screen controls
- Playlists and queue management
- Reads song title, artist, album, and cover art from ID3 tags

## Download

Grab the latest APK from the [Releases](https://github.com/axnast/lowspin/releases) page.

1. Download `lowspin.apk` to your phone
2. Open it and allow **Install from unknown sources** if Android asks
3. Launch Lowspin and grant access to your music files
4. Your library appears automatically

> Lowspin is not on the Play Store. It is distributed only through GitHub Releases.

## Build from source

```bash
git clone https://github.com/axnast/lowspin.git
cd lowspin
```

```bash
# edit to match your setup
npm install
npm run build
```

The APK ends up in the build output folder. Adjust this section to match how you actually build it.

## Tech

- Built with Google AI Studio (Gemini)
- Android, local file playback
- <!-- list your framework / libraries here -->

## Project structure

```
lowspin/
├── src/          # app source
├── docs/         # screenshots
├── README.md
└── LICENSE
```

## Roadmap

- [ ] Equalizer
- [ ] Sleep timer
- [ ] Lyrics support (.lrc files)
- [ ] Custom themes and accent colors
- [ ] Folder-based browsing

## Disclaimer

Lowspin is an independent personal project and is not affiliated with or endorsed by Spotify.

## License

MIT, see [LICENSE](LICENSE).

## Author

Built by [axnast](https://github.com/axnast).
