# Bisq 2.1.13 Release Notes

Proposed release notes for the Bisq 2.1.13 branch.

## Scope

This note set covers commits after `539cc023496fe0c59c961e32b250442905999fa3` through `bf97681395acaa3fd990ecf85964232096240916`. The range contains 24 commits: 15 non-merge commits and 9 merge commits.

- Generated on: 2026-09-24
- The checkout declares version `2.1.13` in `gradle.properties`.

## Compatibility and Operator Notes

- Canonical encoding supports protobuf `sint32` and `sint64` with ZigZag varints. Existing `int32` and `int64` encodings remain unchanged; use signed schema types where the protocol specifies them.
- Witness authorization keeps version-1 date validation behavior. Version-2 network requests require an ownership preimage and reject requester-supplied dates. When loading persisted version-2 requests, unused legacy dates are normalized to zero so valid ownership bindings remain available.
- An `UNIMPLEMENTED` bridge subscription indicates that the Bisq 1 bridge must be upgraded to provide that RPC. The oracle reports the mismatch and stops retrying that subscription; restart the oracle after upgrading the bridge. Unary bridge RPCs remain available.
- macOS update downloads use architecture-specific `Bisq-x86_64-<version>.dmg` and `Bisq-aarch64-<version>.dmg` names, with detached signatures matching each selected installer filename.

## Notable Changes

### Canonical Signed Integer Encoding

Bisq 2 now includes canonical encoding support and schema types for protobuf signed 32-bit and 64-bit integer fields. The writer uses ZigZag varints, preserving protobuf-compatible bytes for negative and positive values while leaving existing integer encodings unchanged. Golden vectors cover signed boundaries and varint lengths.

Related commits:

- [7334d0941c](https://github.com/bisq-network/bisq2/commit/7334d0941c) - Copy canonical encoding packages to Bisq 2.
- [9a2856c69c](https://github.com/bisq-network/bisq2/commit/9a2856c69c) - Support canonical ZigZag integer fields.
- [bf97681395](https://github.com/bisq-network/bisq2/commit/bf97681395) - Add signed varint boundary vectors.

### Bisq Easy Offer Validation

Take-offer validation now binds the matched offer to its maker. Offer book entries must be authored by the maker, and fallback matching searches only trades signed by that maker. This closes a validation gap where a taker could otherwise present their own offer as the maker's offer.

Related commit: [a9562b0b24](https://github.com/bisq-network/bisq2/commit/a9562b0b24).

### Witness Authorization and Reputation Compatibility

Version-specific witness request validation preserves the intended bindings while handling persisted data safely. Version 2 requires a salted ownership preimage and does not accept requester-supplied dates; older version-2 records with unused dates are normalized during loading instead of causing the request store to fail. Version-1 date behavior remains unchanged, and unsupported versions remain unauthorized. The signed-witness ownership rule is clarified to match the Bisq 1 bridge's historical arbitrator-signed record behavior; ownership continues to be established through the proven witness data, not the leaf's owner field.

Related commits:

- [6fe10cb3e1](https://github.com/bisq-network/bisq2/commit/6fe10cb3e1) - Validate witness requests without losing bindings.
- [524676e93e](https://github.com/bisq-network/bisq2/commit/524676e93e) - Align the signed-witness owner key rule with the Bisq 1 bridge.
- [823b57f505](https://github.com/bisq-network/bisq2/commit/823b57f505) - Reuse witness bucket duration.

### Bridge and Bonded-Role Reliability

Bridge recovery retains concurrent catch-up demand so a new trigger is not lost as an active snapshot completes. Oracle subscription handling reports `UNIMPLEMENTED` as a bridge deployment mismatch and stops retrying that subscription, while ordinary stream failures retain recovery behavior. Bonded-role revalidation continues when individual stored records are malformed or cannot be converted into bridge requests; unverifiable registrations remain stored rather than being treated as verified. Directly constructed bridge messages receive the required outbound validation.

Related commits:

- [b30b68a85b](https://github.com/bisq-network/bisq2/commit/b30b68a85b) - Preserve concurrent bridge catch-up requests.
- [a677c6bbb3](https://github.com/bisq-network/bisq2/commit/a677c6bbb3) - Report incompatible bridge subscriptions.
- [7a140344ac](https://github.com/bisq-network/bisq2/commit/7a140344ac) - Keep bonded-role revalidation progressing.

### Updater and Runtime Diagnostics

macOS update selection chooses the installer matching the running architecture, and release packaging checks the same architecture-specific artifact names. Version details in the application UI and logs show the Tor binary version alongside the Tor Browser version.

Related commits:

- [829a9b1e18](https://github.com/bisq-network/bisq2/commit/829a9b1e18) - Select macOS update installers by architecture.
- [64346f320f](https://github.com/bisq-network/bisq2/commit/64346f320f) - Add Tor binary version next to Tor Browser version.

### Build Workflow Updates

GitHub Actions workflows now use updated pinned versions of `actions/checkout`, `actions/setup-java`, and `gradle/actions/setup-gradle`.

Related commits:

- [1d95d33b1b](https://github.com/bisq-network/bisq2/commit/1d95d33b1b) - Update actions/checkout.
- [ed06e5c1c9](https://github.com/bisq-network/bisq2/commit/ed06e5c1c9) - Update actions/setup-java.
- [ac2399673b](https://github.com/bisq-network/bisq2/commit/ac2399673b) - Update gradle/actions/setup-gradle.

## Tests and Specifications

Regression tests cover canonical encoding and ZigZag boundaries, maker-bound offer matching, versioned witness request validation and persisted-store migration, bridge subscription mismatch behavior, concurrent catch-up requests, and bonded-role revalidation. Specifications were updated for bridge block continuity, account age, signed witnesses, and architecture-specific updater downloads.

## Commit Inventory

| Date | Commit | Summary |
| --- | --- | --- |
| 2026-08-22 | [829a9b1e18](https://github.com/bisq-network/bisq2/commit/829a9b1e18) | Select macOS update installers by architecture |
| 2026-08-22 | [7a140344ac](https://github.com/bisq-network/bisq2/commit/7a140344ac) | Keep bonded-role revalidation progressing |
| 2026-08-22 | [b30b68a85b](https://github.com/bisq-network/bisq2/commit/b30b68a85b) | Preserve concurrent bridge catch-up requests |
| 2026-09-16 | [a677c6bbb3](https://github.com/bisq-network/bisq2/commit/a677c6bbb3) | Report incompatible bridge subscriptions |
| 2026-09-16 | [6fe10cb3e1](https://github.com/bisq-network/bisq2/commit/6fe10cb3e1) | Validate witness requests without losing bindings |
| 2026-08-22 | [823b57f505](https://github.com/bisq-network/bisq2/commit/823b57f505) | Reuse witness bucket duration |
| 2026-09-16 | [524676e93e](https://github.com/bisq-network/bisq2/commit/524676e93e) | Align signed-witness owner key rule with the Bisq 1 bridge |
| 2026-09-18 | [64346f320f](https://github.com/bisq-network/bisq2/commit/64346f320f) | Add Tor binary version next to Tor Browser version |
| 2026-09-20 | [1d95d33b1b](https://github.com/bisq-network/bisq2/commit/1d95d33b1b) | Update actions/checkout |
| 2026-09-20 | [ed06e5c1c9](https://github.com/bisq-network/bisq2/commit/ed06e5c1c9) | Update actions/setup-java |
| 2026-09-20 | [ac2399673b](https://github.com/bisq-network/bisq2/commit/ac2399673b) | Update gradle/actions/setup-gradle |
| 2026-09-20 | [7334d0941c](https://github.com/bisq-network/bisq2/commit/7334d0941c) | Copy canonical encoding packages to Bisq 2 |
| 2026-09-20 | [a9562b0b24](https://github.com/bisq-network/bisq2/commit/a9562b0b24) | Verify take offer against maker's own offers |
| 2026-09-22 | [9a2856c69c](https://github.com/bisq-network/bisq2/commit/9a2856c69c) | Support canonical ZigZag integer fields |
| 2026-09-24 | [bf97681395](https://github.com/bisq-network/bisq2/commit/bf97681395) | Add signed varint boundary vectors |
