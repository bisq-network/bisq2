# Trade status API (experimental)

`GET /api/v1/trades/status?offerId=<id>` returns the status of this node's Bisq Easy trades for
one offer. A client that lost a response or a WebSocket connection uses it to observe whether a
trade for that offer exists locally and in which state.

## Response

The response is a list of entries, each with `tradeId`, `offerId`, `tradeState` and `store`.
`store` is `OPEN` or `CLOSED` and names the local trade store the entry was read from. It is not
the trade's `lifecycleState` (ACTIVE, HISTORICAL, FAILED).

Entries contain no payment details, profile keys or chat content, so that a client which only
needs to reconcile state does not receive private trade data.

Entries are sorted by trade ID. A trade being moved to the closed store can appear in both stores
for a short time. For a duplicate trade ID the closed entry wins, because a trade only moves from
open to closed.

A missing or malformed `offerId` returns 400.

## Access

The endpoint uses the same access control as the other REST routes. With the security setting
`supportSessionHandling` enabled it requires an authenticated API session, and with
`authorizationRequired` enabled it requires the TRADES permission. Both are enabled in the API app
and desktop configurations. A node with both disabled serves the endpoint without either check.

The TRADES permission also authorizes the state-changing trade routes; this endpoint does not add a
narrower read-only grant.

Nodes that support the endpoint list the `trade-status` key in their API capabilities, so a client
can detect support without probing for a 404.

## Recovery semantics

- The endpoint never creates, accepts, closes or replays a trade.
- The open and closed trade stores can change while they are read. The result is an observation,
  not an atomic snapshot or a durable commit point.
- An empty list does not prove that an earlier take-offer request failed, and must not be used to
  authorize retrying it after a timeout: the request can be acknowledged before it is durably
  applied (bisq2#5126).
- Several trades can share an offer ID. The endpoint does not establish which request, if any,
  created a listed trade.
- Safe retries need a per-request idempotency contract, which this endpoint does not provide.
