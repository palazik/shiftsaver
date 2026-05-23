#!/usr/bin/env bash
set -euo pipefail

systemctl --user stop shiftsaver.service
echo "ShiftSaver server stopped."
