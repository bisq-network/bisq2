# Bisq 2.1.12 Release Notes

Proposed release notes for the Bisq 2 branch `bind-bonded-roles-and-witness-reputation-to-proven-owners`.

## Scope

This note set covers the range from tag `v2.1.11` (`505e4f48a46afd3bf87918aa4869acbceb965f03`) through `fd21896f66`. It contains 10 reachable commits, 9 non-merge commits, 1 merge commit, and 82 changed files.

- Generated on: 2026-08-19
- Bisq version is set to `2.1.12` in `gradle.properties`, dependency versions, and the desktop launcher metadata.

## Compatibility and Operator Notes

- Version-2 account-age and signed-witness ownership proofs are bound to the authenticated Bisq 2 profile, protocol domain, owner key, and witness data.
- Version-1 reputation records remain parseable for network compatibility but are assigned zero score or treated conservatively until replacement version-2 data is available.
- Version-2 bonded-role registrations bind the profile, role proposal, and exact lockup transaction. New admission is gated by `bondedRoleRegistrationEnabled`; legacy data remains readable.
- Bonded-role revalidation removes a registration only after an explicit per-registration invalid result. Bridge timeouts, transport failures, or unverifiable bulk responses retain the registration and schedule a later retry.
- Bridge block-continuity changes require the matching Bisq 1 bridge protocol additions before deployment. Mixed deployments must follow the documented rollout order.

## Notable Changes

### Owner-Bound Reputation with Privacy Protection

Account-age and signed-witness reputation claims now require Bisq 1 bridge-authoritative ownership proofs. Proofs are bound to the requesting profile, domain, salted account input, owner key, and witness hash. The published record uses a domain-separated nullifier and a one-day UTC bucket rather than the raw Bisq 1 hash and exact millisecond date, limiting cross-network correlation.

Authorized data signatures include the version, nullifier, and bucket. Cross-source conflicts, legacy claims, and unverifiable claims are scored conservatively, and removed claims are cleared even when the profile is absent.

Related commits:

- [ca844f2059](https://github.com/bisq-network/bisq2/commit/ca844f2059) - Bind witness reputation to proven owners privately.
- [46b6b72786](https://github.com/bisq-network/bisq2/commit/46b6b72786) - Include authorized-data version in equality and hash identity.

### Bonded-Reputation Unlock Integrity

Authorized bonded-reputation version 2 commits both the version and unlock status to the oracle signature. Version-1 records remain parseable but contribute zero score, preventing relays from changing unlock markers to suppress or preserve reputation.

Related commit: [c01a052bd0](https://github.com/bisq-network/bisq2/commit/c01a052bd0).

### Bonded-Role Registration Binding

Bonded-role version 2 registrations identify the accepted role proposal and exact lockup transaction, validate proposal-derived terms and bond ownership through the Bisq 1 bridge, persist complete requests before publication, recover oracle-authored registrations, and batch-revalidate them on startup and DAO-block updates.

Revalidation is serialized and fail-safe: only explicit invalid results remove roles, while unavailable or unverifiable bridge responses preserve state for retry. The registration protocol, persistence, cancellation rules, UI model, bridge messages, and deployment invariants are documented.

Related commit: [68c15fb445](https://github.com/bisq-network/bisq2/commit/68c15fb445).

### Reliable Bisq 1 Bridge Recovery

Bridge snapshots and live streams now use an acknowledged, resumable contiguous cursor. The service subscribes before requesting the snapshot, detects gaps and duplicates, coalesces catch-up requests, recovers errored or normally completed streams, and replays buffered blocks in order after catch-up. Snapshot/live overlap is deduplicated by transaction ID, and revalidation callbacks occur only after the contiguous cursor advances.

Interactive bridge RPCs receive fresh 30-second deadlines; historical bulk requests receive 10-minute deadlines. Timeouts fail closed for issuance while retaining bonded roles for later revalidation.

Related commits:

- [8a3326539c](https://github.com/bisq-network/bisq2/commit/8a3326539c) - Bound bridge RPC wait times.
- [84f7a7bc5b](https://github.com/bisq-network/bisq2/commit/84f7a7bc5b) - Recover contiguous Bisq 1 block state.
- [fcbfba415c](https://github.com/bisq-network/bisq2/commit/fcbfba415c) - Process only contiguous bridge blocks.
- [fd21896f66](https://github.com/bisq-network/bisq2/commit/fd21896f66) - Replay buffered blocks after catch-up.

### CI Supply-Chain Hardening

Transifex CLI installation and synchronization workflows now use a hardened, verified installation path, and a security policy document was added.

Related commit: [cd849511d7](https://github.com/bisq-network/bisq2/commit/cd849511d7).

## Tests and Specifications

Regression coverage was added for ownership-proof protocol vectors, legacy containment and removal, bonded-reputation signatures, bonded-role registration and revalidation, bridge message serialization, RPC deadlines, block continuity, snapshot/live overlap, and buffered-block replay. Specifications were added or updated for reputation ownership, bonded-role registration, and Bisq 1 bridge continuity.

## Release Preparation

Before publishing, regenerate release artifacts and checksums, run the relevant module tests and release checks, and verify the cross-repository Bisq 1 bridge compatibility commit.

## Commit Inventory

| Date | Commit | Type | Summary | Author |
| --- | --- | --- | --- | --- |
| 2026-05-21 | [cd849511d7](https://github.com/bisq-network/bisq2/commit/cd849511d7e1b034ad7110d7ffa9ba8d3dafd88a) | Commit | Harden Transifex CI supply chain | Takahiro Nagasawa |
| 2026-05-29 | [3e0f0f8d69](https://github.com/bisq-network/bisq2/commit/3e0f0f8d69e2449a717d8a4bb404a9fa7dd9b295) | Merge | Merge pull request #4777 from hiciefte/codex/transifex-cli-hardening-v2.1.11 | HenrikJannsen |
| 2026-08-19 | [68c15fb445](https://github.com/bisq-network/bisq2/commit/68c15fb445ce40dea6a2503ce609415d9b132145) | Commit | Bind bonded-role registrations to verified DAO bonds | HenrikJannsen |
| 2026-08-19 | [c01a052bd0](https://github.com/bisq-network/bisq2/commit/c01a052bd0e33b13713f2d9552a59c79429fe5fa) | Commit | Bind bonded-reputation unlock status to oracle signatures | HenrikJannsen |
| 2026-08-19 | [ca844f2059](https://github.com/bisq-network/bisq2/commit/ca844f2059d0a9ff5a708bcbff5d53e7c68be9d0) | Commit | Bind witness reputation to proven owners privately | HenrikJannsen |
| 2026-08-19 | [8a3326539c](https://github.com/bisq-network/bisq2/commit/8a3326539cb7828b43c9d40599cf55c0236e5b6d) | Commit | Bound bridge RPC wait times | HenrikJannsen |
| 2026-08-19 | [84f7a7bc5b](https://github.com/bisq-network/bisq2/commit/84f7a7bc5ba2be468023d3da65a576fcc37fd1ae) | Commit | Recover contiguous Bisq 1 block state | HenrikJannsen |
| 2026-08-19 | [46b6b72786](https://github.com/bisq-network/bisq2/commit/46b6b7278661c387eb50fdfcbecadec5a392d729) | Commit | Bind witness data equality to version | HenrikJannsen |
| 2026-08-19 | [fcbfba415c](https://github.com/bisq-network/bisq2/commit/fcbfba415cf32e6e9723244d2c04efce4703a4f6) | Commit | Process only contiguous bridge blocks | HenrikJannsen |
| 2026-08-19 | [fd21896f66](https://github.com/bisq-network/bisq2/commit/fd21896f665f0b770eb00841275442f9376b622f) | Commit | Replay buffered blocks after catch-up | HenrikJannsen |
