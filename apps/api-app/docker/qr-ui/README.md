# Bisq2 trusted node — web UI sidecar

A tiny **nginx** sidecar that gives the headless Bisq2 trusted node ([`../`](../)) a browser
face: a **pairing/status page** so a self-hoster can pair their Bisq Connect mobile app
(scan a QR or copy the pairing code) and see whether the node is ready — without SSH or
log scraping. This is what opens when the user clicks the app tile on the Umbrel dashboard.

## What it serves

| Path | Purpose |
|---|---|
| `/` | The static pairing/status page (`index.html`). |
| `/pairing-code` | The node's `pairing_qr_code.txt` from the shared `/data` volume (read-only). The page uses the **base64 code** (first chunk) for the copy field + onion, and renders the **QR from the `\n\n\n` ASCII-art block** the node writes (see _QR rendering_). |
| `/api/v1/settings/version` | Reverse-proxied to the node REST API on loopback — a **liveness** probe. The API is auth-gated, so this returns **403 when unauthenticated, which still proves the node answered (= up)**. Only this endpoint is exposed. |
| `/version.json` | The image version baked at build time (see Build). |

## Status model (all client-side)

The page polls every 5s and derives one of four states:

| version endpoint | pairing file | State |
|---|---|---|
| responds | present | **Ready** |
| responds | missing | **Tor bootstrapping** (onion not published yet) |
| no response (never reached) | — | **Starting** |
| no response (was up) | — | **Stopped** |

## QR rendering

The QR is **reconstructed from the ASCII-art block** bisq2 writes in `pairing_qr_code.txt`
(`█`/`▀`/`▄`/space → module grid), **not** re-encoded from the base64. That block is bisq2's
own ZXing output, so the rendered QR is **byte-identical to what Bisq Connect / Desktop
produce** and can never drift. It's drawn pixel-perfect (integer px per module + a 4-module
quiet zone). A tiny inlined JS encoder remains only as the `?demo`-preview / last-resort
fallback if the ASCII block is ever absent.

## How it reaches the node

The node binds its API to `127.0.0.1:8090`. The sidecar therefore shares the node
container's network namespace so loopback is shared:

```yaml
# (docker-compose, lives in the Umbrel app repo — illustrative)
services:
  server:
    image: ghcr.io/bisq-network/bisq2-api:<version>
    volumes: [ "${APP_DATA_DIR}/data:/data" ]
  web:
    image: ghcr.io/bisq-network/bisq2-api-web-ui:<version>
    network_mode: "service:server"   # share the node's loopback -> reach 127.0.0.1:8090
    volumes: [ "${APP_DATA_DIR}/data:/data:ro" ]
```

> Compose's `network_mode: "service:server"` is the same thing as plain Docker's
> `--network container:<name>` (see the parent [`README.md`](../README.md) run example).

## Build

Run from the **repository root** (the `-f` path and build context are repo-root-relative):

```bash
docker build -f apps/api-app/docker/qr-ui/Dockerfile \
  --build-arg APP_VERSION=2.1.11.1 \
  -t bisq2-api-web-ui apps/api-app/docker/qr-ui
```

`APP_VERSION` (default `dev`) is baked into `/version.json` and shown in the footer + info
panel. Scheme: **`<bisq2-version>.<image-build>`** (e.g. `2.1.11.1`) — bump the 4th
component for image-only releases on the same bisq2 core.

The sidecar runs as **uid 999** (the node's `bisq` user) so it can read the node's
owner-only (`0600`) `pairing_qr_code.txt`.

## Dev preview

Open `index.html` with `?demo` in the URL to use the in-page "Preview state" toggle
(renders a sample pairing code and lets you flip through all four states) without a
running node.

## Fonts

IBM Plex Sans (Bisq's brand font) is **vendored locally** under `fonts/` (weights 400/500/600/700,
OFL-1.1 — see `fonts/LICENSE.txt`) and loaded via `@font-face`, so the page never calls a font CDN.

## TODO

- The onion address is derived from the pairing code (decoded client-side) — validated, but
  best-effort; if it ever regresses, wire it from the Tor hostname file or a node endpoint.
- Multi-arch image build (amd64 only for now).
