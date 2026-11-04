# Bisq 2.1.15 Notable Changes

Based on the net changes from `v2.1.14` to `release-candidate-2.1.15` at `890386ab407fedda22a69089a39b11c4b4bb3066`. See the [full release notes](full-release-notes.md) for upgrade requirements and source references.

## Tor and Privacy

- Bisq runs its bundled Tor on Linux, macOS and Windows. Linux packages no longer require the system `tor` package. The bundle uses Tor Browser 16.0 and Tor 0.4.9.15.
- The Tor which Bisq starts runs inside the sandbox of sandy 1.0.0 by default. Startup fails when no supported sandbox level can run or a requested operation is forbidden. An operator can turn the sandbox off explicitly.
- Torrc overrides in `bisq.conf` which make Tor run a program or load a library (`ClientTransportPlugin`, `ServerTransportPlugin`, `AccelName`, `AccelDir`) stop the start of Bisq's own Tor. Program arguments and JVM options can still set them.
- The Tor shutdown also stops the processes which Tor started. Processes which it could not stop are reported and stopped again at the next start. The Bisq log no longer names the relay or bridge of a bootstrap warning.
- Built-in or custom bridges can be configured in `bisq.conf`. Bisq selects the bundled pluggable transport program for each bridge. Bridges never silently fall back to a direct connection.
- The desktop offers a confirmation switch to permit pluggable transports in the sandbox. Bridge selection stays in `bisq.conf`. Windows cannot run pluggable transports inside the sandbox.
- External Tor supports control-port authentication, including SAFECOOKIE, and checks that its SOCKS port does not report disabled authentication-based circuit isolation.
- Bisq moves existing Tor state to `tor/data` and excludes recreatable Tor state, binaries and the `tor/data-mode` record from backups. The backup still includes `external_tor.config`, which may hold a control password.

## API for Mobile Clients

- `GET /api/v1/info` returns the node version, the image version and the supported API features.
- `GET /api/v1/trades/{tradeId}/account-data-banned` lets a buyer check whether the seller's payment account data is banned.
- The offerbook API shows the same offers as the desktop offerbook, plus the user's own offers. Each item has the advisory field `hasSellerSufficientReputation`. `NUM_OFFERS` counts the same offers that `OFFERS` sends.
- The API deletes a stale pairing QR code file at start and at shutdown. Cancelling a trade writes "cancelled" instead of "rejected" into the trade log.

## Desktop and Application Reliability

- A desktop notice explains when Tor runs with a weaker sandbox level. Network information shows the running Tor version. Tor logs are accessible from the resources page.
- Seed nodes, oracle nodes and the API application exit with a failure status when startup fails, so service managers and container runtimes can restart them. The API application also exits when its HTTP server does not run. The API waits for the Tor transport before publishing its onion service.
- A service which fails to shut down no longer stops the shutdown of the services after it.
- Network stores start removing expired entries only after the stored data is read. Shared decoded public keys reduce memory use.
- HTTP clients limit response bodies after decoding; the Tor client also bounds status lines and headers. Oversized responses fail instead of using unbounded memory.
- Filesystem cleanup does not follow symbolic links or Windows junctions. Shutdown closes Tor HTTP resources. Windows detection and external-Tor status updates are corrected.

## Build and Verification

- The Tor library is a pinned Git submodule. Initialize it when building from a checkout.
- The build uses Gradle 9 and JDK 21.0.13. CI covers Linux, Intel and Apple Silicon macOS, and Windows.
- Bundle and sandy verification use pinned hashes and signing rules. The sandy 1.0.0 archives are also checked against the signed checksums of the sandy release. Installer generation reports packaging failures.
- `check` accepts every build of the pinned Java update; release builds still need the exact JDK build.
- `publishAll` and JitPack builds publish all modules with versioned internal dependencies. The API Docker build takes the commit hash as the `BUILD_COMMIT` build argument.

## Upgrade Requirements

- Linux users no longer need the system Tor package for Bisq. An existing system Tor service remains installed and running until the operator removes it.
- Operators who use bridges with pluggable transports must enable `sandbox.allowPluggableTransports`, or explicitly turn the sandbox off. Windows requires the sandbox off for those bridges.
- Operators using external Tor must retain SOCKS username/password authentication and `IsolateSOCKSAuth`. Review `external_tor.config` before sharing a backup.
- Move `ClientTransportPlugin`, `ServerTransportPlugin`, `AccelName` and `AccelDir` overrides from `bisq.conf` to JVM options or program arguments. Use `0` and `1`, not `true` and `false`, for boolean torrc overrides.
- Run the API application under a supervisor with a restart policy, and check the cause of repeated restarts.

See [full release notes](full-release-notes.md) for configuration and compatibility details.
