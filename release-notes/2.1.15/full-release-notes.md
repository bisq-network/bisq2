# Bisq 2.1.15 Release Notes

Proposed release notes for the `release-candidate-2.1.15` branch. This release changes how Bisq starts and isolates Tor, adds support for external Tor and bridges, and tightens network and build checks.

## Scope

These notes describe the net changes from `v2.1.14` (`945dec0ecb3d4f750060047078037afdc437f070`) to `release-candidate-2.1.15` at `2505caa154f329dd588c3ca9235efe45d1b94248`. The checkout declares version `2.1.15` in `gradle.properties`. These are release-candidate notes, not a claim that version 2.1.15 has been published. Changes were checked against the resulting code, configuration and specifications; the range also contains the extraction of the Tor library into a Git submodule, so the parent repository's file diff alone does not describe the Tor changes.

## Upgrade and Operator Notes

- **Bundled Tor on Linux:** Bisq now runs the Tor expert bundle included in the build on Linux, macOS and Windows. Linux packages no longer require the distribution's `tor` package. An existing system Tor installation and service are not removed by the update; Bisq does not use them for its own Tor start. The bundled Tor is from Tor Browser 16.0, with Tor daemon 0.4.9.15. Linux still needs the system's glibc and `libz.so.1`.
- **Tor sandbox:** Bisq enables the sandy sandbox for the Tor it starts. If the system cannot enforce any supported sandbox level, or the configuration asks for an operation the sandbox cannot allow, Tor startup fails instead of silently starting without the sandbox. Set `application.network.configByTransportType.tor.sandbox.enabled=false` only when the operator accepts running Tor without it. External Tor, including Whonix, is outside this sandbox. Sandbox strength can vary with the operating system and the configured bridges.
- **Bridges and pluggable transports:** `bisq.conf` can select a built-in bridge type with `builtInBridges` or supply complete custom `bridges` lines, but not both. Bisq uses the matching transport programs from its Tor bundle. Bridges that need a pluggable transport require `sandbox.allowPluggableTransports=true` while the sandbox is enabled. The desktop network settings have a switch for that permission, which applies after a restart; bridge selection itself is configured in `bisq.conf`. Windows cannot run pluggable transports inside the sandbox. Use bridges without them or explicitly turn the sandbox off on Windows. A configured bridge that cannot run fails the start; Bisq does not fall back to a direct connection.
- **Existing Tor data:** At the first start, Bisq moves the old Tor state into the new `tor/data` layout. Tor binaries are installed in `tor/bin`; sandy has its own directory. Backups omit the Tor state, bundled binaries and sandy because Bisq can recreate them. A backup retains `external_tor.config`, which can contain an external Tor control password in plain text. Handle and share backups accordingly.
- **External Tor:** Bisq can use external Tor on Whonix, when `TOR_SKIP_LAUNCH` is set, or when its configuration selects external Tor. The configuration file `external_tor.config` supplies its control endpoint and authentication. Cookie authentication uses SAFECOOKIE. External Tor must offer SOCKS username/password authentication and must not disable `IsolateSOCKSAuth` on the port Bisq uses. Bisq refuses a reported `NoIsolateSOCKSAuth` setting; it cannot verify a filtered configuration such as onion-grater on Whonix. An external Tor on another host must be reached over a trusted network because the control connection carries onion-service private keys.
- **Failed headless starts:** Seed and oracle nodes exit with status 1 when application startup fails, allowing a service manager configured with `Restart=on-failure` to restart them. Operators should inspect the cause before allowing repeated retries. The API and node-monitor applications still log an initialization failure without this exit behavior.

The exact configuration, migration, sandbox refusal and compatibility rules are in the [Tor startup specification](../../docs/specifications/network/tor-startup.md). The [build guide](../../docs/dev/build.md) shows `bisq.conf` examples and the data directory.

## Notable Changes

### Tor startup, privacy and desktop controls

- Bisq chooses the control and SOCKS ports of its own Tor, authenticates to its control port, and checks after bootstrap that the SOCKS port belongs to that start. Configuration cannot replace Tor's protected listeners or authentication. Its Tor log is written to `tor/tor-stdout.log`, rotated before another start at 10 MiB, and available from the desktop resources page. The log zip omits the home-directory path. Tor's own notices may still disclose private bridge details; review a log zip before sharing it.
- Tor startup and shutdown now coordinate concurrent callers. A failed start can be retried, and a shutdown waits until no Tor of that service runs. Sandbox refusal messages explain the reason and possible configuration changes. On the desktop, the notice about a weaker sandbox level identifies the missing protection; the user can dismiss each distinct set of weaknesses. Network information shows the running Tor version.
- The network settings switch writes only `sandbox.allowPluggableTransports` into `bisq.conf` and asks for confirmation before enabling it. It is unavailable while the sandbox is off and on Windows. Bridge selection remains a separate configuration step.
- Tor now has a separate repository and a pinned Git submodule. The parent build and packaging use that pinned library. Builders need to initialize the submodule before building.

### Network and application reliability

- HTTP response bodies are bounded after decoding. An oversized or endless response fails the request and discards the connection. Tor HTTP responses also have a bounded status line and headers; malicious responses cannot consume unbounded response-head memory. Failures of one transport are logged without obscuring another transport's failure.
- The Tor HTTP client and HTTP request service close their resources during shutdown. The API waits until the Tor transport is ready before publishing its onion service and binds the API server to the address that Tor or the sandbox relay can reach.
- Bisq avoids filesystem deletion through symbolic links and Windows junctions. Tor data migration and cleanup remove the link itself rather than its target. Random network ports use the complete usable range from 1 to 65535.
- A proof-of-work string is bounded, preventing an attacker-controlled value from producing unbounded log or display text. The Windows OS check no longer mistakes Darwin for Windows. The external-Tor indicator refreshes when the Tor transport finishes starting.

### Build and verification

- The build moves to Gradle 9 and JDK 21.0.13. The release JDK, Docker image, CI jobs and JavaFX version are pinned consistently. CI builds on Linux, Intel and Apple Silicon macOS, and Windows; its Tor step prints diagnostics when a test times out.
- Tor bundles and the sandy binaries are verified against pinned hashes and their signing rules. The build refuses malformed or untrusted key material and checks signature validity at signing time. Installer generation fails when `jpackage` fails, times out, or produces no package. Linux installer builds need the package tools listed in the build guide.
- CI includes a nightly check against the previous release. Dependency verification and toolchain selection have been updated for the extracted Tor build. Tests cover Tor startup, sandbox configuration, backup behavior, HTTP bounds, Windows OS detection, and application startup failures.

Details: [Tor startup](../../docs/specifications/network/tor-startup.md), [Tor circuit isolation](../../docs/specifications/network/tor-stream-isolation.md), [HTTP response bounds](../../docs/specifications/network/http-response-bodies.md), [Tor bundle verification](../../docs/specifications/development/tor-bundle-verification.md), [release build environment](../../docs/specifications/development/release-build-environment.md), and [build instructions](../../docs/dev/build.md).

## Verification of These Notes

The notes were checked against `git log` and the net code and documentation diff from `v2.1.14` to the stated branch commit, including the Tor submodule at its pinned revision. They describe behavior in the release candidate; no application build or runtime upgrade was performed while writing them. Release testing remains separate from this documentation review.
