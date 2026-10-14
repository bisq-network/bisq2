# Bisq 2.1.14 Notable Changes

Based on the net diff from `v2.1.13` to `v2.1.14-integration` at `26943944fee057e4f40a934f196097f3269558fc`. See the [full release notes](full-release-notes.md) for scope, compatibility, and source references.

## Bisq Easy Trade and Payment Validation

- Makers validate take requests before storing a trade and accept only their current, complete offers.
- Contract prices must match the offer. Fixed-price trades allow rounding differences rather than the configurable market-price deviation.
- Buyers enforce seller reputation limits as maker and as taker. As taker, the buyer's node checks every take, also through the API and a mobile node built on this version. Displayed seller maximums fit those limits.
- Missing, old, or inconsistent market prices make the reputation check fail, so Bisq Easy takes wait for current prices.
- While an alert requires a minimum version, takers refuse offers of makers below that version instead of waiting without an answer.
- Bitcoin URI and BOLT 11 invoice amounts must be absent or equal the contract amount. Unsupported BOLT 12 payment data is rejected. Sellers must still check dynamically obtained LNURL or Lightning-address invoices in their wallets. QR scanning keeps the case of legacy Bitcoin addresses.
- Trade messages, chat channels, and mediation participants are bound to the trade and contract. Rejected actions no longer produce misleading trade-log entries.
- Mediators see unverified forwarded messages and receive the newest messages of long trade chats. Each trader's mediation request is accepted once and acknowledged.

## Storage Integrity and Network Limits

- Storage requests gain signature V2, which covers the fields that affect storage decisions and preserves the exact signed payload bytes.
- Until the deadline, updated nodes also verify the legacy signature, and they repair copies whose signature V2 a relay removed. User-profile removals require signature V2.
- Owner-specific storage keys, ownership checks, local metadata, and corrected removal ordering prevent another publisher from interfering with owner data.
- Network envelopes are limited to 4 MiB, handshakes to 64 KiB, and inventory hash-set filters to 100,000 entries. Concurrent inbound handshakes are limited. Pending state and replay queues have tighter bounds and cleanup.
- Inventory uses the new storage-key filter with fallback for older peers. Legacy hash-cash authorization is accepted only for compatible initial handshake requests.

## Tor Privacy

- P2P connections of different identities use different SOCKS credentials. Each Tor HTTP connection uses fresh credentials.
- Bisq requires SOCKS5 username/password authentication and bounds handshake reads. External Tor must retain authentication-based circuit isolation.
- Tor control logs redact private keys. The Tor bundle is updated to `15.0.25`, with binary version `0.4.9.14`. **Log files of versions 2.1.3 to 2.1.13 contain onion private keys; do not share them.**

## Desktop and Chat

- Support chat adds Markdown rendering and visible link destinations. Mentions, edits, channel switching, and private-chat navigation are improved.
- Bisq Easy history adds CSV export and a contact-peer action, and displays the agreed trade price instead of recalculating it from current market prices. CSV exports write formula-like cells as text.
- Language settings add country and currency selection. Trade settings add a price warning threshold, and terms acceptance separates legal terms from risk acknowledgement.
- When stored data cannot be read, Bisq keeps a copy for 90 days and the desktop app lists the affected files.
- macOS update downloads select the installer matching Intel or Apple Silicon. Translations and Linux tray rendering are updated.

## API and Bisq Connect

- New APIs cover chat, contacts, alerts, capability discovery, trade limits, and closed-trade queries. Payment-account APIs add typed fiat and crypto models.
- Identity responses no longer contain the key bundle with its private keys. Session authentication is enabled by default; API clients must support the revised sessions, routes, and DTOs.
- Paired devices can be listed and revoked through the API, revoked in the desktop app, or disconnected by expiring their session. Revocation closes the device's connections and removes its push registrations. The node stores only hashes of client secrets.
- Every WebSocket upgrade request is authenticated. WebSocket subscriptions and mobile trade notifications have improved delivery and filtering, and makers receive a notification when their offer is taken.
- A Docker image of the API app is available for linux/amd64 and linux/arm64.

## Reliability and Development

- Store replacement preserves the current file until the new file is ready. Failed writes report failure and are retried, and file locks protect data directories during operation and backup.
- Individual malformed records no longer stop store loading, replay, or bonded-role revalidation. Bridge recovery retains concurrent catch-up requests and reports unsupported subscription RPCs. Oracle nodes read their stored bridge requests at startup.
- Oracle price data for markets that a node does not know no longer causes disconnects.
- MuSig workflows, mediation, arbitration, custom payouts, and wallet address tools are expanded. **MuSig, its dispute services, and integrated wallet navigation remain restricted to developer-mode activation.**
- CI runs unit tests and separately builds the desktop module. A nightly workflow tests compatibility with the last release. Dependency and Tor bundle signature checks, headless UI testing, desktop automation, and log anomaly scanning are improved.

## Upgrade Requirements

- **Upgrade before 2026-11-25 00:00 UTC.** Updated nodes require V2 storage signatures from that date, also for mailbox messages. Oracle and seed operators must migrate publications; security managers, moderators, and release managers must republish their long-lived data with the updated version.
- `application.bondedRoles.onlyStaticOracleNodes=true` is the default. Networks must use a consistent setting, and non-static oracle operators cannot start with that default. Oracle operators should test the first start on regtest with a copy of their data directory.
- External SOCKS proxies must support username/password authentication. API consumers must account for default session handling and changed account/identity models. A downgrade to 2.1.13 is not supported.
- Fixed-price offers that Bisq Connect published through 2.1.13 nodes are not shown on updated nodes; makers must create them again.
- Security managers should publish a minimum trading version as one alert for `DESKTOP` and one for `MOBILE_NODE`, not as `UNSPECIFIED`, while 2.1.13 nodes trade.
- Check old log files before sharing them, and check `db/unreadable` on headless nodes after the upgrade.
- An unsupported bridge subscription requires upgrading the bridge and restarting the oracle.
