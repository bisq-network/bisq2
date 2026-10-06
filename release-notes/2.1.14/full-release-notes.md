# Bisq 2.1.14 Release Notes

Proposed release notes for the Bisq 2.1.14 integration branch. This release strengthens Bisq Easy trade validation, network storage integrity, and Tor privacy. It also expands the API and improves chat, trade history, and desktop settings.

## Scope

These notes describe the net changes from release tag `v2.1.13` (`539cc023496fe0c59c961e32b250442905999fa3`) to branch `v2.1.14-integration` at `ddebe75934e190609261126e4a0897652d11dfdf`.

- Generated on: 2026-10-06.
- The checkout declares version `2.1.14` in `gradle.properties`.
- Comparison: `git diff v2.1.13 ddebe75934e190609261126e4a0897652d11dfdf`.
- The range contains 1,543 reachable commits, including 1,095 non-merge commits, and changes 2,861 files.
- Changes are grouped by resulting behavior. The range includes main-branch merges, repeated implementations, and reversions; commit titles alone do not identify the final changes.

## Compatibility and Operator Notes

- **Storage signature deadline:** updated nodes require signature V2 for signed storage requests from **2027-01-01 00:00 UTC**, according to their own clock. They then reject legacy-only add and remove requests and prune legacy-only stored additions. Upgrade oracle and seed nodes before this deadline. Security managers, moderators, and release managers must republish their long-lived data with the updated version. Older clients cannot publish signed storage data to updated nodes after the deadline. Before the deadline, legacy acceptance still permits removal of V2 signatures; the transition does not give legacy requests V2 protection.
- **Authorized-data publisher migration:** oracle and seed nodes migrate eligible publications to their authorized owner keys. The legacy publisher exception also ends for requests created from 2027-01-01 UTC. Applications use owner publications during the transition. Oracle-node announcements continue to use the network identity key for compatibility with 2.1.13.
- **Oracle authority:** `application.bondedRoles.onlyStaticOracleNodes` defaults to `true` in every application. Only the oracle keys listed in the code may authorize oracle data or operate as oracle nodes under this setting. Non-static oracle operators cannot start with the default. All nodes of a network must use the same setting.
- **Tor proxy requirements:** external Tor proxies must accept SOCKS5 username/password authentication and keep SOCKS authentication isolation enabled. Bisq now rejects a proxy that offers only unauthenticated connections. P2P SOCKS handshake reads now have timeouts.
- **API sessions and models:** session handling is enabled by default in the desktop and API applications. API clients must support session authentication. Identity responses no longer contain `keyBundle` or its private keys. Payment-account routes and DTOs have changed; clients must use the revised typed models and `/payment-accounts/fiat` routes for user-defined fiat accounts.
- **Bitcoin payment data:** buyers must supply payment data without an amount or with the contract's exact Bitcoin amount. Incompatible BOLT 11 invoices and Bitcoin URI amounts are rejected. BOLT 12 offers and invoices are rejected because their requested amounts are not supported by this validator. Sellers must still check amounts returned dynamically through LNURL or Lightning addresses in their wallets.
- **Bisq 1 bridge:** an `UNIMPLEMENTED` subscription RPC is reported as a deployment mismatch and is no longer retried. Upgrade the bridge to provide the RPC, then restart the oracle. Ordinary stream failures still trigger recovery.
- **Developer features:** MuSig activation remains restricted to developer mode. The integrated wallet navigation is tied to MuSig activation. The MuSig and wallet changes below describe development work, not general availability.

Details: [storage signatures](../../docs/specifications/network/storage-request-signatures.md), [publisher migration](../../docs/specifications/network/storage-ownership.md), [oracle authority](../../docs/specifications/bonded-roles/authorized-data.md), and [Tor isolation](../../docs/specifications/network/tor-stream-isolation.md).

## Notable Changes

### Bisq Easy Trade Acceptance and Pricing

- The maker validates a take-offer request before creating and storing the trade or adding the taker to contacts. Rejected requests no longer leave newly created trades behind.
- A take must match a complete, current offer authored by the maker. A trade containing an old offer no longer makes a withdrawn or already closed offer takeable again. Duplicate requests cannot create another trade.
- The contract's price specification must match the maker's offer. The maker derives its expected amounts from that offer, preventing a taker from choosing another contract price.
- Fixed-price trades allow only the rounding difference: the Bitcoin value of one fiat cent plus one satoshi. The configurable market-price deviation no longer permits a worse price for a fixed-price offer. Market-based trades retain their deviation setting.
- A buyer acting as maker checks a seller taker's reputation before signing. The rule uses 200 reputation points per USD, with the larger of a 5% allowance or 200 extra points for positive scores. The displayed seller maximum is rounded and reduced until the seller's score covers it.
- Reputation validation rejects missing market prices, prices at least 12 hours old, or currency and USD prices more than 12 minutes apart. This prevents inconsistent price timestamps from understating the required score.
- Ignored takers receive the same rejection as takers of an unknown offer. Take retries and timeouts recover their UI state, and trading restrictions produce controlled failures.

The underlying gaps were acceptance of taker-controlled contract data and trade creation before validation. The maker now validates against its current offer and local reputation data before committing the trade.

Details: [take-offer validation](../../docs/specifications/trade/take-offer-request-validation.md), [contract prices](../../docs/specifications/trade/bisq-easy-contract-price.md), and [seller limits](../../docs/specifications/trade/bisq-easy-seller-reputation-limit.md).

### Bitcoin Payment Data

- Bitcoin payment data is checked before the seller stores or displays it. The buyer's node checks the same rule before sending, including requests made through the API.
- BOLT 11 invoice amounts and BIP 21 URI amounts must be absent or equal the contract amount. Validation also checks nested Lightning invoices and encoded representations a wallet may interpret.
- Invalid requested amounts fail the receiving trade; invalid buyer API input returns HTTP 400. Desktop validation explains which payment data to provide.

Previously, the trade view showed the agreed amount while a wallet could read a different amount from the buyer's invoice or URI. Validation now binds supported requested amounts to the contract at both sending and receiving boundaries. Dynamically obtained LNURL invoices remain subject to the seller's wallet confirmation.

Details: [Bitcoin payment data](../../docs/specifications/trade/bisq-easy-bitcoin-payment-data.md).

### Trade Messages, Chat Channels, and Mediation

- Confidential trade messages bind the authenticated sender and decrypted receiver to the trade parties' keys. Identity matching tolerates changed transport addresses where key identity is sufficient.
- Trade chat channels derive their identities, offer, and mediator from the trade and its signed contract. A mismatch fails the trade instead of allowing actions through an unrelated channel. Channels are created only for existing trades.
- Trade chat messages and trade-log entries must belong to the signed trade. User-action log entries are sent only after the protocol applies the action.
- Pending trade messages and mediator responses are bounded and replayed when their trade or channel becomes available. Replay isolates individual failures so one invalid message does not stop the remaining messages.
- Mediation validates the request's trade ID, contract amounts, participants, and included chat messages. Moderator reports authenticate their sender and have bounded retention.

Details: [trade senders](../../docs/specifications/trade/trade-message-sender.md), [channel binding](../../docs/specifications/trade/chat-channel-binding.md), and [mediation parties](../../docs/specifications/support/mediation-request-parties.md).

### Network Storage Integrity

- Add, remove, and refresh operations gain signature V2. Its canonical, domain-separated preimage protects versions, ordering fields, payloads, keys, and the legacy signature, as applicable to the request type.
- Higher-level payloads retain their received protobuf `Any` bytes for V2 signature verification. Rebuilding a payload no longer loses bytes that the sender signed.
- Storage keys distinguish owners, or mailbox receivers, so equal data belonging to different owners no longer shares one storage entry. Existing stores recompute keys on load, retaining removal records needed to prevent replay.
- The publisher must own the data. Authorized data must appear inside its authorization wrapper, and applications derive authorized state from the remaining valid publications.
- Storage metadata comes from a local registry. Received values cannot select another storage policy, expiry, or limit.
- Removal ordering prevents an unauthenticated legacy sequence number from permanently blocking a later valid owner update. Refresh requests require V2, update the stored addition, and are relayed.
- Canonical public-key encoding prevents multiple byte representations from creating different owner keys. New ECDSA signatures use low-S encoding; V2 requires it while legacy signatures retain compatibility.
- Canonical encoding supports signed ZigZag integers, nested canonical values, and exact encoded payloads, with explicit signature, storage-key, proof-of-work, and trade-contract purposes. Existing trade contract hashes remain on their current serialization path.

Legacy signatures omitted fields that affect storage decisions. V2 covers those fields; owner-specific keys and ownership checks also prevent another publisher from sharing or replacing the owner's entry. The dated transition preserves communication with 2.1.13 while imposing a deadline for legacy-only storage requests.

Details: [signatures](../../docs/specifications/network/storage-request-signatures.md), [storage keys](../../docs/specifications/network/storage-keys.md), [metadata](../../docs/specifications/network/storage-metadata.md), and [canonical encoding](../../docs/specifications/encoding/canonical-encoding.md).

### Network Limits and Recovery

- Network envelopes are limited to 4 MiB, with a 64 KiB limit during the handshake. Inventory hash-set filters are limited to 100,000 entries.
- Inventory synchronization prefers `HASH_SET_V2`, which uses the new storage keys, and falls back to the legacy filter for older peers.
- Established connections accept supported `HASH_CASH_V2` or `EQUI_HASH` authorization. Legacy `HASH_CASH` is retained only for validated initial handshake requests from compatible older clients. Message cost factors are fixed protocol constants, and proof-of-work checks enforce minimum difficulty.
- Parsing rejects malformed fields and unsupported storage request versions before the requests enter services. An unparseable message within a valid frame is dropped rather than closing the connection; invalid framing and size violations still close it.
- Peer exchange, pending requests, processed confidential messages, and startup replay have bounded state and improved cleanup. Connection registration and store creation handle concurrent operations.
- Inventory requests clean up when connections close, retain complete responses already received, and cancel requests without running cancellation inside a map update.

Details: [framing](../../docs/specifications/network/message-framing.md), [inventory filters](../../docs/specifications/network/inventory-hash-set-filter.md), [authorization](../../docs/specifications/network/authorization-tokens.md), and [parsing](../../docs/specifications/network/validation-at-parsing.md).

### Tor Privacy and Runtime Updates

- Outgoing P2P connections use SOCKS credentials specific to each network identity. Each Tor HTTP connection uses fresh random credentials, separating it from other HTTP connections and P2P traffic.
- Bisq performs the SOCKS5 handshake itself, requires username/password authentication, leaves destination resolution to Tor, and bounds handshake reads.
- This corrects circuit sharing caused by Java's SOCKS client sending the same operating-system username for every identity and HTTP connection. Isolation still depends on Tor configuration and does not remove correlation through timing or request content.
- Tor control logs redact onion private keys, and other diagnostics avoid logging message contents and private account data.
- The Tor bundle version changes from `15.0.22` to `15.0.24`; the declared Tor binary version is `0.4.9.13`. Version details display the binary and bundle versions.
- macOS updates select the Intel or Apple Silicon installer and matching detached signature. A bundled Tor seed address is updated.

Details: [Tor isolation](../../docs/specifications/network/tor-stream-isolation.md), [secrets in logs](../../docs/specifications/security/secrets-in-logs.md), and [installer selection](../../docs/specifications/updater/installer-download-selection.md).

### Chat and Desktop Improvements

- Support chat renders Markdown with safe link handling and visible link destinations. URLs containing user information are rejected. Desktop browser actions accept only web URLs, and untrusted popup text cannot create links.
- Mention selection follows the caret and survives supported edits. Chat input clears when switching channels, private-chat navigation selects the intended channel, and edit actions use the author's identity.
- Private messages and reactions authenticate their sender; private channels validate membership. Public message types and reaction IDs are validated, and queued reactions are bounded and cleaned up. User profiles must meet the original proof-of-work minting difficulty, preventing cheap searches for similar profile icons and names.
- Bisq Easy history adds CSV export and a contact-peer action. CSV includes price, payment method, payment address or invoice, and transaction ID or preimage. History displays the agreed trade price and its percentage relative to the market price at taking, and improves searching and amount formatting.
- Language settings add country and currency selection. Language changes update resources and trigger UI reload handling, including changes through the API.
- Trade settings add a configurable price-deviation warning threshold. Terms acceptance separates legal terms from risk acknowledgement and records versioned acceptance.
- Linux tray transparency, numeric input validation, profile-card offer totals, and unavailable-price display are improved. Montenegro is added to the SEPA country list, and translations are updated.

Details: [safe links](../../docs/specifications/chat/safe-links.md), [history implementation](../../apps/desktop/desktop/src/main/java/bisq/desktop/main/content/bisq_easy/history/BisqEasyHistoryView.java), and [language settings](../../apps/desktop/desktop/src/main/java/bisq/desktop/main/content/settings/language/LanguageSettingsController.java).

### API, Bisq Connect, and Mobile Notifications

- New REST APIs cover public and private chat, contacts, alert notifications, trading-restriction alerts, capability discovery, and trade amount limits. Closed-trade queries support filtering and pagination.
- Payment-account APIs expose typed fiat and crypto accounts, creation payloads, payment-method restrictions, and country-specific bank fields. User-defined fiat account operations move to their dedicated routes.
- Identity DTOs no longer expose asymmetric private keys or Tor/I2P identity keys. The removed `keyBundle` field was unnecessary for current clients; older consumers need coordinated updates.
- Bisq Connect lets users revoke a paired device or expire its session and disconnect it. Expiring a session preserves pairing so the device can reconnect without pairing again.
- WebSocket subscriptions register before building their initial payload, group subscriptions by topic and parameter, and add negotiated compression. This reduces missed updates and duplicate subscriptions.
- Mobile push registration and relay encoding are corrected. Notifications filter duplicate and insignificant trade updates, include category and trade ID, and support optional encryption compatible with iOS CryptoKit.
- User rate limiting is configurable at startup through `application.user.rateLimitEnabled` and remains enabled by default.

Details: [API endpoints](../../api/src/main/java/bisq/api/rest_api/endpoints), [identity DTO](../../api/src/main/java/bisq/api/dto/identity/IdentityDto.java), and [mobile trade notifications](../../bisq-easy/src/main/java/bisq/bisq_easy/BisqEasyMobileTradeNotificationService.java).

### Persistence, Reputation, and Oracle Reliability

- Store writes keep the current file until the complete replacement is ready. Replacement uses an atomic move where supported. Failed serialization, writes, or replacement report failure instead of a successful persistence result.
- Instance protection uses a file lock and fails closed when the lock cannot be established. The lock remains held during backup, preventing another process from opening the same data directory mid-backup.
- Unparseable persisted entries are skipped individually and logged without their private contents. Startup replay likewise isolates failed entries and cleans up derived state when publications disappear.
- Version-2 witness requests require ownership preimages and reject supplied dates. Persisted requests with unused legacy dates normalize those dates without losing ownership bindings. Signed-witness handling aligns with historical arbitrator-signed Bisq 1 records.
- Bridge recovery retains catch-up requests arriving during active recovery. Unsupported subscription RPCs report the required bridge upgrade instead of retrying indefinitely.
- Bonded-role revalidation continues past entries that cannot be replayed, retaining unverifiable registrations for later handling. Multiple valid oracle publications keep a role or account timestamp available until the last valid publication disappears.
- Market-price ingestion rejects non-positive values. Oracle requests bind the authenticated sender to the requested profile, and reputation state clears correctly when source data is removed.
- Webcam QR capture uses bounded frames over the helper process's standard input/output instead of a local TCP listener. Helper failures and shutdown release resources, and stderr goes to a separate rolling log.

The persistence correction removes a window in which the store path was absent and the next startup could treat it as an empty store. Write failure propagation also prevents an oracle from publishing a witness authorization whose ownership binding was not saved.

Details: [store writes](../../docs/specifications/persistence/store-write-results.md), [bridge continuity](../../docs/specifications/network/bisq1-bridge-block-continuity.md), and [authorized data](../../docs/specifications/bonded-roles/authorized-data.md).

### MuSig and Integrated Wallet Development

These features remain gated by developer-mode MuSig activation.

- MuSig offer creation and taking use new domain workflows with payment-account compatibility, payment-method limits, fiat/crypto amount conversion, price warnings, and restored draft preferences.
- Maker-side validation checks live offers, identities, amounts, prices, accounts, and signatures before trade creation. Offer claims and state changes are serialized to prevent concurrent takes from exceeding availability.
- Trade setup checks the peer contract and payment-account payload hashes. Dispute handling adds signed mediation results, custom cooperative payouts, no-payout results, arbitration escalation, and arbitrator case views.
- Mediation handles out-of-order messages and synchronizes state changes. Trade and offer details and chat delivery status are improved.
- The integrated wallet adds address balances, a funds table, persisted dashboard selection, send/receive overlays, and a receive-address wizard with saved address notes and QR display.

Details: [MuSig create offer](../../docs/specifications/offer/mu-sig/create-offer.md), [MuSig take offer](../../docs/specifications/offer/mu-sig/take-offer.md), and [activation gate](../../mu-sig/src/main/java/bisq/mu_sig/MuSigService.java).

### Build Workflows and Verification

- CI builds now run unit tests and include a separate desktop composite-build job. Ordinary JavaFX tests run headlessly; robot-driven UI tests require explicit opt-in.
- Composite dependency verification audits resolved artifacts with strict verification, exact reviewed checksum fallbacks, and reusable inventories that invalidate when dependency inputs change. The policy rejects ignored signing keys already present in the keyring.
- Release checks require the exact signing-key marker, and the release keyring includes updated key validity. Linux ARM64 protobuf tools have explicit verification metadata.
- `cleanAll` and `buildAll` coordinate composite builds in one Gradle invocation. Desktop automation modules, smoke scenarios, a local multi-node runner, and a log anomaly scanner are added.
- Apache HttpClient changes from `5.6.1` to `5.6.4`, and HttpCore from `5.4.2` to `5.4.3`. GitHub Actions use updated pinned checkout, Java setup, and Gradle setup versions.
- Domain specifications and regression tests are added across storage, signatures, trade validation, chat, Tor, persistence, APIs, and UI behavior.

Details: [dependency verification](../../docs/specifications/development/dependency-verification.md), [desktop test execution](../../docs/specifications/development/desktop-test-execution.md), and [build workflow](../../.github/workflows/build_bisq_module.yml).

## Verification of These Notes

The notes were checked against the tag-to-branch file diff, affected implementations, configurations, and specifications. Changes already present at `v2.1.13` are not presented as new solely because they occur in merged commit history. No application build, runtime scenario, or test suite was run while preparing these documentation files; this is not a release certification.
