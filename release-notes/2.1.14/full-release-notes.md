# Bisq 2.1.14 Release Notes

Proposed release notes for the Bisq 2.1.14 integration branch. This release strengthens Bisq Easy trade validation, network storage integrity, and Tor privacy. It also expands the API and improves chat, trade history, and desktop settings.

## Scope

These notes describe the net changes from release tag `v2.1.13` (`539cc023496fe0c59c961e32b250442905999fa3`) to branch `v2.1.14-integration` at `26943944fee057e4f40a934f196097f3269558fc`.

- Generated on: 2026-10-06. Updated on: 2026-10-13 for the pull requests merged after `ddebe75934e190609261126e4a0897652d11dfdf`.
- The checkout declares version `2.1.14` in `gradle.properties`.
- Comparison: `git diff v2.1.13 26943944fee057e4f40a934f196097f3269558fc`.
- The range contains 1,798 reachable commits, including 1,293 non-merge commits, and changes 3,030 files.
- Changes are grouped by resulting behavior. The range includes main-branch merges, repeated implementations, and reversions; commit titles alone do not identify the final changes.

## Compatibility and Operator Notes

- **Storage signature deadline:** updated nodes require signature V2 for signed storage requests from **2026-11-25 00:00 UTC**, according to their own clock. They then reject legacy-only add and remove requests and prune legacy-only stored additions. Upgrade oracle and seed nodes before this deadline. Security managers, moderators, and release managers must republish their long-lived data with the updated version. Older clients cannot publish signed storage data to updated nodes after the deadline. This includes mailbox messages: updated nodes, including the node of an updated receiver, reject a message that a 2.1.13 node sends through the mailbox. A 2.1.13 node uses the mailbox whenever it has no open connection to the receiver, also when the receiver is online. Trades that are open between a 2.1.13 user and an updated user after the deadline can therefore stall and may need mediation.
- **Before the deadline:** updated nodes accept legacy-only requests, but they also check the legacy signature of every request that carries signature V2. A relay can still remove signature V2 from a request, and updated nodes accept the stripped copy. When the owner's complete request arrives, it replaces that copy, so data of updated owners is not pruned at the deadline. The transition does not give legacy requests V2 protection.
- **User-profile removals:** updated nodes accept a removal of a user profile only with signature V2, also before the deadline. A profile that a 2.1.13 node removes therefore stays on updated nodes until it expires after 15 days.
- **Authorized-data publisher migration:** oracle and seed nodes migrate eligible publications to their authorized owner keys. Updated nodes accept a publication from a key other than the owner's only without signature V2, so this legacy publisher exception ends in practice at the 2026-11-25 deadline. Requests created from 2027-01-01 UTC are refused in any case. Applications use owner publications during the transition. Oracle-node announcements continue to use the network identity key for compatibility with 2.1.13.
- **Oracle authority:** `application.bondedRoles.onlyStaticOracleNodes` defaults to `true` in every application. Only the oracle keys listed in the code may authorize oracle data or operate as oracle nodes under this setting. Non-static oracle operators cannot start with the default. All nodes of a network must use the same setting.
- **Oracle startup:** oracle nodes now read their stored Bisq 1 bridge requests at startup. At the first start after the upgrade, they load and revalidate the stored witness bindings and registrations. Test this first start on regtest with a copy of a real oracle data directory.
- **Tor proxy requirements:** external Tor proxies must accept SOCKS5 username/password authentication and keep SOCKS authentication isolation enabled. Bisq now rejects a proxy that offers only unauthenticated connections. P2P SOCKS handshake reads now have timeouts.
- **Old log files:** log files written by versions 2.1.3 to 2.1.13 contain the onion private keys of all identities of the node. `bisq.log` keeps these lines after the upgrade until it rolls over at 10 MB. The error report of the desktop app now hides the keys in its zip file. The log files on disk, rolled log files, and the logs of headless nodes still contain them. Do not share these log files. Operators of seed and oracle nodes should decide whether to delete old logs and rotate onion keys; a new onion key changes the address of the identity.
- **Unreadable stored data:** when this version cannot read some entries of a private or settings store, it keeps a copy of the original file in `db/unreadable` in the data directory. Copies are deleted after 90 days, and the folder holds at most 100 MB. The copies contain private keys and payment data. The desktop app lists the affected files at startup; headless nodes keep the copies without a notice. An unreadable API access store is deleted instead, so its paired devices must pair again.
- **API sessions and models:** session handling is enabled by default in the desktop and API applications. API clients must support session authentication. Identity responses no longer contain `keyBundle` or its private keys. Payment-account routes and DTOs have changed; clients must use the revised typed models and `/payment-accounts/fiat` routes for user-defined fiat accounts. The node stores only a hash of each client secret. A downgrade to 2.1.13 is not supported.
- **Bitcoin payment data:** buyers must supply payment data without an amount or with the contract's exact Bitcoin amount. Incompatible BOLT 11 invoices and Bitcoin URI amounts are rejected. BOLT 12 offers and invoices are rejected because their requested amounts are not supported by this validator. Sellers must still check amounts returned dynamically through LNURL or Lightning addresses in their wallets. In a trade between a 2.1.13 buyer and an updated seller, payment data that requests another amount makes the seller's node fail the trade, and the traders may need mediation.
- **Bisq Connect fixed-price offers:** updated nodes drop fixed-price offers that Bisq Connect published through 2.1.13 API nodes, because their price is not in the exact form that updated nodes require. Users of updated nodes do not see these offers, and a maker who upgrades must create them again. Trades, trade chats, and mediation cases created from such offers still load.
- **Minimum-version alerts:** while an alert requires a minimum version for trading, the taker's node refuses offers of makers below that version. A node below that version acknowledges the trade messages that it receives and then drops them, and it cannot complete trade steps. Trades that are open between such a node and an updated node therefore stop while the alert applies. Security managers should publish such a minimum version as one alert for `DESKTOP` and one for `MOBILE_NODE`, with the same version. They should not use `UNSPECIFIED` while 2.1.13 nodes still trade: 2.1.13 nodes ignore it, but updated takers would refuse all 2.1.13 makers. An offer of a profile that the maker has not selected since the update keeps the earlier version, so takers refuse it until the maker publishes that profile.
- **Bisq 1 bridge:** an `UNIMPLEMENTED` subscription RPC is reported as a deployment mismatch and is no longer retried. Upgrade the bridge to provide the RPC, then restart the oracle. Ordinary stream failures still trigger recovery.
- **One version per data directory:** close 2.1.13 before you start 2.1.14 on the same data directory. 2.1.13 locks the data directory with a local port, and this version with a lock file, so neither version detects the other.
- **Custom authorization setting:** this version no longer supports the token type `HASH_CASH`. If a custom configuration lists it in `application.network.authorization.myPreferredAuthorizationTokenTypes`, the node does not start. Remove the entry; the default is `HASH_CASH_V2`.
- **Oldest compatible version:** with their default settings, nodes of versions before 2.1.3 use only the legacy `HASH_CASH` authorization, so they cannot exchange messages with nodes of this version.
- **Developer features:** MuSig activation remains restricted to developer mode. The MuSig mediator and arbitrator services also run only in developer mode, so release builds do not process MuSig dispute requests. The integrated wallet navigation is tied to MuSig activation. The MuSig and wallet changes below describe development work, not general availability.

Details: [storage signatures](../../docs/specifications/network/storage-request-signatures.md), [publisher migration](../../docs/specifications/network/storage-ownership.md), [oracle authority](../../docs/specifications/bonded-roles/authorized-data.md), [Tor isolation](../../docs/specifications/network/tor-stream-isolation.md), [secrets in logs](../../docs/specifications/security/secrets-in-logs.md), [unreadable store files](../../docs/specifications/persistence/unreadable-store-files.md), [fixed prices](../../docs/specifications/offer/fixed-price.md), and [trade restrictions](../../docs/specifications/trade/trade-restrictions.md).

## Notable Changes

### Bisq Easy Trade Acceptance and Pricing

- The maker validates a take-offer request before creating and storing the trade or adding the taker to contacts. Rejected requests no longer leave newly created trades behind.
- A take must match a complete, current offer authored by the maker. Duplicate requests cannot create another trade.
- The contract's price specification must match the maker's offer, and the maker derives its expected amounts from that offer.
- Fixed-price trades allow only the rounding difference: the Bitcoin value of one fiat cent plus one satoshi. The configurable price deviation applies only to offers with a market-based price.
- A buyer checks the seller's reputation in both roles. As maker, the buyer's node checks a seller taker before signing. As taker, the buyer's node checks the seller maker before it creates the trade, for every client: the desktop, the REST API, and the mobile node. Both checks use 200 reputation points per USD, with the larger of a 5% allowance or 200 extra points for positive scores. The displayed seller maximum is rounded and reduced until the seller's score covers it.
- Reputation validation rejects missing market prices, prices at least 12 hours old, or currency and USD prices more than 12 minutes apart. Without current market prices, Bisq Easy takes in both directions fail until current prices arrive; the desktop then asks the user to try again later.
- While an alert requires a minimum version, a taker refuses offers of makers below that version. Before, such a take waited without an answer, because the restricted maker drops the request. Open trades with a node below that version stop while the alert applies, because that node drops their messages.
- A node that starts during an emergency halt or a minimum-version alert ignores replayed trade messages, as it ignores new ones.
- Ignored takers receive the same rejection as takers of an unknown offer. Take retries and timeouts recover their UI state, and trading restrictions produce controlled failures.

The maker validates a take against its current offer and its local reputation data before it commits the trade. The buyer's node enforces the seller reputation limit for every take.

Details: [take-offer validation](../../docs/specifications/trade/take-offer-request-validation.md), [contract prices](../../docs/specifications/trade/bisq-easy-contract-price.md), [seller limits](../../docs/specifications/trade/bisq-easy-seller-reputation-limit.md), and [trade restrictions](../../docs/specifications/trade/trade-restrictions.md).

### Bitcoin Payment Data

- Bitcoin payment data is checked before the seller stores or displays it. The buyer's node checks the same rule before sending, including requests made through the API.
- BOLT 11 invoice amounts and BIP 21 URI amounts must be absent or equal the contract amount. Validation also checks nested Lightning invoices and encoded representations a wallet may interpret.
- Invalid requested amounts fail the receiving trade; invalid buyer API input returns HTTP 400. Desktop validation explains which payment data to provide.
- Scanning a QR code keeps the case of legacy Bitcoin addresses, which start with 1 or 3. Before, Bisq changed them to lowercase, and the seller received an invalid address. The `bitcoin:` scheme is accepted in any case.

Validation binds the supported requested amounts to the contract amount when the buyer sends the payment data and when the seller receives it. Dynamically obtained LNURL invoices remain subject to the seller's wallet confirmation.

Details: [Bitcoin payment data](../../docs/specifications/trade/bisq-easy-bitcoin-payment-data.md).

### Trade Messages, Chat Channels, and Mediation

- Confidential trade messages bind the authenticated sender and decrypted receiver to the trade parties' keys. Identity matching tolerates changed transport addresses where key identity is sufficient. Trade messages and trade chat messages are accepted only when they are addressed to the user's identity of that trade.
- Trade chat channels derive their identities, offer, and mediator from the trade and its signed contract. A mismatch fails the trade instead of allowing actions through an unrelated channel. Channels are created only for existing trades.
- Trade chat messages and trade-log entries must belong to the signed trade. User-action log entries are sent only after the protocol applies the action.
- Pending trade messages and mediator responses are bounded and replayed when their trade or channel becomes available. Replay isolates individual failures so one invalid message does not stop the remaining messages.
- Mediation validates the request's trade ID, contract amounts, and participants. Forwarded chat messages that fail validation are skipped instead of rejecting the whole request.
- Mediators see chat messages that the requesting trader forwarded but did not write marked as unverified. The mediator accepts one request per trader and trade, keeps the requests of both traders in one case, and acknowledges each of them. A requester can have at most 10 open cases of other trades.
- A mediation request with a long trade chat keeps the newest messages within the size limit. Earlier versions could drop the newest messages. The full effect of the mediation changes needs a mediator that runs this version.
- Moderator reports authenticate their sender and have bounded retention. Moderators can reply to reports longer than 1,000 characters; the reply quotes the first 1,000 characters.

Details: [trade senders](../../docs/specifications/trade/trade-message-sender.md), [channel binding](../../docs/specifications/trade/chat-channel-binding.md), [mediation parties](../../docs/specifications/support/mediation-request-parties.md), [mediation requests](../../docs/specifications/support/mediation-request-validation.md), and [chat history pruning](../../docs/specifications/support/dispute-chat-history-pruning.md).

### Network Storage Integrity

- Add, remove, and refresh operations gain signature V2. Its canonical, domain-separated preimage protects versions, ordering fields, payloads, keys, and the legacy signature, as applicable to the request type.
- Until the deadline, updated nodes also verify the legacy signature of a request that carries signature V2, so they do not store or serve data that 2.1.13 nodes reject. When a relay has removed signature V2, the owner's complete request replaces the stripped copy.
- Higher-level payloads retain their received protobuf `Any` bytes for V2 signature verification. Rebuilding a payload no longer loses bytes that the sender signed.
- Storage keys distinguish owners, or mailbox receivers, so equal data belonging to different owners no longer shares one storage entry. Existing stores recompute keys on load, retaining removal records needed to prevent replay.
- The publisher must own the data. During the publisher migration, a request from another publisher is accepted only without signature V2. Authorized data must appear inside its authorization wrapper, and applications derive authorized state from the remaining valid publications.
- Storage metadata comes from a local registry. Received values cannot select another storage policy, expiry, or limit.
- Removal ordering prevents an unauthenticated legacy sequence number from permanently blocking a later valid owner update. Refresh requests require V2, are accepted only from the owner, update the stored addition, and are relayed. The user-profile store accepts and keeps only removals with signature V2.
- Canonical public-key encoding prevents multiple byte representations from creating different owner keys. New ECDSA signatures use low-S encoding; V2 requires it while legacy signatures retain compatibility.
- Canonical encoding supports signed ZigZag integers, nested canonical values, and exact encoded payloads, with explicit signature, storage-key, proof-of-work, and trade-contract purposes. Existing trade contract hashes remain on their current serialization path.

Signature V2 covers the fields that affect storage decisions, and owner-specific keys and ownership checks keep the entries of different owners apart. The dated transition preserves communication with 2.1.13 while imposing a deadline for legacy-only storage requests.

Details: [signatures](../../docs/specifications/network/storage-request-signatures.md), [storage keys](../../docs/specifications/network/storage-keys.md), [metadata](../../docs/specifications/network/storage-metadata.md), and [canonical encoding](../../docs/specifications/encoding/canonical-encoding.md).

### Network Limits and Recovery

- Network envelopes are limited to 4 MiB, with a 64 KiB limit during the handshake. Inventory hash-set filters are limited to 100,000 entries.
- Inventory synchronization prefers `HASH_SET_V2`, which uses the new storage keys, and falls back to the legacy filter for older peers. Responses to older peers leave out removals of the publisher migration that a newer owner publication replaces.
- Established connections accept supported `HASH_CASH_V2` or `EQUI_HASH` authorization. Legacy `HASH_CASH` is retained only for validated initial handshake requests from compatible older clients. Message cost factors are fixed protocol constants, and proof-of-work checks enforce minimum difficulty.
- Parsing rejects malformed fields and unsupported storage request versions before the requests enter services. An unparseable message within a valid frame is dropped rather than closing the connection; invalid framing and size violations still close it.
- A node runs at most as many inbound handshakes at the same time as its maximum number of peers, and it closes additional sockets immediately. Slow inbound handshakes no longer block outbound connections.
- Inventory messages check their size and number of entries before their entries are parsed, and sorting serializes each entry once. Unknown capability values from peers are skipped and reported in one log line.
- An HTTP GET request that receives a 4xx response tries the next provider instead of failing.
- Peer exchange, pending requests, processed confidential messages, and startup replay have bounded state and improved cleanup. Connection registration and store creation handle concurrent operations.
- Inventory requests clean up when connections close, retain complete responses already received, and cancel requests without running cancellation inside a map update.

Details: [framing](../../docs/specifications/network/message-framing.md), [inventory filters](../../docs/specifications/network/inventory-hash-set-filter.md), [authorization](../../docs/specifications/network/authorization-tokens.md), [parsing](../../docs/specifications/network/validation-at-parsing.md), [inbound handshakes](../../docs/specifications/network/inbound-handshakes.md), and [HTTP retries](../../docs/specifications/network/http-request-retries.md).

### Tor Privacy and Runtime Updates

- Outgoing P2P connections use SOCKS credentials specific to each network identity. Each Tor HTTP connection uses fresh random credentials, separating it from other HTTP connections and P2P traffic.
- Bisq performs the SOCKS5 handshake itself, requires username/password authentication, leaves destination resolution to Tor, and bounds handshake reads.
- This corrects circuit sharing caused by Java's SOCKS client sending the same operating-system username for every identity and HTTP connection. Isolation still depends on Tor configuration and does not remove correlation through timing or request content.
- Tor control logs redact onion private keys. Other diagnostics avoid logging message contents, profile data, and private account data. API logs no longer contain WebSocket messages, request or response bodies sent over the WebSocket, or pairing code IDs. The desktop error report hides onion private keys in its copies of old log files.
- The Tor bundle version changes from `15.0.22` to `15.0.25`; the declared Tor binary version is `0.4.9.14`. Version details display the binary and bundle versions.
- A Docker image of the API app, a headless trusted node for mobile clients, is added for linux/amd64 and linux/arm64. It installs Tor from the Tor Project's package repository and requires Tor `0.4.9.14` or newer.
- macOS updates select the Intel or Apple Silicon installer and matching detached signature. A bundled Tor seed address is updated.

Details: [Tor isolation](../../docs/specifications/network/tor-stream-isolation.md), [secrets in logs](../../docs/specifications/security/secrets-in-logs.md), [installer selection](../../docs/specifications/updater/installer-download-selection.md), and [Docker image](../../apps/api-app/docker/README.md).

### Chat and Desktop Improvements

- Support chat renders Markdown with safe link handling and visible link destinations. URLs containing user information are rejected. Desktop browser actions accept only web URLs, and untrusted popup text cannot create links.
- Mention selection follows the caret and survives supported edits. Chat input clears when switching channels, private-chat navigation selects the intended channel, and edit actions use the author's identity.
- Private messages and reactions authenticate their sender; private channels validate membership. Public message types, channel IDs, and reaction IDs are validated, and queued reactions are bounded and cleaned up. User profiles must meet the original proof-of-work minting difficulty, preventing cheap searches for similar profile icons and names.
- Opening a private or trade chat selects that chat's profile, also after the profile's terms or statement were edited. A late removal of an older profile version no longer removes a newer version that the node already shows. Profiles with an unknown avatar version show the default icon style instead of breaking icon rendering.
- Bisq Easy history adds CSV export and a contact-peer action. CSV includes price, payment method, payment address or invoice, and transaction ID or preimage. History displays the agreed trade price and its percentage relative to the market price at taking, and improves searching and amount formatting.
- All CSV exports write a cell that a spreadsheet could run as a formula as text, also after a semicolon, tab, or line break. Hidden control characters are removed.
- When stored data cannot be read at startup, the desktop app lists the affected files and can open the folder with their copies.
- Language settings add country and currency selection. Language changes update resources and trigger UI reload handling, including changes through the API.
- Trade settings add a configurable price-deviation warning threshold. Terms acceptance separates legal terms from risk acknowledgement and records versioned acceptance.
- Linux tray transparency, numeric input validation, profile-card offer totals, and unavailable-price display are improved. Montenegro is added to the SEPA country list, and translations are updated.

Details: [safe links](../../docs/specifications/chat/safe-links.md), [CSV export](../../docs/specifications/desktop/csv-export.md), [unreadable store files](../../docs/specifications/persistence/unreadable-store-files.md), [history implementation](../../apps/desktop/desktop/src/main/java/bisq/desktop/main/content/bisq_easy/history/BisqEasyHistoryView.java), and [language settings](../../apps/desktop/desktop/src/main/java/bisq/desktop/main/content/settings/language/LanguageSettingsController.java).

### API, Bisq Connect, and Mobile Notifications

- New REST APIs cover public and private chat, contacts, alert notifications, trading-restriction alerts, capability discovery, and trade amount limits. Closed-trade queries support filtering and pagination.
- Payment-account APIs expose typed fiat and crypto accounts, creation payloads, payment-method restrictions, and country-specific bank fields. National-bank and same-bank accounts can be listed and created. User-defined fiat account operations move to their dedicated routes.
- Identity DTOs no longer expose asymmetric private keys or Tor/I2P identity keys. The removed `keyBundle` field was unnecessary for current clients; older consumers need coordinated updates.
- Bisq Connect lets users revoke a paired device or expire its session and disconnect it. Expiring a session preserves pairing so the device can reconnect without pairing again.
- Clients with the new `CLIENT_MANAGEMENT` permission can list paired devices with `GET /access/clients` and revoke them with `DELETE /access/clients/{managementId}`. Revocation also closes the device's WebSocket connections and removes its push registrations. If closing the connections or removing the push registrations fails, the node completes the revocation at its next start. If the node cannot save the removal of the device's access, the device has no access while the node runs, but the next start restores the access until the revocation is repeated successfully.
- The node stores only a SHA-256 hash of each client secret. The first start converts stored secrets and deletes the old backups of the API access store.
- WebSocket subscriptions register before building their initial payload, group subscriptions by topic and parameter, and add negotiated compression. This reduces missed updates and duplicate subscriptions.
- The node authenticates every WebSocket upgrade request. An upgrade request that is not a GET request or lacks the `Sec-WebSocket-Key` header receives HTTP 400, and each connection opens at most one WebSocket.
- Mobile push registration and relay encoding are corrected. Notifications filter duplicate and insignificant trade updates, include category and trade ID, and support optional encryption compatible with iOS CryptoKit. Makers receive a notification when their offer is taken.
- A push registration belongs to the client that created it; another client cannot change or delete it. Registrations whose token the push gateway reports as permanently invalid are removed.
- A take that the buyer's node refuses returns HTTP 400. A refusal because the maker is below an alert's minimum version carries `errorCode` `MAKER_VERSION_BELOW_MIN_REQUIRED` and `minRequiredVersion`.
- The API converts a fixed price from Bisq Connect to the exact canonical form before it publishes the offer. When the node cannot forward a REST call that arrives over the WebSocket, the client gets a generic error text.
- User rate limiting is configurable at startup through `application.user.rateLimitEnabled` and remains enabled by default.

Details: [API endpoints](../../api/src/main/java/bisq/api/rest_api/endpoints), [identity DTO](../../api/src/main/java/bisq/api/dto/identity/IdentityDto.java), [closed trades](../../docs/specifications/api/closed-trades.md), [mobile trade notifications](../../bisq-easy/src/main/java/bisq/bisq_easy/BisqEasyMobileTradeNotificationService.java), and [offer-taken notification](../../docs/specifications/trade/bisq-easy/mobile-offer-taken-notification.md).

### Persistence, Reputation, and Oracle Reliability

- Store writes keep the current file until the complete replacement is ready. Replacement uses an atomic move where supported. Failed serialization, writes, or replacement report failure instead of a successful persistence result. A store whose write failed is written again at its next change or at shutdown.
- Instance protection uses a file lock and fails closed when the lock cannot be established. The lock remains held during backup, preventing another process from opening the same data directory mid-backup.
- Unparseable persisted entries are skipped individually and logged without their private contents. When it skips entries of a private or settings store at startup, Bisq keeps a copy of the original file before the next write removes these entries. Startup replay likewise isolates failed entries and cleans up derived state when publications disappear.
- Version-2 witness requests require ownership preimages and reject supplied dates. Persisted requests with unused legacy dates normalize those dates without losing ownership bindings. Signed-witness handling aligns with historical arbitrator-signed Bisq 1 records.
- Oracle nodes read their stored bridge requests at startup, so stored witness bindings stay in effect after a restart. Bridge recovery retains catch-up requests arriving during active recovery. Unsupported subscription RPCs report the required bridge upgrade instead of retrying indefinitely.
- Bonded-role revalidation continues past entries that cannot be replayed, retaining unverifiable registrations for later handling. Data that waits for its bonded role is kept once per publication and dropped when the stores remove it. Multiple valid oracle publications keep a role or account timestamp available until the last valid publication disappears.
- Market-price ingestion rejects non-positive values. Nodes keep oracle market-price data for markets they do not know and apply only known markets; before, such data caused repeated disconnects, for example on Android. Oracle requests bind the authenticated sender to the requested profile, and reputation state clears correctly when source data is removed.
- Webcam QR capture uses bounded frames over the helper process's standard input/output instead of a local TCP listener. Helper failures and shutdown release resources, and stderr goes to a separate rolling log.

The persistence correction removes a window in which the store path was absent and the next startup could treat it as an empty store. Write failure propagation also prevents an oracle from publishing a witness authorization whose ownership binding was not saved.

Details: [store writes](../../docs/specifications/persistence/store-write-results.md), [unreadable store files](../../docs/specifications/persistence/unreadable-store-files.md), [bridge continuity](../../docs/specifications/network/bisq1-bridge-block-continuity.md), and [authorized data](../../docs/specifications/bonded-roles/authorized-data.md).

### MuSig and Integrated Wallet Development

These features remain gated by developer-mode MuSig activation.

- MuSig offer creation and taking use new domain workflows with payment-account compatibility, payment-method limits, fiat/crypto amount conversion, price warnings, and restored draft preferences.
- Maker-side validation checks live offers, identities, amounts, prices, accounts, and signatures before trade creation. Offer claims and state changes are serialized to prevent concurrent takes from exceeding availability.
- Trade setup checks the peer contract and payment-account payload hashes. Dispute handling adds signed mediation results, custom cooperative payouts, no-payout results, arbitration escalation, and arbitrator case views.
- Mediation handles out-of-order messages and synchronizes state changes. Trade and offer details and chat delivery status are improved.
- The integrated wallet adds address balances, a funds table, persisted dashboard selection, send/receive overlays, and a receive-address wizard with saved address notes and QR display.

Details: [MuSig create offer](../../docs/specifications/offer/mu-sig/create-offer.md), [MuSig take offer](../../docs/specifications/offer/mu-sig/take-offer.md), [dispute agents](../../docs/specifications/support/mu-sig-dispute-agents.md), and [activation gate](../../mu-sig/src/main/java/bisq/mu_sig/MuSigService.java).

### Build Workflows and Verification

- CI builds now run unit tests and include a separate desktop composite-build job. Ordinary JavaFX tests run headlessly; robot-driven UI tests require explicit opt-in.
- Composite dependency verification audits resolved artifacts with strict verification, exact reviewed checksum fallbacks, and reusable inventories that invalidate when dependency inputs change. The policy rejects ignored signing keys already present in the keyring.
- Release checks require the exact signing-key marker. The release keys `E222AA02` and `387C8307` are valid until 2027-10-06 and 2029-09-24. Linux ARM64 protobuf tools have explicit verification metadata.
- The build accepts the Tor bundle only when it is signed by a signing subkey that is not revoked, and it fails when the Tor primary key is revoked.
- `cleanAll` and `buildAll` coordinate composite builds in one Gradle invocation. Desktop automation modules, smoke scenarios, a local multi-node runner, and a log anomaly scanner are added. A nightly workflow tests compatibility with the last release, including a network of mixed versions.
- Apache HttpClient changes from `5.6.1` to `5.6.4`, and HttpCore from `5.4.2` to `5.4.3`. GitHub Actions use updated pinned checkout, Java setup, and Gradle setup versions.
- Domain specifications and regression tests are added across storage, signatures, trade validation, chat, Tor, persistence, APIs, and UI behavior.

Details: [dependency verification](../../docs/specifications/development/dependency-verification.md), [Tor bundle verification](../../docs/specifications/development/tor-bundle-verification.md), [desktop test execution](../../docs/specifications/development/desktop-test-execution.md), and [build workflow](../../.github/workflows/build_bisq_module.yml).

## Verification of These Notes

The notes were checked against the tag-to-branch file diff, affected implementations, configurations, and specifications. The update of 2026-10-13 checked each pull request merged after `ddebe75934` against its diff and the code at `26943944fe`, and against the code at `v2.1.13` to decide whether a change is new for users. Fixes of work that `v2.1.13` did not contain are folded into the description of that work. Changes already present at `v2.1.13` are not presented as new solely because they occur in merged commit history. No application build, runtime scenario, or test suite was run while preparing these documentation files; this is not a release certification.
