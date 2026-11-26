# Lowspin for iOS

The iPhone companion to the Android app. Native SwiftUI, iOS 17+, fully offline.

**Status: source written, never compiled.** This machine is Windows with no
Xcode, so the only verification available is the GitHub Actions macOS build.
A green CI build means it compiles — it does not mean it runs correctly.
Running it requires a Mac, Xcode and an iPhone.

## Build

On a Mac:

```bash
brew install xcodegen
cd ios
xcodegen generate --spec project.yml
open Lowspin.xcodeproj
```

Then pick a simulator or your device in Xcode and press Run.

Automated compile check (no Mac needed by you):

- Push the repo to GitHub.
- `.github/workflows/ios.yml` runs on a macOS runner, generates the project
  and performs an unsigned simulator build.

## Features

- **Themes** — Violet `#A78BFA`, Blue `#4C6FFF`, Emerald `#4FD1A5`, Amber
  `#F0B86E`, with the same 6–10% lerp tinting as Android. Launcher icon and
  vinyl stay violet as brand marks.
- **Library** — folder import from Files with security-scoped bookmarks,
  incremental rescan, off-main-actor metadata/artwork parsing, cached dominant
  colour, search, Liked, hidden songs and folder management.
- **Playlists** — create, delete and add songs via the context menu.
- **Playback** — `AVAudioEngine` → 5-band `AVAudioUnitEQ` → mixer, background
  audio, interruption handling, seek-safe completion callbacks.
- **Dynamic Island** — system Now Playing (artwork, title, scrubber, transport
  controls) plus a **sleep-timer Live Activity** with real Dynamic Island
  compact/expanded views. The countdown uses `Text(..., style: .timer)`.
- **Equaliser** — X button, enable checkbox, preset tabs (Classical, Heavy M,
  Dance, Rock, Custom), five vertical sliders with ±12 dB guides and the exact
  `60 Hz (Bass) / 230 Hz (Low-Mid) / 910 Hz (Mid) / 3.6 kHz (Presence) /
  14 kHz (Treble)` labels. Flat is the zero-gain default; Custom is one
  persistent edit-in-place slot. The mini player stays docked on this screen.
- **Lyrics** — ID3v2 USLT and SYLT parsed directly from the file, with synced
  highlighting. AVFoundation does not expose SYLT, hence the manual parser.
- **Sleep timer** — 5/10/15/30/45/60 minutes, pauses playback and ends the
  Live Activity.
- **About** — "built by axnast".

## Constraints

- Import model is a **Files folder**, not a filesystem scan. iOS does not allow
  arbitrary storage scanning. See `.fileImporter` in `LibraryView`.
- Supported audio: MP3, M4A/AAC, ALAC, FLAC, WAV, AIFF, CAF, MP4/M4B.
  No OGG/Opus/WMA — AVFoundation cannot decode them.
- Equaliser is in-app only. iOS provides no system-wide equaliser.
- Dynamic Island hardware is limited to iPhone 14 Pro and newer; on other
  models the same information appears on the Lock Screen.
- No network permissions, no analytics, no third-party dependencies.

## TestFlight

1. Join the Apple Developer Program and create App IDs for `com.lowspin.app`
   and `com.lowspin.app.widget`, plus App Store provisioning profiles named
   exactly `Lowspin App Store` and `Lowspin Widget App Store`.
2. Create a Distribution certificate and export it as a `.p12`.
3. Add the repository secrets documented at the top of
   `.github/workflows/ios-release.yml`, and set the team ID in
   `ios/ExportOptions.plist`.
4. Run the **iOS TestFlight build** workflow. Upload the resulting `.ipa` with
   Transporter or `xcrun altool`, then add a build to TestFlight in App Store
   Connect.

## Caveats

The first CI run will almost certainly surface compiler errors — none of this
code has seen a Swift compiler. Fix them in a loop until the simulator build is
green. Even then, only a real device can confirm the Dynamic Island, audio
session behaviour and ID3 parsing.
