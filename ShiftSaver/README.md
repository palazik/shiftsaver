# ShiftSaver Android

This folder contains the Android app for ShiftSaver.

## Stack

- Kotlin
- Jetpack Compose
- Compose UI components with a MIUIX-style skin
- DataStore Preferences
- OkHttp

The app uses one MIUIX-style interface with two selectable themes:

- `White`
- `Dark`

MIUIX itself is experimental, so this app currently uses a MIUIX-inspired Compose skin instead of depending directly on unstable MIUIX APIs.

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
