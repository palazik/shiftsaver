# ShiftSaver Android

This folder contains the Android app for ShiftSaver.

## Stack

- Kotlin
- Jetpack Compose
- Miuix UI components for Xiaomi/HyperOS-style Compose design
- DataStore Preferences
- OkHttp

The app uses one MIUIX-style interface with two selectable themes:

- `White`
- `Dark`

Miuix is experimental, so APIs may change between releases, but the app now depends on the real Android Miuix artifact, `top.yukonga.miuix.kmp:miuix-android`, instead of a Material-style skin.

## Build

Open this folder in Android Studio:

```text
ShiftSaver/
```

Then build and install the `app` module.

## Run

1. Install and start the Linux server from the repo root:

   ```bash
   ./linux/install.sh
   ```

2. Open the Android app.
3. Enter the server IP and port printed by the installer.
4. Tap `Test`.
5. Paste a public media URL.
6. Tap `Start`.

## App Sections

- `Download` - paste a media URL, start a job, and open the completed file.
- `Servers` - configure the server IP/port, test the connection, and pick quick presets.
- `Settings` - switch between White and Dark themes.
- `About` - app version, current server, and responsible-use notes.

## Notes

The app allows cleartext HTTP because the server is intended to run on your local network. Do not expose the Linux server directly to the public internet without adding authentication, HTTPS, rate limiting, and stricter host/network controls.
