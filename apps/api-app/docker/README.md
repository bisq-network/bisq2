# Dockerised Bisq2 trusted node (api-app)

A headless Bisq2 trusted node (the `api-app`) packaged as a container, so a mobile
client can pair with a self-hosted node without manually running a Java daemon.

The shipped `api_app.conf` is already configured for headless operation: Tor
transport, WebSocket-only API on `127.0.0.1:8090`, and it writes the pairing QR code
to `pairing_qr_code.txt` in the data dir.

> Multi-arch: **linux/amd64** and **linux/arm64** — base images are pinned to multi-arch
> index digests, and CI builds both via the "Release Umbrel images" workflow in bisq-mobile.
> A local `docker build` produces a single-arch image for the host; use `docker buildx
> --platform` for multi-arch.

## Build

The build context is the **repository root** (the image builds the api-app from
source). From the repo root:

```bash
docker build -f apps/api-app/docker/Dockerfile -t bisq2-api .
```

## Run

All persistent state (identity, db, tor, pairing) lives under a single `/data` mount,
so backing up that one directory captures everything.

```bash
docker run --rm \
  -v bisq2-api-data:/data \
  -p 127.0.0.1:8090:8090 \
  bisq2-api
```

The node uses its **own bundled Tor** by default, so this works standalone. The
pairing QR code is written to `/data/pairing_qr_code.txt`.

> Always pass a named `-v <name>:/data` mount (as above). The image declares
> `VOLUME /data`, so running without `-v` creates a fresh **anonymous** volume on
> every `docker run` — your node identity/state is lost between runs and orphaned
> volumes accumulate on the host.

## Web UI sidecar (the full app)

The node is headless; the **web UI sidecar** ([`qr-ui/`](qr-ui/)) gives it a browser
pairing/status page. The two containers run together — the sidecar shares the node's
network namespace (to reach `127.0.0.1:8090`) and reads the same `/data` volume:

```bash
# build both images
docker build -f apps/api-app/docker/Dockerfile -t bisq2-api .
docker build -f apps/api-app/docker/qr-ui/Dockerfile --build-arg APP_VERSION=2.1.11.1 \
  -t bisq2-api-web-ui apps/api-app/docker/qr-ui

# run the node — publish BOTH ports (the sidecar's 8091 rides on the node's netns).
# Bound to 127.0.0.1 ONLY: 8091 serves the pairing code unauthenticated (see warning below),
# so it must never be reachable from the LAN. Loopback keeps it on this host for testing.
docker run -d --name bisq2-api -v bisq2-api-data:/data -p 127.0.0.1:8090:8090 -p 127.0.0.1:8091:8091 bisq2-api

# run the sidecar, sharing the node's netns + reading the data volume read-only
docker run -d --name bisq2-web --network container:bisq2-api -v bisq2-api-data:/data:ro bisq2-api-web-ui

# pairing/status page: http://127.0.0.1:8091
```

> Because the sidecar uses `--network container:bisq2-api` it has no network of its own:
> `docker ps` shows **both** 8090 and 8091 published on `bisq2-api`, and `bisq2-web` with
> no published ports. The sidecar does not (and cannot) publish a separate 8091.

> [!WARNING]
> **`/pairing-code` is an unauthenticated, cleartext bearer secret that grants trade
> control of the node.** The sidecar serves the pairing code on `8091` with no
> authentication of its own — anyone who can reach `8091` can pair as your node and
> control trades. There is no auth, TLS, or rate-limit in front of it inside this repo.
>
> Its confidentiality rests **entirely on a deployment convention**: `8091` must never be
> published to an untrusted network. This holds in two ways:
> - **Production (Umbrel):** the compose does **not** publish `8091` to the host — the app
>   is reached only through Umbrel's authenticated app-proxy, which is the real auth layer.
> - **Local testing:** the `-p 127.0.0.1:8091:8091` above binds loopback **only**. Do
>   **not** change it to `-p 8091:8091` (which binds `0.0.0.0`) — that leaks node control
>   to your entire LAN, in cleartext. The same applies to `8090` (the node API).

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `BISQ_DATA_DIR` | `/data` | Data directory (single mount for all state). |
| `PAIRING_TTL_SECONDS` | `86400` | Pairing-code lifetime (24h). |
| `JAVA_OPTS` | — | Extra `-Dapplication.*` overrides, appended. |
| `TOR_CONTROL_PORT` | — | If set, use an external Tor instead of the bundled one (see below). |
| `TOR_CONTROL_HOST` | `127.0.0.1` | External Tor control host. |
| `TOR_SOCKS_PORT` / `TOR_SOCKS_HOST` | — / `127.0.0.1` | External Tor SOCKS endpoint. |
| `TOR_COOKIE_AUTH_FILE` | — | Path to the external Tor control auth cookie. |

> **Security:** `JAVA_OPTS` and the `TOR_*` variables are a host-operator trust boundary —
> they can alter JVM/Tor behaviour. Only the operator who controls the host should set
> them; do **not** expose them as user-editable fields in an app-store manifest.

### External Tor

Setting `TOR_CONTROL_PORT` makes the entrypoint write `${BISQ_DATA_DIR}/tor/external_tor.config`
and enable external-Tor mode (`TOR_SKIP_LAUNCH=1`).

> FIXME(external-tor): bisq2's external-Tor mode talks to Tor's **control port** (with
> cookie auth) and discovers the SOCKS port from it — it does not just consume a SOCKS
> port. `TorSocksProxyFactory` also hardcodes `127.0.0.1`, so the external Tor must be
> reachable on the container's loopback. Reusing a host/shared Tor needs its control
> port + auth cookie exposed on loopback inside the container; this is wired up when
> integrating with the target self-hosting platform's bundled Tor.

## Publishing the images (interim — manual)

Until a release pipeline exists, images are built and pushed by hand to a personal GHCR
namespace (`ghcr.io/<you>/…`). One-time: create a GitHub PAT with the `write:packages`
scope, then:

```bash
echo "$GITHUB_PAT" | docker login ghcr.io -u <you> --password-stdin

VERSION=2.1.11.1
docker build -f apps/api-app/docker/Dockerfile -t ghcr.io/<you>/bisq2-api:$VERSION .
docker build -f apps/api-app/docker/qr-ui/Dockerfile --build-arg APP_VERSION=$VERSION \
  -t ghcr.io/<you>/bisq2-api-web-ui:$VERSION apps/api-app/docker/qr-ui
docker push ghcr.io/<you>/bisq2-api:$VERSION
docker push ghcr.io/<you>/bisq2-api-web-ui:$VERSION
```

Then set both packages to **public** in their GHCR settings so Umbrel can pull without
auth. Version scheme: `<bisq2-version>.<image-build>` (e.g. `2.1.11.1`) — bump the 4th
component for image-only releases on the same bisq2 core.

> **Record the pushed digests.** `docker push` prints a `sha256:…` digest for each image
> (or read it back with `docker inspect --format='{{index .RepoDigests 0}}' <image>:$VERSION`).
> Note both digests and share them with the Umbrel maintainers, and prefer referencing
> images by digest (`…@sha256:…`) rather than the floating tag in the app manifest, so what
> ships can be verified against exactly what was published.

> TODO(release): replace this with a GitHub Actions workflow in `bisq-network/bisq2` that
> builds multi-arch and pushes with the repo `GITHUB_TOKEN` (no personal PAT). The Umbrel
> manifest's image refs move from `ghcr.io/<you>/…` to `ghcr.io/bisq-network/…` then.

## Notes

- Memory: set the container `mem_limit` to ~2g; the host should have ~4GB of free RAM.
- TODO(build-speed): revisit the build-from-source approach near the finish line —
  once a release pipeline produces the distribution, the Dockerfile can copy a
  pre-built `installDist` instead of rebuilding from source.
