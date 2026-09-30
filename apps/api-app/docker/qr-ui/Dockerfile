# syntax=docker/dockerfile:1
#
# Web UI sidecar for the dockerised Bisq2 trusted node (api-app).
#
# A tiny nginx that serves a static pairing/status page on :8091, reverse-proxies the
# node's version endpoint for a health probe, and serves the pairing-code file from the
# shared /data volume. All page logic (QR generation, the 4 status states) is client-side
# — there is no custom backend.
#
# Build context = this directory:
#   docker build -f apps/api-app/docker/qr-ui/Dockerfile -t bisq2-api-web-ui apps/api-app/docker/qr-ui
#
# Runs alongside the node container, sharing its network namespace so the node API
# (127.0.0.1:8090) is reachable. The docker-compose wiring lives in the Umbrel app repo.
#
# nginx-unprivileged so the worker can run as an arbitrary, non-root uid. We run it as
# uid 999 — the SAME uid as the node's `bisq` user — so it can read the pairing-code file,
# which the node writes owner-only (0600, uid 999). A stock root-master nginx drops its
# workers to uid 101 and would get 403 (Permission denied) reading that file.
# Pinned by multi-arch index digest for reproducibility + arm64 (the floating :alpine tag
# moves). Bump when intentionally updating nginx.
FROM nginxinc/nginx-unprivileged:alpine@sha256:a8d5564c3354241473c1e152d5dd3281ab4224edb61b23c291e0bfd9854687a1

# Image version, shown in the page (footer + info panel). Scheme: <bisq2-version>.<image-build>
# e.g. 2.1.11.1 — the 4th component bumps for image-only releases on the same bisq2 core.
# Pass at build: --build-arg APP_VERSION=2.1.11.1  (CI sets it; defaults to "dev" locally).
ARG APP_VERSION=dev

# Build steps need root to write into the image root; runtime drops to 999:0 below.
USER root
# curl: used by web-entrypoint.sh's netns watchdog to probe the node's loopback API.
RUN apk add --no-cache curl
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/index.html
COPY favicon.png /usr/share/nginx/html/favicon.png
COPY fonts/ /usr/share/nginx/html/fonts/
COPY web-entrypoint.sh /web-entrypoint.sh
RUN chmod +x /web-entrypoint.sh
# Validate APP_VERSION before baking it into JSON (prevents a bad build-arg injecting
# extra JSON fields), then write version.json.
RUN printf '%s' "$APP_VERSION" | grep -Eq '^[0-9A-Za-z._-]+$' || { echo "ERROR: invalid APP_VERSION '$APP_VERSION'"; exit 1; } \
    && printf '{"version":"%s"}\n' "$APP_VERSION" > /usr/share/nginx/html/version.json

# Match the node's runtime uid so the shared, owner-only /data files are readable.
# uid 999 = the node's `bisq` user (reads the 0600 pairing file); gid 0 = the group the
# nginx-unprivileged image makes its temp/pid dirs writable to. Keep uid in sync with the
# node Dockerfile's `bisq` uid.
USER 999:0

# Static page + health-proxy. Bound inside the container; the host/Umbrel app-proxy
# publishes it as appropriate.
EXPOSE 8091

# Run nginx under a netns watchdog (see web-entrypoint.sh): if the node container restarts
# on its own, our shared netns is orphaned — exit so `restart: on-failure` re-attaches us.
ENTRYPOINT ["/web-entrypoint.sh"]
