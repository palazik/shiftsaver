#!/usr/bin/env bash
# ShiftSaver — Update Script (updates yt-dlp to latest)
set -euo pipefail

VENV_DIR="/opt/shiftsaver/venv"
SERVICE_NAME="shiftsaver"

echo "[INFO] Updating yt-dlp..."
"$VENV_DIR/bin/pip" install --upgrade yt-dlp

if command -v systemctl &>/dev/null; then
    echo "[INFO] Restarting service..."
    systemctl restart "$SERVICE_NAME"
    echo "[OK] Service restarted"
fi

echo "[OK] yt-dlp updated to: $("$VENV_DIR/bin/yt-dlp" --version)"
