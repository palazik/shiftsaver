# ShiftSaver

Download videos from **YouTube, TikTok, and Instagram** without watermarks via a self-hosted Linux server and Android app.

## Repository layout

```
ShiftSaver/        ← Android app (Jetpack Compose, Kotlin)
linux/             ← Linux server scripts
  setup.sh         ← Install & autostart server
  server.py        ← Flask + yt-dlp download API
  update.sh        ← Update yt-dlp
  uninstall.sh     ← Remove everything
```

---

## Android app

- Choosable **Material Design 3** or **Miuix (HyperOS)** design — switchable live in Settings
- Connect to your Linux server via IP + port
- Download from YouTube / TikTok / Instagram (no watermark)
- History screen for completed downloads
- Share-sheet support: share a URL from any app → ShiftSaver

### Build

The GitHub Actions workflow (`.github/workflows/build.yml`) builds debug + unsigned release APKs automatically on push to `main` and sends them to Telegram.

---

## Linux server

### Requirements
- Debian/Ubuntu, Fedora/RHEL, or Arch Linux
- Python 3.10+
- ffmpeg (auto-installed by setup.sh)

### Quick start

```bash
git clone https://github.com/YOUR_USER/ShiftSaver.git
cd ShiftSaver/linux
sudo bash setup.sh
```

The script will:
1. Install Python, pip, ffmpeg
2. Create a virtualenv at `/opt/shiftsaver/venv`
3. Install Flask + yt-dlp
4. Install and start a `systemd` service
5. Print the server IP and port to enter in the app

### Default port

`5050` — open it in your firewall (setup.sh will tell you the exact command for your distro).

### API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/status` | Health check |
| POST | `/download` | Download a URL |
| GET | `/list` | List downloaded files |
| GET | `/files/<name>` | Serve a downloaded file |

**POST /download body:**
```json
{ "url": "https://...", "quality": "best" }
```
`quality`: `best` | `720p` | `480p` | `audio`

---
