# Bisq 2.1.15 Notable Changes

Based on the net changes from `v2.1.14` to `release-candidate-2.1.15` at `2505caa154f329dd588c3ca9235efe45d1b94248`. See the [full release notes](full-release-notes.md) for upgrade requirements and source references.

## Tor and Privacy

- Bisq runs its bundled Tor on Linux, macOS and Windows. Linux packages no longer require the system `tor` package. The bundle uses Tor Browser 16.0 and Tor 0.4.9.15.
- The Tor which Bisq starts runs inside a sandy sandbox by default. Startup fails when no supported sandbox level can run or a requested operation is forbidden. An operator can turn the sandbox off explicitly.
- Built-in or custom bridges can be configured in `bisq.conf`. Bisq selects the bundled pluggable transport program for each bridge. Bridges never silently fall back to a direct connection.
- The desktop offers a confirmation switch to permit pluggable transports in the sandbox. Bridge selection stays in `bisq.conf`. Windows cannot run pluggable transports inside the sandbox.
- External Tor supports control-port authentication, including SAFECOOKIE, and checks that its SOCKS port does not report disabled authentication-based circuit isolation.
- Bisq moves existing Tor state to `tor/data` and excludes recreatable Tor state and binaries from backups. The backup still includes `external_tor.config`, which may hold a control password.

## Desktop and Application Reliability

- A desktop notice explains when Tor runs with a weaker sandbox level. Network information shows the running Tor version. Tor logs are accessible from the resources page.
- Seed and oracle nodes exit with a failure status when startup fails, so service managers can restart them. The API waits for the Tor transport before publishing its onion service.
- HTTP clients limit response bodies after decoding; the Tor client also bounds status lines and headers. Oversized responses fail instead of using unbounded memory.
- Filesystem cleanup does not follow symbolic links or Windows junctions. Shutdown closes Tor HTTP resources. Windows detection and external-Tor status updates are corrected.

## Build and Verification

- The Tor library is a pinned Git submodule. Initialize it when building from a checkout.
- The build uses Gradle 9 and JDK 21.0.13. CI covers Linux, Intel and Apple Silicon macOS, and Windows.
- Bundle and sandy verification use pinned hashes and signing rules. Installer generation reports packaging failures.

## Upgrade Requirements

- Linux users no longer need the system Tor package for Bisq. An existing system Tor service remains installed and running until the operator removes it.
- Operators who use bridges with pluggable transports must enable `sandbox.allowPluggableTransports`, or explicitly turn the sandbox off. Windows requires the sandbox off for those bridges.
- Operators using external Tor must retain SOCKS username/password authentication and `IsolateSOCKSAuth`. Review `external_tor.config` before sharing a backup.

See [full release notes](full-release-notes.md) for configuration and compatibility details.
