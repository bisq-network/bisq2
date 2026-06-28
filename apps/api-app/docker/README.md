# Dockerised Bisq2 trusted node (api-app)

A headless Bisq2 trusted node (the `api-app`) packaged as a container, so a mobile
client can pair with a self-hosted node without manually running a Java daemon.

The shipped `api_app.conf` is already configured for headless operation: Tor
transport, WebSocket-only API on `127.0.0.1:8090`, and it writes the pairing QR code
to `pairing_qr_code.txt` in the data dir.

> Currently **linux/amd64** only.
> TODO(multi-arch): add `linux/arm64` once the release pipeline does multi-arch builds.

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
  -p 8090:8090 \
  bisq2-api
```

The node uses its **own bundled Tor** by default, so this works standalone. The
pairing QR code is written to `/data/pairing_qr_code.txt`.

> Always pass a named `-v <name>:/data` mount (as above). The image declares
> `VOLUME /data`, so running without `-v` creates a fresh **anonymous** volume on
> every `docker run` — your node identity/state is lost between runs and orphaned
> volumes accumulate on the host.

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

### External Tor

Setting `TOR_CONTROL_PORT` makes the entrypoint write `${BISQ_DATA_DIR}/tor/external_tor.config`
and enable external-Tor mode (`TOR_SKIP_LAUNCH=1`).

> FIXME(external-tor): bisq2's external-Tor mode talks to Tor's **control port** (with
> cookie auth) and discovers the SOCKS port from it — it does not just consume a SOCKS
> port. `TorSocksProxyFactory` also hardcodes `127.0.0.1`, so the external Tor must be
> reachable on the container's loopback. Reusing a host/shared Tor needs its control
> port + auth cookie exposed on loopback inside the container; this is wired up when
> integrating with the target self-hosting platform's bundled Tor.

## Notes

- Memory: set the container `mem_limit` to ~2g; the host should have ~4GB of free RAM.
- TODO(build-speed): revisit the build-from-source approach near the finish line —
  once a release pipeline produces the distribution, the Dockerfile can copy a
  pre-built `installDist` instead of rebuilding from source.
