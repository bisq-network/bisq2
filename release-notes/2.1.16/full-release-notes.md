# Bisq 2.1.16 Release Notes

Proposed release notes for the `release-candidate-2.1.16` branch. This release lets trades continue after a restart when trade messages arrived out of order, removes closed trades from the open trades, stops the start when `bisq.conf` cannot be read, and offers choices in the desktop when the Tor sandbox refuses to start Tor. It also updates Tor to 0.4.9.16 and sandy to 1.2.0, and a slow first peer exchange no longer fails the start.

## Scope

These notes describe the net changes from `v2.1.15` (`cfe8ab251f2b6579f29e9955036afc33ba36861e`) to `release-candidate-2.1.16` at `ada74cf324dab72f4ab68e0987433919683f729f`. The Tor library submodule is pinned at `ad2bcaa5cf30e51e1605d2cf496dadb04b44f31a`, after the Tor library release candidate 1.0.1 (before: `6edc00e7413a1a95767586abc4899a8f0202ac85`, Tor library 1.0.0). The checkout declares version `2.1.16` in `gradle.properties`. These are release-candidate notes, not a claim that version 2.1.16 has been published.

The `v2.1.15` tag is on the release branch and is not an ancestor of `release-candidate-2.1.16`; the release candidate of 2.1.15 reached `main` through a merge. So the commit range `v2.1.15..release-candidate-2.1.16` also lists commits whose changes 2.1.15 already contains under other commit IDs, for example the JitPack and `publishAll` fixes, the reproducible commit hash, the shared decoded public keys, the resolution of `@ExcludeForHash` fields once per class, and the start of the removal of expired store entries. These notes describe only what is new compared with the tree of `v2.1.15`.

## Upgrade and Operator Notes

- **Unreadable `bisq.conf` stops the start:** If `bisq.conf` exists but cannot be read, cannot be parsed, or contains a substitution such as `${x}`, the application no longer starts. Before, it logged an error and started without any setting of the file, for example without the configured Tor bridges, the torrc override `ExcludeNodes`, or the switch of the Tor sandbox. A typing error such as `ExcludeNodes = {ch}` without quotes was enough. The error names the file and the reason. The desktop shows it in a dialog; seed nodes, oracle nodes and the API application log it, print it to the standard error and exit with status 1. A service manager with `Restart=on-failure` then restarts them in a loop until the file is fixed. A `bisq.conf` that links to a missing file and an include with `required()` of a missing file also stop the start. Check `bisq.conf` before the update if it was edited by hand.
- **No rewrite of an unreadable `bisq.conf`:** When the application changes `bisq.conf`, for example from the network settings, it leaves a file which it cannot read as it is and reports an error. Before, it replaced such a file with its own setting alone. The network settings now write only the entries which the user changed, so values which only a program argument or JVM option set are no longer copied into `bisq.conf`.
- **Tor log renamed:** Tor writes its standard output and its standard error, and in the sandbox those of sandy, into one log, `tor/tor.log`, in the order in which they were written. Above 10 MiB the log moves to `tor.log.1` before the next start. At every start, Bisq deletes the logs of earlier versions: `tor-stdout.log`, `tor-stdout.log.1` and `tor-stderr.log`. Scripts or monitoring which read those files must read `tor.log` instead. The error of a failed Tor start now quotes the part of `tor.log` which this start wrote, so it contains the reason which Tor logs.
- **App type of the API application:** The API application now uses the app type `MOBILE_NODE` for alerts instead of `DESKTOP`. It is the trusted node of mobile clients and ships after the desktop app, so an alert which halts trading or requires a minimum version only for `DESKTOP` no longer stops it. Releases of the API application up to 2.1.15 still use `DESKTOP`. Security managers who want to halt trading on every node, for example because of a bug in the trade protocol, must publish the alert for `DESKTOP` and for `MOBILE_NODE`. While a minimum version for trading applies to `DESKTOP` or to `MOBILE_NODE`, a taker of this version refuses makers below the higher of the two minimum versions, also when no alert applies to the taker itself. The trade-offs are in the [trade restrictions specification](../../docs/specifications/trade/trade-restrictions.md).
- **Persisted trades:** A trade now stores the trade messages which its protocol could not process yet, in the new field `queuedTradeMessages` (number 33) of `Trade`. Number 32, the former `submarineTrade` of versions up to 2.1.7, is now reserved. A store of 2.1.15 loads as before. A downgrade to 2.1.15 ignores the field and loses the queued messages, as 2.1.15 did at every restart. A queued trade message can contain the payment account data of the trade peer. The redaction of old trade data does not cover queued messages, so such data stays on disk until the trade reaches a final state.
- **Closed trades from earlier versions:** A store of an earlier version can contain a closed Bisq Easy trade also as an open trade. At the first start, Bisq keeps only the closed trade. A trade which the user deleted from the trade history with an earlier version cannot be recognized this way and stays an open trade without a trade chat channel.
- **Tor directory behind a link:** Bisq now passes the real path of the Tor directory to Tor and to sandy. On macOS, this fixes a sandboxed start which failed with "Unable to open configuration file" when the data directory was reached through a symbolic link. On Windows, a Tor directory behind a link or a junction gets a new AppContainer once.
- **Builders:** The build requires an Azul Zulu JDK 21 at or above `21.0.13.0.3` instead of one exact build. This version removes the emSign root certificates from `cacerts`. Release builds use the newest Zulu build of Java 21 for each operating system. The installers bundle the build JDK as their runtime, so two release builds can bundle different Zulu builds of the same feature version. The API Docker image uses the Zulu 21 image tag, pinned by digest. In IntelliJ, the Gradle JVM must be a JDK 21; a newer JDK can corrupt the Gradle build cache (see the [build guide](../../docs/dev/build.md)).
- **Linux packages:** The Linux installer build first creates the app image, sets the file modes inside it (directories and executables 755, other files 644), runs jpackage under umask 022, and creates the deb and the rpm from it. Before, an rpm built under a restrictive umask, such as 027, installed `/opt/bisq2` so that only the installing user could start Bisq.
- **Oracle node installer:** The Debian install script of the oracle node installs OpenJDK 21 instead of 17, which could not run the oracle node, and finds its systemd unit and environment file again.

The exact rules are in the specifications of [queued trade messages](../../docs/specifications/trade/queued-trade-messages.md), [the configuration file](../../docs/specifications/application/configuration-file.md), [the popup of a failed start](../../docs/specifications/desktop/startup-failure-popup.md), [Tor startup](../../docs/specifications/network/tor-startup.md) and [the release build environment](../../docs/specifications/development/release-build-environment.md).

## Notable Changes

### Trades which continue after a restart

- The mailbox of the network does not keep the order of messages, so a trade message can arrive before the trade can process it. The protocol keeps such a message in the event queue of the trade. Before, this queue existed only in memory. After a restart the trade waited without end for a message which it had already received and acknowledged. Example: a seller restarted before the buyer's Bitcoin address arrived, and then never learned that the buyer had confirmed the fiat payment. The queued trade messages are now stored with the trade.
- At startup, the trade service applies the stored queued messages of each trade once, after it knows the trade restrictions of the stored alerts. During a halt of trading, or below the minimum version for trading, the messages stay queued until the next change of the trade state. A queued message which fails at startup fails its trade and is reported to the peer; a failure of one trade does not stop the start or the other trades.
- The event queue keeps at most one message of each type which only changes the state, so copies from a peer cannot grow the queue. After a state change, the protocol removes other queued events of the same type.
- A trade message which arrives while the maker creates the protocol of the trade is no longer left behind as a pending message. Keeping a message as pending and registering the protocol are now one step, and the node passes each pending message to the protocol only once. Before, a seller who took a buy offer and sent the payment account data right after the request could leave both traders waiting.
- The maker sends the rejection of an invalid take offer request after its trade creation step, so a slow send no longer blocks the creation of other trades.

### Closed trades and trade storage

- A trade is now identified only by its trade ID. Before, the hash code of a trade also covered data which changes while the trade runs, such as its state and protocol version. The removal of a closed trade from the open trades failed without an error in practically every session. As a result, closed trades got a protocol at every start, their offers looked taken, the API reported them as trades, and deleting a trade from the history kept its peer data and contract in the store.
- A failure while a store is copied for a write no longer stops all later writes of that store until shutdown. Before, a crash after such a failure lost every later change. A trade which cannot be serialized is logged by its ID only.

### Configuration and startup failures

- The start fails with a clear error when `bisq.conf` cannot be read (see the operator notes). The desktop shows this error in a translated dialog before the main window opens.
- A failed start of the desktop shows one popup instead of a detailed error popup covered by a second, general warning.
- When Tor cannot start because the sandbox refuses the configured bridges, the popup "Tor cannot use your bridges" names the pluggable transports and the cost of each choice. It offers to allow them in the sandbox or, on Windows, to turn the sandbox off, and in both cases to use no bridges. When Tor cannot run in its sandbox for another reason, the popup "Tor cannot run in its sandbox" shows the error and offers to turn the sandbox off. These popups offer no bug report, because a setting fixes the failure.
- A choice appends its settings to `bisq.conf` and shuts Bisq down. Bisq checks the new file with the same code as the start before it replaces `bisq.conf`, keeps the user's comments and includes, and follows a link to the real file. If the change fails, `bisq.conf` stays as it was, and Bisq shows the lines which the user can add by hand. A choice whose setting a program argument or JVM option overrides is not offered; the popup names that option.
- The network settings show the values of the next start: the command line over `bisq.conf` over the defaults. The switch for pluggable transports in the Tor sandbox is disabled while the command line sets that value.

### Network

- A first peer exchange which does not succeed within 90 seconds, for example through slow obfs4 bridges, no longer fails the start with a bare `TimeoutException`. The start continues, the open requests continue, and the node retries in the background. Versions 2.1.8 to 2.1.15 failed the start in this case.
- The peer group initializes once instead of retrying a step which could not succeed and which left it without housekeeping or keep-alive. A shutdown during the first peer exchange no longer starts periodic tasks after the shutdown.
- Monitor nodes no longer enable session handling, which they do not use.

### Tor

- The bundled Tor is from Tor Browser 16.0.1 with tor 0.4.9.16 (before: Tor Browser 16.0 with tor 0.4.9.15). For clients, the update brings new fallback directories; its other fixes concern directory authorities. The pluggable transport lyrebird moves from 0.9.0 to 0.10.0.
- The sandbox runs with sandy 1.2.0 instead of 1.0.0, with unchanged policies. The build verifies the sandy 1.2.0 archives against pinned hashes and the signed checksums of the sandy release.
- Bisq shows only the sandy warnings which name a weaker sandbox level or a host problem that can make Tor fail or weaken its sandbox. Warnings about accepted platform limits no longer appear at every start.

### API

- The API application uses the app type `MOBILE_NODE` for alerts (see the operator notes). End-to-end tests publish alerts per app type and check that a halt only for `DESKTOP` stops an API application of 2.1.13 but not of this version.

### Other fixes

- Truncated text, for example a cited chat message, no longer splits an emoji or another character outside the Basic Multilingual Plane. Before, the peer could receive a `?` before the ellipsis.
- The bi2p and webcam helper jars contain their `version.txt` also after a clean build.
- The selection of a Burningman fee receiver adds the weights as `long` values instead of `int`, which could overflow.
- The display names of stable coins use the display names of their network and token standard. No current screen or API shows them yet.
- Translations were updated. The new English texts of this release, mainly the startup popups and the configuration error, are not translated yet and appear in English in other languages.

### Build and verification

- The build checks for a minimum Java version instead of one exact JDK build (see the operator notes). CI installs the newest Zulu build of Java 21 on every runner.
- A CodeQL workflow analyzes the Java code and the GitHub workflows. Dependabot proposes new versions of GitHub Actions only after 7 days. The Transifex token is now readable only by workflows on `main`. First-time contributors are told that reviews of their pull requests are paused.
- Tests log failed and skipped tests with their full exceptions in all builds. The storage priority is now an enum. Tests cover the queued and pending trade messages, Bisq Easy trades, the startup popups, `bisq.conf` loading and appending, the network settings and the peer exchange timeout.

Details: [queued trade messages](../../docs/specifications/trade/queued-trade-messages.md), [pending trade messages](../../docs/specifications/trade/pending-trade-messages.md), [trade ID](../../docs/specifications/trade/trade-id.md), [take offer request validation](../../docs/specifications/trade/take-offer-request-validation.md), [store write results](../../docs/specifications/persistence/store-write-results.md), [trade restrictions](../../docs/specifications/trade/trade-restrictions.md), [configuration file](../../docs/specifications/application/configuration-file.md), [popup of a failed start](../../docs/specifications/desktop/startup-failure-popup.md), [peer exchange](../../docs/specifications/network/peer-exchange.md), [Tor startup](../../docs/specifications/network/tor-startup.md), [release build environment](../../docs/specifications/development/release-build-environment.md), and [build instructions](../../docs/dev/build.md).

## Known Limitations

- A restart still loses the local events of the event queue, such as a user action which the protocol could not process yet. Only trade messages are stored.
- The node does not check whether the sender of a queued trade message was banned after the message arrived.
- A queued message which waits during a restriction of trading at startup is applied at the next change of the trade state or at the next start without a restriction, not when the restriction ends.

The full list, with the reasons and what would remove each limitation, is in the [queued trade messages specification](../../docs/specifications/trade/queued-trade-messages.md#known-limitations).

## Verification of These Notes

The notes were checked against `git log`, a patch comparison (`git cherry`) with `v2.1.15`, the net code and documentation diff from `v2.1.15` to the stated branch commit, the commit messages, and the Tor submodule commits from `6edc00e7` to `ad2bcaa5`. Selected claims were checked in the source code: the app type of the API application, the exit of headless applications on an unreadable `bisq.conf`, and the minimum JDK version. No application build or runtime upgrade was performed while writing them. Release testing remains separate from this documentation review.
