#!/usr/bin/env bash
# ============================================================
#  ShiftSaver — Linux Setup Script
#  Supports: Debian/Ubuntu, Fedora/RHEL, Arch Linux
# ============================================================

set -euo pipefail

INSTALL_DIR="/opt/shiftsaver"
SERVICE_NAME="shiftsaver"
SERVER_PORT="${PORT:-5050}"
DOWNLOAD_DIR="${INSTALL_DIR}/downloads"
VENV_DIR="${INSTALL_DIR}/venv"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ── Detect distro ─────────────────────────────────────────

detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        echo "$ID"
    else
        echo "unknown"
    fi
}

DISTRO=$(detect_distro)

# ── Colour output ──────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; exit 1; }

# ── Root check ────────────────────────────────────────────

if [[ $EUID -ne 0 ]]; then
    error "Please run as root: sudo bash $0"
fi

info "Detected distro: $DISTRO"

# ── Install system dependencies ───────────────────────────

install_deps() {
    info "Installing system dependencies..."
    case "$DISTRO" in
        ubuntu|debian|linuxmint|pop)
            apt-get update -qq
            apt-get install -y python3 python3-pip python3-venv ffmpeg curl wget
            ;;
        fedora|rhel|centos|rocky|almalinux)
            dnf install -y python3 python3-pip ffmpeg curl wget 2>/dev/null || \
            yum install -y python3 python3-pip ffmpeg curl wget
            ;;
        arch|manjaro|endeavouros|garuda)
            pacman -Sy --noconfirm python python-pip ffmpeg curl wget
            ;;
        *)
            warn "Unknown distro '$DISTRO'. Attempting generic install with pip..."
            ;;
    esac
    success "System dependencies installed"
}

# ── Create directory structure ────────────────────────────

setup_dirs() {
    info "Creating directories..."
    mkdir -p "$INSTALL_DIR" "$DOWNLOAD_DIR"
    success "Directories ready: $INSTALL_DIR"
}

# ── Python virtualenv + pip packages ─────────────────────

setup_python() {
    info "Setting up Python virtual environment..."
    python3 -m venv "$VENV_DIR"
    source "$VENV_DIR/bin/activate"
    pip install --quiet --upgrade pip
    pip install --quiet flask flask-cors yt-dlp
    deactivate
    success "Python venv ready at $VENV_DIR"
}

# ── Copy server script ────────────────────────────────────

copy_server() {
    info "Copying server script..."
    cp "$SCRIPT_DIR/server.py" "$INSTALL_DIR/server.py"
    chmod +x "$INSTALL_DIR/server.py"
    success "Server script installed"
}

# ── systemd service ───────────────────────────────────────

install_service() {
    if ! command -v systemctl &>/dev/null; then
        warn "systemd not found — skipping service install. Run manually:"
        echo "  PORT=$SERVER_PORT $VENV_DIR/bin/python $INSTALL_DIR/server.py"
        return
    fi

    info "Installing systemd service..."
    cat > /etc/systemd/system/${SERVICE_NAME}.service << EOF
[Unit]
Description=ShiftSaver Download Server
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${INSTALL_DIR}
Environment=PORT=${SERVER_PORT}
Environment=DOWNLOAD_DIR=${DOWNLOAD_DIR}
ExecStart=${VENV_DIR}/bin/python ${INSTALL_DIR}/server.py
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable "${SERVICE_NAME}"
    systemctl start "${SERVICE_NAME}"
    success "Service ${SERVICE_NAME} installed and started"
}

# ── Firewall hint ─────────────────────────────────────────

firewall_hint() {
    echo ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}  FIREWALL: Enable port ${SERVER_PORT}/tcp${NC}"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    if command -v ufw &>/dev/null; then
        echo "  Run: ufw allow $SERVER_PORT/tcp"
    elif command -v firewall-cmd &>/dev/null; then
        echo "  Run: firewall-cmd --permanent --add-port=${SERVER_PORT}/tcp && firewall-cmd --reload"
    elif command -v iptables &>/dev/null; then
        echo "  Run: iptables -A INPUT -p tcp --dport $SERVER_PORT -j ACCEPT"
    fi
    echo ""
}

# ── Show server IP ────────────────────────────────────────

show_info() {
    LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || echo "unknown")
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}  ShiftSaver server is running!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  Server IP  : ${BLUE}${LOCAL_IP}${NC}"
    echo -e "  Port       : ${BLUE}${SERVER_PORT}${NC}"
    echo -e "  Connect in app: ${BLUE}${LOCAL_IP}:${SERVER_PORT}${NC}"
    echo ""
    echo -e "  Downloads  : $DOWNLOAD_DIR"
    echo -e "  Logs       : journalctl -u $SERVICE_NAME -f"
    echo -e "  Stop       : systemctl stop $SERVICE_NAME"
    echo -e "  Restart    : systemctl restart $SERVICE_NAME"
    echo ""
}

# ── Main ──────────────────────────────────────────────────

main() {
    echo ""
    echo -e "${BLUE}  ╔══════════════════════════════╗"
    echo -e "  ║     ShiftSaver Setup v1.0    ║"
    echo -e "  ╚══════════════════════════════╝${NC}"
    echo ""

    install_deps
    setup_dirs
    setup_python
    copy_server
    install_service
    firewall_hint
    show_info
}

main "$@"
