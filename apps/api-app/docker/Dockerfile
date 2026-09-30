# syntax=docker/dockerfile:1
#
# Dockerised Bisq2 trusted node (api-app) — headless daemon for mobile clients.
#
# Build context MUST be the bisq2 repository root, e.g. from the repo root:
#   docker build -f apps/api-app/docker/Dockerfile -t bisq2-api .
#
# Multi-arch: linux/amd64 + linux/arm64. The base images are pinned to multi-arch index
# digests and the build is arch-agnostic, so `docker buildx --platform` produces both.
# Built by the "Release Umbrel images" workflow in bisq-mobile.

# ── Build stage: compile the api-app distribution from source ──────────────────
# Self-contained: the image build runs the Gradle build itself, so no host build
# step is required and the result is reproducible from the repo alone.
#
# Must be an Azul Zulu JDK 21: bisq2's Gradle toolchain pins vendor=AZUL
# (e.g. :apps:desktop:bi2p), so a non-Azul JDK makes Gradle try to auto-download
# Zulu — which fails (and picks the wrong arch) inside the container. This mirrors
# the host's sdkman toolchain (.sdkmanrc -> java=21.x.fx-zulu).
# TODO(build-speed): revisit this build strategy near the finish line — once a
# release pipeline produces the distribution, this can switch to copying a
# pre-built `installDist` artifact instead of rebuilding from source each time.
#
# Pinned by MULTI-ARCH INDEX digest so the build is reproducible AND buildx can resolve both
# amd64 and arm64 from it (the floating :21 tag moves). Bump when intentionally updating the JDK.
FROM azul/zulu-openjdk:21@sha256:fa2396e3484ca23385063ad05d5bccdf6408a0736f196e3891b9917c8f37eeb2 AS build
WORKDIR /src
COPY . .
# installDist builds the api-app launcher plus every sibling module it depends on.
# We deliberately do NOT run the full `build` task — packaging the daemon does not
# need the whole repository's test suite.
# auto-download=false forces Gradle to use the image's Azul JDK (auto-detected via
# JAVA_HOME) rather than attempting to provision a toolchain (which fails in-container).
# The --mount cache keeps the Gradle home (downloaded deps + compiled module output)
# across builds, so a source change recompiles incrementally instead of from scratch.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon \
    -Dorg.gradle.java.installations.auto-download=false \
    :apps:api-app:installDist

# ── Runtime stage: minimal JRE + system Tor + the built distribution ───────────
# Azul Zulu JRE to match the build JDK vendor (and bisq1's runtime), pinned by MULTI-ARCH
# INDEX digest for reproducibility + arm64. Bump when intentionally updating the runtime JRE.
FROM azul/zulu-openjdk:21-jre@sha256:1c037894a0959926d02d7715f20bd31d7f6a492fc5c2055539800d1e03771761

LABEL org.opencontainers.image.title="Bisq2 trusted node (api-app)" \
      org.opencontainers.image.description="Headless Bisq2 node exposing the api-app REST/WebSocket API, e.g. for mobile clients" \
      org.opencontainers.image.source="https://github.com/bisq-network/bisq2" \
      org.opencontainers.image.licenses="AGPL-3.0-only"

# bisq2's bundled Tor mode on Linux launches the SYSTEM tor binary
# (TorService#getTorBinaryPath -> EmbeddedTorProcess.getSystemTorPath()), so it must
# be installed for the default (self-contained) Tor mode to work. The bundled Tor
# archive bisq2 ships for macOS/Windows is never used here, so the Tor security
# level of this image is decided by this install step alone.
#
# Ubuntu's own `tor` package lags Tor security releases, so it comes from the Tor
# Project's repository (signed by its archive key, fingerprint pinned below). The build
# fails when the installed tor is older than TOR_MIN_VERSION; newer is fine, so a Tor
# security release reaches the next image build without a Dockerfile change. Bump
# TOR_MIN_VERSION when a release must not ship without a given Tor fix. The apt
# preference makes the Tor Project's package win over Ubuntu's whatever their versions,
# and a failed index fetch fails the build instead of quietly falling back to Ubuntu's.
ARG TOR_MIN_VERSION=0.4.9.13
ARG TOR_REPO_FINGERPRINT=A3C4F0F979CAA22CDBA8F512EE8CBC9E886DDD89
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates gnupg wget \
    && wget -qO- "https://deb.torproject.org/torproject.org/${TOR_REPO_FINGERPRINT}.asc" \
        | gpg --dearmor -o /usr/share/keyrings/deb.torproject.org-keyring.gpg \
    && gpg --no-default-keyring --keyring /usr/share/keyrings/deb.torproject.org-keyring.gpg \
        --list-keys --with-colons | grep -q "^fpr:::::::::${TOR_REPO_FINGERPRINT}:" \
    && . /etc/os-release \
    && printf 'Types: deb\nURIs: https://deb.torproject.org/torproject.org/\nSuites: %s\nComponents: main\nSigned-By: /usr/share/keyrings/deb.torproject.org-keyring.gpg %s\n' \
        "$VERSION_CODENAME" "$TOR_REPO_FINGERPRINT" > /etc/apt/sources.list.d/tor.sources \
    && printf 'Package: tor tor-geoipdb\nPin: origin deb.torproject.org\nPin-Priority: 990\n' \
        > /etc/apt/preferences.d/tor \
    && apt-get -o APT::Update::Error-Mode=any update \
    && apt-get install -y --no-install-recommends tor \
    && installed="$(dpkg-query -W -f='${source:Upstream-Version}' tor)" \
    && { dpkg --compare-versions "$installed" ge "$TOR_MIN_VERSION" \
        || { echo "tor $installed is older than the required $TOR_MIN_VERSION" >&2; exit 1; }; } \
    && apt-get purge -y --auto-remove gnupg wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /root/.gnupg

# Non-root runtime user. All persistent state lives under the /data mount.
# uid pinned to 999 so it stays in sync with the web-UI sidecar, which runs as uid 999
# to read the node's owner-only (0600) pairing_qr_code.txt on the shared /data volume.
RUN useradd --system --uid 999 --create-home --home-dir /home/bisq --shell /usr/sbin/nologin bisq \
    && mkdir -p /data \
    && chown -R bisq:bisq /data

# The trailing slash copies the distribution's CONTENTS (bin/, lib/) into
# /opt/bisq2-api/, so the launcher lands at /opt/bisq2-api/bin/api-app.
COPY --from=build /src/apps/api-app/build/install/api-app/ /opt/bisq2-api/
# Fail the build early if the launcher isn't where the entrypoint expects it.
RUN test -x /opt/bisq2-api/bin/api-app
COPY apps/api-app/docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# NOTE: we intentionally start as root (no `USER bisq` here) so the entrypoint can take
# ownership of a freshly-created, possibly root-owned /data bind mount, then drop to the
# bisq user via setpriv. A named-volume run inherits the image's bisq-owned /data and the
# entrypoint's chown is a no-op. See entrypoint.sh.
WORKDIR /opt/bisq2-api

# Granular distribution version (e.g. 2.1.11.2) reported by the /settings/version endpoint as
# imageVersion, alongside the unchanged core version. Paired clients key config caches on it, so
# it must change with every image release even when the core version does not. Stamped by the
# release workflow; empty (field omitted) for ad-hoc local builds.
ARG IMAGE_VERSION=""
ENV BISQ_IMAGE_VERSION=$IMAGE_VERSION

# Single mount for all persistent state (identity, db, tor, pairing) so a host-level
# backup of this one directory captures everything.
ENV BISQ_DATA_DIR=/data
VOLUME ["/data"]

# WebSocket API. Bound to loopback inside the container by default; the host is
# expected to forward/publish it as appropriate for the deployment.
# TODO(healthcheck): add a HEALTHCHECK once the api-app exposes a dedicated health
# endpoint — a plain GET on the WebSocket-only 8090 isn't a reliable liveness probe,
# and the slim JRE base ships no curl/wget to call it with.
EXPOSE 8090

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
