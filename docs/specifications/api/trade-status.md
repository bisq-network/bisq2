# Read-only trade status observation (experimental)

This proposal adds a minimal GET endpoint for clients that lost a response
or a WebSocket connection: `GET /api/v1/trades/status?offerId=<id>`.

The response is a list containing `tradeId`, `offerId`, `tradeState`
and `lifecycle` (OPEN/CLOSED), with no payment details or key material.
Results are sorted by trade ID. A currently closed trade can temporarily
appear in both stores, so the closed record wins for duplicate trade IDs.

It requires the existing TRADES permission and the existing API session.
**That permission currently also authorizes state-mutating trade routes.**
This patch does not implement fine-grained read-only grants or change pairing.

## Recovery semantics

- This endpoint never creates, accepts, closes or replays a trade.
- Open and closed trade stores can change during iteration; the result is
  observational, not a linearizable or durable commit point.
- An empty list does **not** prove that a prior take-offer request failed.
  It must not be used to authorize a blind retry after a timeout.
- Multiple trades can share an offer ID; this endpoint does not establish
  which request, if any, created a listed trade.
- This feature does not repair the acknowledged-before-durable-apply issue
  tracked in bisq2#5126.
- A production agent would still need a separately reviewed per-request
  idempotency contract and narrowly scoped credentials.

## Companion adapter

A read-only client and an observation-only recovery journal are implemented
in patrickdugan/Bisq-Agent-Network on branch
`experiment/bisq2-trade-state-reconciliation`.

## Verification

Run Bisq 2's API tests and check permission behavior and API compatibility
on a fork before submitting this feature for upstream review.
