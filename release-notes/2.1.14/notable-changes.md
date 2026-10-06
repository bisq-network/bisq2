# Bisq 2.1.14 Notable Changes

Based on the net diff from `v2.1.13` to `v2.1.14-integration` at `ddebe75934e190609261126e4a0897652d11dfdf`. See the [full release notes](full-release-notes.md) for scope, compatibility, and source references.

## Bisq Easy Trade and Payment Validation

- Makers validate take requests before storing a trade and accept only their current, complete offers. Withdrawn or closed offers cannot be taken again through a previous trade.
- Contract prices must match the offer. Fixed-price trades allow rounding differences rather than the configurable market-price deviation.
- Buyer makers enforce seller reputation limits. Displayed seller maximums fit those limits, and validation rejects missing, old, or inconsistent market prices.
- Bitcoin URI and BOLT 11 invoice amounts must be absent or equal the contract amount. Unsupported BOLT 12 payment data is rejected. Sellers must still check dynamically obtained LNURL or Lightning-address invoices in their wallets.
- Trade messages, chat channels, and mediation participants are bound to the trade and contract. Rejected actions no longer produce misleading trade-log entries.

## Storage Integrity and Network Limits

- Storage requests gain signature V2, covering fields omitted by legacy signatures and preserving the exact signed payload bytes.
- Owner-specific storage keys, ownership checks, local metadata, and corrected removal ordering prevent another publisher from interfering with owner data.
- Network envelopes are limited to 4 MiB, handshakes to 64 KiB, and inventory hash-set filters to 100,000 entries. Pending state and replay queues have tighter bounds and cleanup.
- Inventory uses the new storage-key filter with fallback for older peers. Legacy hash-cash authorization is accepted only for compatible initial handshake requests.

## Tor Privacy

- P2P connections of different identities use different SOCKS credentials. Each Tor HTTP connection uses fresh credentials.
- Bisq requires SOCKS5 username/password authentication and bounds handshake reads. External Tor must retain authentication-based circuit isolation.
- Tor control logs redact private keys. The Tor bundle is updated to `15.0.24`, with binary version `0.4.9.13`.

## Desktop and Chat

- Support chat adds Markdown rendering and visible link destinations. Mentions, edits, channel switching, and private-chat navigation are improved.
- Bisq Easy history adds CSV export and a contact-peer action, and displays the agreed trade price instead of recalculating it from current market prices.
- Language settings add country and currency selection. Trade settings add a price warning threshold, and terms acceptance separates legal terms from risk acknowledgement.
- macOS update downloads select the installer matching Intel or Apple Silicon. Translations and Linux tray rendering are updated.

## API and Bisq Connect

- New APIs cover chat, contacts, alerts, capability discovery, trade limits, and closed-trade queries. Payment-account APIs add typed fiat and crypto models.
- Identity responses no longer expose private keys. Session authentication is enabled by default; API clients must support the revised sessions, routes, and DTOs.
- Paired devices can be revoked or disconnected by expiring their session. WebSocket subscriptions and mobile trade notifications have improved delivery and filtering.

## Reliability and Development

- Store replacement preserves the current file until the new file is ready. Failed writes report failure, and file locks protect data directories during operation and backup.
- Individual malformed records no longer stop store loading, replay, or bonded-role revalidation. Bridge recovery retains concurrent catch-up requests and reports unsupported subscription RPCs.
- MuSig workflows, mediation, arbitration, custom payouts, and wallet address tools are expanded. **MuSig and integrated wallet navigation remain restricted to developer-mode activation.**
- CI runs unit tests and separately builds the desktop module. Dependency signature checks, headless UI testing, desktop automation, and log anomaly scanning are improved.

## Upgrade Requirements

- **Upgrade before 2027-01-01 00:00 UTC.** Updated nodes require V2 storage signatures from that date. Oracle and seed operators must migrate publications; security managers, moderators, and release managers must republish their long-lived data with the updated version.
- `application.bondedRoles.onlyStaticOracleNodes=true` is the default. Networks must use a consistent setting, and non-static oracle operators cannot start with that default.
- External SOCKS proxies must support username/password authentication. API consumers must account for default session handling and changed account/identity models.
- An unsupported bridge subscription requires upgrading the bridge and restarting the oracle.
