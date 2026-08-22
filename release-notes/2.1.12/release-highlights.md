# Bisq 2.1.12 Highlights

## Reputation Privacy and Integrity

- Account-age and signed-witness reputation is bound to proven ownership and the requesting Bisq 2 profile.
- Public reputation records use a nullifier and day bucket rather than raw witness identifiers and exact timestamps.
- Legacy and conflicting claims receive conservative scoring.

## Bonded Roles

- Bonded-role registrations identify the exact accepted proposal and lockup transaction.
- Oracle-authored registrations are persisted, recovered, and revalidated against Bisq 1 DAO state.
- Bonded-reputation unlock status is committed to oracle signatures.

## Bridge Reliability

- Bisq 1 bridge state recovery enforces contiguous blocks, handles gaps and duplicates, and replays buffered blocks after catch-up.
- RPC deadlines and fail-safe retry behavior prevent an unavailable bridge from blocking interactive services.

## Release Status

- Transifex CI supply-chain handling is hardened.
- The checkout remains versioned `2.1.11`; bump version declarations before releasing `2.1.12`.
