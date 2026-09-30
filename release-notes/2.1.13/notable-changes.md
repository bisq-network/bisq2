# Bisq 2.1.13 Notable Changes

## Canonical Signed Integer Encoding

- Canonical encoding now supports protobuf `sint32` and `sint64` fields using ZigZag varints.
- Existing `int32` and `int64` encodings remain unchanged.
- Signed boundary vectors verify canonical bytes and varint lengths.

Related commits: [7334d0941c](https://github.com/bisq-network/bisq2/commit/7334d0941c), [9a2856c69c](https://github.com/bisq-network/bisq2/commit/9a2856c69c), [bf97681395](https://github.com/bisq-network/bisq2/commit/bf97681395).

## Bisq Easy Offer Validation

- Take-offer requests must match an offer authored by its maker or a whole offer in a trade signed by that maker.
- This prevents a taker's own offer from being validated as the maker's offer.

Related commit: [a9562b0b24](https://github.com/bisq-network/bisq2/commit/a9562b0b24).

## Witness and Bridge Reliability

- Version-2 witness requests require ownership proof and reject supplied dates. Persisted version-2 requests with old unused dates are migrated without losing profile or witness bindings.
- Signed-witness ownership behavior now matches the Bisq 1 bridge's historical arbitrator-signed records.
- Bridge catch-up preserves requests that arrive during an active recovery. An unsupported subscription RPC is reported as a bridge version mismatch and is not retried until the bridge is upgraded.
- Bonded-role revalidation proceeds when individual stored records cannot be replayed or converted, while retaining registrations that cannot be verified.

Related commits: [6fe10cb3e1](https://github.com/bisq-network/bisq2/commit/6fe10cb3e1), [524676e93e](https://github.com/bisq-network/bisq2/commit/524676e93e), [b30b68a85b](https://github.com/bisq-network/bisq2/commit/b30b68a85b), [a677c6bbb3](https://github.com/bisq-network/bisq2/commit/a677c6bbb3), [7a140344ac](https://github.com/bisq-network/bisq2/commit/7a140344ac).

## Updater and Runtime Diagnostics

- macOS clients download the DMG matching Intel or Apple Silicon architecture.
- Local version details show both the Tor binary and Tor Browser versions.

Related commits: [829a9b1e18](https://github.com/bisq-network/bisq2/commit/829a9b1e18), [64346f320f](https://github.com/bisq-network/bisq2/commit/64346f320f).

## Build Workflows

- GitHub Actions now use updated pinned versions of checkout, Java setup, and Gradle setup actions.

Related commits: [1d95d33b1b](https://github.com/bisq-network/bisq2/commit/1d95d33b1b), [ed06e5c1c9](https://github.com/bisq-network/bisq2/commit/ed06e5c1c9), [ac2399673b](https://github.com/bisq-network/bisq2/commit/ac2399673b).
