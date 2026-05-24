# ShiftSaver

ShiftSaver is a LAN-based Android app plus Linux helper server for saving public media links through your own machine.

The repo is split like this:

- `ShiftSaver/` - Android app source.
- `linux/` - Linux installer, systemd service scripts, and Python server.

## What Works Now

- Android app scaffold using Jetpack Compose.
- MIUIX-style interface with White and Dark themes.
- Saved server IP and port settings.
- Server health check from the app.
- Download job creation and status polling.
- Linux server using FastAPI, Uvicorn, `yt-dlp`, and `ffmpeg`.
- Installer for Debian/Ubuntu, Fedora, and Arch based distros.
- User systemd auto-run service.

## Important Limits

This app is for public media you own or have permission to save.

It does not bypass DRM, private accounts, paywalls, login-only content, app-only restrictions, or platform access controls. YouTube, TikTok, and Instagram regularly change their sites, so downloads depend on the installed `yt-dlp` version continuing to support the URL you try.

## Linux Server Setup

Clone the repo on a Linux machine connected to the same network as your Android phone:

```bash
git clone <your-repo-url>
cd ShiftSaver
./linux/install.sh
```

The installer:

- Installs system packages with `apt-get`, `dnf`, or `pacman`.
- Creates a Python virtual environment at `~/.local/share/shiftsaver-server`.
- Installs server Python dependencies.
- Creates `~/ShiftSaverDownloads`.
- Enables and starts `shiftsaver.service` as a user systemd service.
- Prints the server IP and port.

Default server URL:

```text
http://YOUR_LINUX_IP:8787
```

If your phone cannot connect, allow TCP port `8787` in your firewall:

```bash
sudo ufw allow 8787/tcp
```

or on Fedora/firewalld:

```bash
sudo firewall-cmd --add-port=8787/tcp --permanent
sudo firewall-cmd --reload
```

## Linux Commands

```bash
./linux/status.sh
./linux/start.sh
./linux/stop.sh
```

Install with a custom port:

```bash
SHIFTSAVER_PORT=9000 ./linux/install.sh
```

Use a custom downloads folder:

```bash
SHIFTSAVER_DOWNLOAD_DIR="$HOME/Videos/ShiftSaver" ./linux/install.sh
```

## Android Build

Open the `ShiftSaver/` folder in Android Studio.

Requirements:

- Android Studio with Android SDK.
- JDK installed through Android Studio or locally.
- Internet access for Gradle dependency download.

Build and install the `app` module, then open ShiftSaver on the phone and enter:

- IP address: the Linux IP printed by `./linux/install.sh`
- Port: `8787` unless changed

Tap `Test`, then paste a public TikTok, YouTube, or Instagram URL and tap `Start`.

The app has four bottom tabs:

- `Download` for submitting public media URLs.
- `Servers` for server IP/port, connection test, and quick presets.
- `Settings` for White/Dark theme selection.
- `About` for app/version/server information and usage limits.

## GitHub Actions APK Build

The workflow is at `.github/workflows/build.yml`.

It runs on:

- Pushes to `main`.
- Manual runs from the GitHub Actions tab.

It builds:

- Debug APK.
- Unsigned release APK.

The project is pinned to Android Gradle Plugin `9.2.1`, Kotlin `2.3.21`, compile SDK `35`, and Gradle `9.4.1` so it matches the workflow base and Miuix dependency metadata.

Artifacts are uploaded as:

- `ShiftSaver-debug`
- `ShiftSaver-release-unsigned`

Optional Telegram upload uses these repository secrets:

- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `TELEGRAM_TOPIC_ID`

If the Telegram secrets are not set, the APK still builds and uploads to GitHub Actions artifacts.

## Server API

Health:

```bash
curl http://localhost:8787/health
```

Create a download:

```bash
curl -X POST http://localhost:8787/downloads \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.youtube.com/watch?v=VIDEO_ID"}'
```

Check a job:

```bash
curl http://localhost:8787/downloads/JOB_ID
```

Downloaded files are served from:

```text
http://YOUR_LINUX_IP:8787/files/FILE_NAME
```

## Development Verification

Current local verification:

```bash
python3 -m py_compile linux/server/shiftsaver_server.py
bash -n linux/install.sh
bash -n linux/start.sh
bash -n linux/status.sh
bash -n linux/stop.sh
```

Android build verification still needs Android Studio/JDK/Gradle. This environment did not have `java` or `gradle`, so the Android app has not been compiled here yet.
