# Bisq 2.1.16 Notable Changes

Based on the net changes from `v2.1.15` to `release-candidate-2.1.16` at `ada74cf324dab72f4ab68e0987433919683f729f`, plus the subsequent Corretto security update. See the [full release notes](full-release-notes.md) for upgrade requirements and source references.

## Trades

- Trade messages which arrive before the trade can process them are now stored with the trade. Such a trade no longer waits without end after a restart. The node applies them at startup unless trading is halted or the version is below the minimum for trading.
- The event queue of a trade keeps at most one message of each state-changing type, so copies from a peer cannot grow it.
- A trade message which arrives while the maker creates the trade protocol is no longer left behind, and each pending message reaches the protocol only once.
- Closed trades no longer stay among the open trades. Their offers no longer look taken, the API no longer reports them, and deleting a trade from the history removes its data from the store.
- A failed copy of a store no longer stops all later writes of that store.

## Configuration and Startup

- An unreadable or invalid `bisq.conf` stops the start with an error which names the file and the reason. Before, Bisq started without any of its settings, for example without the configured Tor bridges.
- Bisq no longer replaces an unreadable `bisq.conf` with its own settings. The network settings write only changed values.
- When the Tor sandbox refuses the bridges or cannot run, the desktop offers choices: allow pluggable transports, use no bridges, or turn the sandbox off. The choice is appended to `bisq.conf`, and Bisq shuts down.
- A failed start shows one popup instead of two.

## Network and Tor

- A slow first peer exchange no longer fails the start after 90 seconds. The node continues and retries in the background.
- The peer group initializes once and starts no periodic tasks after a shutdown.
- Tor 0.4.9.16 from Tor Browser 16.0.1, with lyrebird 0.10.0. The sandbox uses sandy 1.2.0 and shows fewer routine warnings.
- Tor writes one log, `tor.log`, with its output and its errors. Errors of a failed Tor start quote the reason from that log.
- On macOS, a sandboxed Tor starts also when the data directory is reached through a symbolic link.

## API

- The API application uses the app type `MOBILE_NODE` for alerts. An alert only for `DESKTOP` no longer stops it.

## Other Fixes

- Truncated chat citations no longer split an emoji.
- Linux rpm packages start for every user, also when the build used a restrictive umask.
- The oracle node install script installs Java 21 and finds its service files.

## Build and Verification

- The build and bundled runtime use Amazon Corretto 21 at or above `21.0.13.1`, fixing CVE-2026-114938 (Java4Shell) and retaining the removal of the compromised emSign root certificates.
- CodeQL analysis, a 7-day delay for Dependabot updates of GitHub Actions, and a Transifex token limited to `main` were added.

## Upgrade Requirements

- Check a hand-edited `bisq.conf` before the update. If it cannot be parsed, Bisq 2.1.16 does not start, and headless nodes exit with status 1.
- Read `tor/tor.log` instead of `tor-stdout.log` and `tor-stderr.log`, which Bisq now deletes.
- Security managers: publish a trading halt for `DESKTOP` and for `MOBILE_NODE` when it must stop every node.
- A downgrade to 2.1.15 loses the stored queued trade messages.
- Builders need an Amazon Corretto JDK 21 at or above `21.0.13.1`, and JDK 21 as the Gradle JVM in IntelliJ.

See [full release notes](full-release-notes.md) for configuration and compatibility details.
