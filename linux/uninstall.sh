#!/usr/bin/env bash
# ShiftSaver — Uninstall Script
set -euo pipefail

SERVICE_NAME="shiftsaver"

if [[ $EUID -ne 0 ]]; then
    echo "Please run as root: sudo bash $0"
    exit 1
fi

if command -v systemctl &>/dev/null; then
    systemctl stop "$SERVICE_NAME" 2>/dev/null || true
    systemctl disable "$SERVICE_NAME" 2>/dev/null || true
    rm -f "/etc/systemd/system/${SERVICE_NAME}.service"
    systemctl daemon-reload
    echo "[OK] Service removed"
fi

read -rp "Remove /opt/shiftsaver (downloads included)? [y/N] " confirm
if [[ "$confirm" =~ ^[Yy]$ ]]; then
    rm -rf /opt/shiftsaver
    echo "[OK] /opt/shiftsaver removed"
fi

echo "[OK] ShiftSaver uninstalled"
