#!/bin/bash
# Bisq2 - Tails OS preparation script
# Copies the onion-grater profile required for Bisq2 to work on Tails and migrates data
# written to the volatile home directory by earlier versions into Persistent Storage.
# Must be run with root privileges.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BISQ_LIB_DIR="$SCRIPT_DIR"
ONION_GRATER_SRC="$BISQ_LIB_DIR/onion-grater/40_bisq_tails.yml"
ONION_GRATER_DEST="/etc/onion-grater.d/bisq.yml"
PERSISTENT_STORAGE_DIR="/home/amnesia/Persistent"
PERSISTENT_DATA_DIR="$PERSISTENT_STORAGE_DIR/Bisq2"
VOLATILE_DATA_DIR="/home/amnesia/.local/share/Bisq2"

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

cp "$ONION_GRATER_SRC" "$ONION_GRATER_DEST"
chmod 644 "$ONION_GRATER_DEST"
echo "Onion-grater profile installed to $ONION_GRATER_DEST"
echo "Restart onion-grater service ..."
systemctl restart onion-grater.service

# Bisq2 uses Persistent Storage as its data directory whenever it is unlocked, so no symlink is
# needed. Running amnesically is a supported choice, so a locked volume is a warning, not an error.
if [ -d "$PERSISTENT_STORAGE_DIR" ]; then
    DATA_DIR="$PERSISTENT_DATA_DIR"
    mkdir -p "$DATA_DIR"

    # Earlier versions wrote to the volatile home directory; migrate that data once.
    if [ -L "$VOLATILE_DATA_DIR" ]; then
        # Symlink created by an earlier version of this script; the app no longer needs it.
        rm -f "$VOLATILE_DATA_DIR"
        echo "Removed obsolete symlink: $VOLATILE_DATA_DIR"
    elif [ -d "$VOLATILE_DATA_DIR" ]; then
        if [ -n "$(find "$DATA_DIR" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
            echo "Error: Both $VOLATILE_DATA_DIR and $DATA_DIR contain data." >&2
            echo "Refusing to merge them because that would overwrite the persistent profile." >&2
            echo "Back up both directories and keep the one you want at $DATA_DIR, then re-run." >&2
            exit 1
        fi
        echo "Migrating $VOLATILE_DATA_DIR into $DATA_DIR ..."
        # Use cp (always present) instead of rsync (not guaranteed). cp -a == -dR --preserve=all,
        # preserving mode, ownership, timestamps, symlinks, hard links, ACLs and xattrs. The trailing
        # "/." copies the directory contents (including dotfiles) into the existing $DATA_DIR.
        cp -a -- "$VOLATILE_DATA_DIR"/. "$DATA_DIR"/
        echo "Migration complete. The original is left at $VOLATILE_DATA_DIR until the next reboot."
    fi

    chown -R amnesia:amnesia "$DATA_DIR"
else
    DATA_DIR="$VOLATILE_DATA_DIR"
    echo "Warning: Persistent Storage is not unlocked, so Bisq2 uses $DATA_DIR." >&2
    echo "All data, including identity keys and open offers, will be lost on shutdown." >&2
fi

# The external Tor config is only auto-detected when absent, so drop a stale file written
# before Tails was detected. Bisq2 recreates it with the onion-grater control port.
STALE_EXTERNAL_TOR_CONFIG="$DATA_DIR/tor/external_tor.config"
if [ -f "$STALE_EXTERNAL_TOR_CONFIG" ]; then
    rm -f "$STALE_EXTERNAL_TOR_CONFIG"
    echo "Removed existing external Tor config: $STALE_EXTERNAL_TOR_CONFIG"
fi

echo "Preparations complete. Bisq2 uses $DATA_DIR."
