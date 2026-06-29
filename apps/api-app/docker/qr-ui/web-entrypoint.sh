#!/bin/sh
# Entrypoint for the Bisq 2 web-UI sidecar: run nginx with a network-namespace watchdog.
#
# The sidecar shares the node container's network namespace (compose
# network_mode: "service:server"), which is how it reaches the node's loopback API on
# 127.0.0.1:8090. The catch: if the node restarts ON ITS OWN — a crash handled by its
# restart policy, or a node-only update — this container is left attached to the node's
# OLD, now-dead netns. nginx keeps running but is no longer reachable, so Umbrel's
# app-proxy returns ECONNREFUSED until something restarts us.
#
# Watchdog: once the node's loopback API has been seen up at least once, treat a
# sustained loss of it as an orphaned netns and exit non-zero. The orchestrator's restart
# policy (Umbrel sets `restart: on-failure`) then relaunches us INTO the node's CURRENT
# netns. "Arm after first success" avoids false alarms during the node's initial boot;
# a node that is merely down just makes us cycle harmlessly until it returns.
set -eu

PROBE_URL="http://127.0.0.1:8090/api/v1/settings/version"  # any HTTP reply (e.g. 403) = netns+node alive
INTERVAL="${WATCHDOG_INTERVAL:-10}"     # seconds between probes
THRESHOLD="${WATCHDOG_THRESHOLD:-6}"    # consecutive failures (after arming) before re-attach (~60s)

# Start nginx via the base image's entrypoint so its non-root temp/pid setup still runs.
/docker-entrypoint.sh nginx -g 'daemon off;' &
NGINX_PID=$!

armed=0
fails=0
while sleep "$INTERVAL"; do
    # If nginx itself died, let the container exit and be restarted.
    kill -0 "$NGINX_PID" 2>/dev/null || { echo "[watchdog] nginx exited"; exit 1; }

    # curl exit 0 = got an HTTP response (even 403) -> shared netns is live.
    # non-zero (7 connection refused / 28 timeout) = netns orphaned or node down.
    if curl -s -o /dev/null --max-time 3 "$PROBE_URL"; then
        armed=1
        fails=0
    elif [ "$armed" -eq 1 ]; then
        fails=$((fails + 1))
        echo "[watchdog] node loopback unreachable ${fails}/${THRESHOLD}"
        if [ "$fails" -ge "$THRESHOLD" ]; then
            echo "[watchdog] netns likely orphaned — exiting so restart re-attaches us"
            kill "$NGINX_PID" 2>/dev/null || true
            exit 1
        fi
    fi
done
