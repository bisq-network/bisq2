# Bisq 2.1.12 Notable Changes

Bisq 2.1.12 focuses on owner-bound reputation, bonded-role authorization, and reliable Bisq 1 bridge state recovery.

## Owner-Bound Reputation

- Account-age and signed-witness claims require bridge-authoritative ownership proofs bound to the Bisq 2 profile and protocol domain.
- Published records use a shared nullifier and one-day bucket instead of the raw Bisq 1 witness hash and exact timestamp.
- Legacy and conflicting claims are retained for compatibility but receive no score until replaced by valid version-2 data.

Related commits: [ca844f2059](https://github.com/bisq-network/bisq2/commit/ca844f2059), [46b6b72786](https://github.com/bisq-network/bisq2/commit/46b6b72786).

## Bonded Roles and Reputation

- Bonded-role registrations bind the profile, accepted role proposal, and exact lockup transaction.
- Registrations are persisted before publication, recovered on startup, and revalidated after DAO updates.
- Bonded-reputation unlock status is committed to version-2 oracle signatures; version-1 data contributes zero score.

Related commits: [68c15fb445](https://github.com/bisq-network/bisq2/commit/68c15fb445), [c01a052bd0](https://github.com/bisq-network/bisq2/commit/c01a052bd0).

## Bridge Reliability

- Bisq 1 bridge snapshot/live handoff is contiguous, resumable, gap-aware, overlap-deduplicated, and able to replay buffered blocks after catch-up.
- RPC deadlines prevent stalled bridge calls from exhausting request workers.
- Bonded-role revalidation is fail-safe on bridge unavailability and retries later.

Related commits: [8a3326539c](https://github.com/bisq-network/bisq2/commit/8a3326539c), [84f7a7bc5b](https://github.com/bisq-network/bisq2/commit/84f7a7bc5b), [fcbfba415c](https://github.com/bisq-network/bisq2/commit/fcbfba415c), [fd21896f66](https://github.com/bisq-network/bisq2/commit/fd21896f66).

## Release Preparation

- Transifex CI installation and synchronization were hardened.
- The release version is `2.1.12`; regenerate release artifacts before publishing.
