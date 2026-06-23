# syntax=docker/dockerfile:1
#
# Dockerised Bisq2 trusted node (api-app) — headless daemon for mobile clients.
#
# Build context MUST be the bisq2 repository root, e.g. from the repo root:
#   docker build -f apps/api-app/docker/Dockerfile -t bisq2-api .
#
# Currently linux/amd64 only.
# TODO(multi-arch): add linux/arm64 once the release pipeline does multi-arch builds.

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
# Pinned by digest so the build is reproducible from the repo alone (the floating
# :21 tag moves). Bump the digest when intentionally updating the build JDK.
FROM azul/zulu-openjdk:21@sha256:fcf711e8fbcb7aa1371e1f831e5114c677e6e9134d9fdd2ee866c7d1027846e2 AS build
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
# Azul Zulu JRE to match the build JDK vendor (and bisq1's runtime), pinned by digest
# for reproducibility. Bump the digest when intentionally updating the runtime JRE.
FROM azul/zulu-openjdk:21-jre@sha256:97defd96b4ff0e00e3bce1ce66a3bc5a3d69dc05df04fc55030dc6f3b6376771

LABEL org.opencontainers.image.title="Bisq2 trusted node (api-app)" \
      org.opencontainers.image.description="Headless Bisq2 node exposing the api-app REST/WebSocket API, e.g. for mobile clients" \
      org.opencontainers.image.source="https://github.com/bisq-network/bisq2" \
      org.opencontainers.image.licenses="AGPL-3.0-only"

# bisq2's bundled Tor mode on Linux launches the SYSTEM tor binary
# (TorService#getTorBinaryPath -> EmbeddedTorProcess.getSystemTorPath()), so it must
# be installed for the default (self-contained) Tor mode to work.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tor \
    && rm -rf /var/lib/apt/lists/*

# Non-root runtime user. All persistent state lives under the /data mount.
RUN useradd --system --create-home --home-dir /home/bisq --shell /usr/sbin/nologin bisq \
    && mkdir -p /data \
    && chown -R bisq:bisq /data

# The trailing slash copies the distribution's CONTENTS (bin/, lib/) into
# /opt/bisq2-api/, so the launcher lands at /opt/bisq2-api/bin/api-app.
COPY --from=build /src/apps/api-app/build/install/api-app/ /opt/bisq2-api/
# Fail the build early if the launcher isn't where the entrypoint expects it.
RUN test -x /opt/bisq2-api/bin/api-app
COPY apps/api-app/docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

USER bisq
WORKDIR /opt/bisq2-api

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
