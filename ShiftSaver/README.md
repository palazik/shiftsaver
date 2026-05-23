# ShiftSaver Android

This folder contains the Android app for ShiftSaver.

## Stack

- Kotlin
- Jetpack Compose
- Material 3 components
- DataStore Preferences
- OkHttp

The app includes two selectable visual modes:

- `MIUIX` - MIUIX-inspired skin using bright blue/green colors and larger rounded surfaces.
- `Material 3` - Material Design 3-style skin using stable Compose Material 3 components.

MIUIX itself is experimental, so this app currently uses a MIUIX-style Compose skin instead of depending directly on unstable MIUIX APIs.

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

## Notes

The app allows cleartext HTTP because the server is intended to run on your local network. Do not expose the Linux server directly to the public internet without adding authentication, HTTPS, rate limiting, and stricter host/network controls.
