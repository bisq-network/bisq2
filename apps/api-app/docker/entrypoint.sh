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

# A host bind mount (e.g. Umbrel's ${APP_DATA_DIR}/data) is created owned by root, but
# the node runs as the non-root `bisq` user and applies 0700 perms to its data dir at
# startup — which fails with "Operation not permitted" unless it owns the dir. So if we
# start as root, take ownership of /data and re-exec this script as bisq. A named-volume
# `docker run` already starts as bisq (image /data is bisq-owned) and skips this branch.
if [ "$(id -u)" = "0" ]; then
    chown -R bisq:bisq "${DATA_DIR}"
    exec setpriv --reuid=bisq --regid=bisq --init-groups "$0" "$@"
fi

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
# bisq2's external-Tor mode connects to Tor's CONTROL port on TOR_CONTROL_HOST, authenticates
# with SAFECOOKIE if a cookie file is given, and uses the SOCKS port which that Tor reports, on
# TOR_CONTROL_HOST as well. If the external Tor cannot be used, the start fails; the bundled Tor
# is not started instead.
# FIXME(external-tor): Tor publishes the onion services of Bisq with targets on 127.0.0.1 of its
# own host. A Tor in another container reaches them only in a shared network namespace. Wire that
# up when integrating with the target self-hosting platform's bundled Tor.
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
