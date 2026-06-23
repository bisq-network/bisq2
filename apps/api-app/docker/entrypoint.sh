#!/usr/bin/env bash
#
# Entrypoint for the dockerised Bisq2 trusted node (api-app).
#
# The shipped api_app.conf is already headless-correct (TOR transport, websocket
# only, binds 127.0.0.1:8090, writes the pairing QR code to disk). This script only
# points the app at the mounted data dir, applies a couple of deployment overrides,
# and — optionally — wires it to an external Tor process.
set -euo pipefail

# All persistent state (identity, db, tor, pairing) lives under one mount so the
# host's backup mechanism captures everything.
DATA_DIR="${BISQ_DATA_DIR:-/data}"
mkdir -p "${DATA_DIR}"

# Config overrides are passed as `-Dapplication.*` JVM system properties (same
# mechanism the repo's run_api.sh uses).
JAVA_OPTS="${JAVA_OPTS:-} -Dapplication.baseDir=${DATA_DIR}"

# Pairing-code lifetime. An always-on self-hosted node is paired only occasionally,
# so default to 24h (override with PAIRING_TTL_SECONDS).
JAVA_OPTS="${JAVA_OPTS} -Dapplication.api.pairing.ttlInSeconds=${PAIRING_TTL_SECONDS:-86400}"

# ── Optional: use an external Tor process instead of the bundled one ───────────
# Default is the bundled (system) Tor, so a plain `docker run` works standalone.
# Set TOR_CONTROL_PORT to enable external-Tor mode.
#
# FIXME(external-tor): bisq2's external-Tor mode connects to Tor's CONTROL port with
# cookie auth and discovers the SOCKS port from the control server (see TorService);
# it does not simply consume a SOCKS port. TorSocksProxyFactory also hardcodes
# 127.0.0.1, so the external Tor must be reachable on the container's loopback.
# Reusing a host/shared Tor therefore requires its control port + auth cookie to be
# made reachable on loopback inside the container — wire that up when integrating
# with the target self-hosting platform's bundled Tor.
if [ -n "${TOR_CONTROL_PORT:-}" ]; then
    TOR_DIR="${DATA_DIR}/tor"
    mkdir -p "${TOR_DIR}"
    {
        echo "UseExternalTor 1"
        echo "ControlPort ${TOR_CONTROL_HOST:-127.0.0.1}:${TOR_CONTROL_PORT}"
        if [ -n "${TOR_COOKIE_AUTH_FILE:-}" ]; then
            echo "CookieAuthentication 1"
            echo "CookieAuthFile ${TOR_COOKIE_AUTH_FILE}"
        else
            echo "CookieAuthentication 0"
        fi
        if [ -n "${TOR_SOCKS_PORT:-}" ]; then
            echo "SocksPort ${TOR_SOCKS_HOST:-127.0.0.1}:${TOR_SOCKS_PORT}"
        fi
    } >"${TOR_DIR}/external_tor.config"
    export TOR_SKIP_LAUNCH=1
    echo "[entrypoint] external Tor enabled (control ${TOR_CONTROL_HOST:-127.0.0.1}:${TOR_CONTROL_PORT})"
else
    # No external Tor requested: drop any external_tor.config left by a previous
    # external-Tor run, otherwise TorService reads its `UseExternalTor 1` and
    # silently switches to external mode.
    rm -f "${DATA_DIR}/tor/external_tor.config"
    echo "[entrypoint] using bundled Tor"
fi

export JAVA_OPTS
echo "[entrypoint] data dir: ${DATA_DIR}"

exec /opt/bisq2-api/bin/api-app "$@"
