#!/usr/bin/env bash
set -euo pipefail

PORT="${SHIFTSAVER_PORT:-8787}"
APP_DIR="${SHIFTSAVER_APP_DIR:-$HOME/.local/share/shiftsaver-server}"

if [ ! -x "$APP_DIR/.venv/bin/uvicorn" ]; then
  echo "ShiftSaver is not installed. Run ./linux/install.sh first." >&2
  exit 1
fi

systemctl --user start shiftsaver.service
IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
echo "Server URL: http://${IP:-YOUR_LINUX_IP}:$PORT"
