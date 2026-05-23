#!/usr/bin/env bash
set -euo pipefail

PORT="${SHIFTSAVER_PORT:-8787}"
IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
systemctl --user --no-pager status shiftsaver.service || true
echo
echo "Server URL: http://${IP:-YOUR_LINUX_IP}:$PORT"
echo "Firewall: allow TCP port $PORT if needed."
