#!/bin/bash
# Bisq2 - Tails OS preparation script
# Copies the onion-grater profile required for Bisq2 to work on Tails.
# Must be run with root privileges.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BISQ_LIB_DIR="$SCRIPT_DIR"
ONION_GRATER_SRC="$BISQ_LIB_DIR/onion-grater/40_bisq_tails.yml"
ONION_GRATER_DEST="/etc/onion-grater.d/bisq.yml"
DataDirectory="/home/amnesia/Persistent/bisq/Bisq2"
DEFAULT_EXTERNAL_TOR_CONFIG="/home/amnesia/.local/share/Bisq2/tor/external_tor.config"

if [ "$(id -u)" -ne 0 ]; then
    echo "Error: This script must be run as root (use sudo)." >&2
    exit 1
fi

if [ ! -f "$ONION_GRATER_SRC" ]; then
    echo "Error: Onion-grater profile not found at $ONION_GRATER_SRC" >&2
    exit 1
fi

if [ ! -d "/etc/onion-grater.d" ]; then
    echo "Error: /etc/onion-grater.d directory not found. Is this a Tails system?" >&2
    exit 1
fi

# Gracefully shut down Bisq2 if it is running
if pgrep -x "Bisq2" > /dev/null 2>&1; then
    echo "Bisq2 is running. Sending SIGTERM and waiting for exit ..."
    pkill -SIGTERM -x "Bisq2"
    for i in $(seq 1 30); do
        if ! pgrep -x "Bisq2" > /dev/null 2>&1; then
            echo "Bisq2 has exited."
            break
        fi
        sleep 1
    done
    if pgrep -x "Bisq2" > /dev/null 2>&1; then
        echo "Warning: Bisq2 did not exit within 30 seconds. Exiting..." >&2
        exit 1
    fi
fi

# Remove stale external Tor config if present
if [ -f "$DEFAULT_EXTERNAL_TOR_CONFIG" ]; then
    rm -f "$DEFAULT_EXTERNAL_TOR_CONFIG"
    echo "Removed existing external Tor config: $DEFAULT_EXTERNAL_TOR_CONFIG"
fi

cp "$ONION_GRATER_SRC" "$ONION_GRATER_DEST"
chmod 644 "$ONION_GRATER_DEST"
echo "Onion-grater profile installed to $ONION_GRATER_DEST"
echo "Restart onion-grater service ..."
systemctl restart onion-grater.service

echo "Redirect user data to Tails Persistent Storage at $DataDirectory ..."
if [ ! -d "$DataDirectory" ]; then
    mkdir -p "$DataDirectory"
    chown -R amnesia:amnesia "$DataDirectory"
fi

TARGET_LINK="/home/amnesia/.local/share/Bisq2"
if [ -e "$TARGET_LINK" ]; then
    if [ -L "$TARGET_LINK" ]; then
        echo "Symlink already exists: $TARGET_LINK"
    else
        echo "Warning: $TARGET_LINK exists and is not a symlink; skipping creation" >&2
    fi
else
    ln -s "$DataDirectory" "$TARGET_LINK"
    echo "Created symlink: $TARGET_LINK -> $DataDirectory"
fi

echo "Preparations complete."
