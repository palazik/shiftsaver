#!/usr/bin/env bash
set -euo pipefail

PORT="${SHIFTSAVER_PORT:-8787}"
APP_DIR="${SHIFTSAVER_APP_DIR:-$HOME/.local/share/shiftsaver-server}"
DOWNLOAD_DIR="${SHIFTSAVER_DOWNLOAD_DIR:-$HOME/ShiftSaverDownloads}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

install_packages() {
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y python3 python3-venv python3-pip ffmpeg curl
  elif command -v dnf >/dev/null 2>&1; then
    sudo dnf install -y python3 python3-pip ffmpeg curl
  elif command -v pacman >/dev/null 2>&1; then
    sudo pacman -Sy --needed python python-pip ffmpeg curl
  else
    echo "Unsupported distro: install python3, venv/pip, ffmpeg, and curl manually." >&2
    exit 1
  fi
}

detect_ip() {
  hostname -I 2>/dev/null | awk '{print $1}'
}

install_packages
mkdir -p "$APP_DIR" "$DOWNLOAD_DIR"
cp "$REPO_ROOT/linux/server/shiftsaver_server.py" "$APP_DIR/"
cp "$REPO_ROOT/linux/server/requirements.txt" "$APP_DIR/"

python3 -m venv "$APP_DIR/.venv"
"$APP_DIR/.venv/bin/pip" install --upgrade pip
"$APP_DIR/.venv/bin/pip" install -r "$APP_DIR/requirements.txt"

cat >"$APP_DIR/shiftsaver.env" <<ENV
SHIFTSAVER_DOWNLOAD_DIR=$DOWNLOAD_DIR
SHIFTSAVER_MAX_JOBS=2
ENV

mkdir -p "$HOME/.config/systemd/user"
cat >"$HOME/.config/systemd/user/shiftsaver.service" <<SERVICE
[Unit]
Description=ShiftSaver local media server
After=network-online.target

[Service]
WorkingDirectory=$APP_DIR
EnvironmentFile=$APP_DIR/shiftsaver.env
ExecStart=$APP_DIR/.venv/bin/uvicorn shiftsaver_server:app --host 0.0.0.0 --port $PORT
Restart=on-failure
RestartSec=3

[Install]
WantedBy=default.target
SERVICE

systemctl --user daemon-reload
systemctl --user enable --now shiftsaver.service
if command -v loginctl >/dev/null 2>&1; then
  sudo loginctl enable-linger "$USER" || true
fi

IP="$(detect_ip)"
echo "ShiftSaver server is running."
echo "Server URL: http://${IP:-YOUR_LINUX_IP}:$PORT"
echo "Open/allow TCP port $PORT in your firewall if your phone cannot connect."
echo "Examples: sudo ufw allow $PORT/tcp | sudo firewall-cmd --add-port=$PORT/tcp --permanent | sudo firewall-cmd --reload"
echo "Downloads directory: $DOWNLOAD_DIR"
