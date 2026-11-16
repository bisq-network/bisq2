# Pending MuSig setups

This specification defines how many setups of a MuSig trade a maker runs at the same time for one offer and one taker,
and how the maker answers a take offer request with the ID of a trade which it holds. It applies to the maker of a
MuSig trade.

## Terms

- The setup of a MuSig trade starts with the take offer request of the taker (`SetupTradeMessage_A`).
- A setup of the maker is pending while its trade is in the state `INIT` or
  `MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES`. In the second state the maker has answered the request and waits
  for the next setup message of the taker. The setup is no longer pending when the trade reaches a later state or when
  the trade fails. A trade fails by a failure of the maker and by a failure report of the taker.
- A held trade is a trade which the node has stored or for which it has a protocol.

## Rules

1. For one offer and one taker the maker runs at most one pending setup. While a setup is pending, the maker rejects a
   take offer request of the same taker for the same offer which has another trade ID. The failure is
   `SETUP_ALREADY_PENDING`. The report carries the trade ID of the rejected request. The request creates no trade.
2. Rule 1 counts only trades in which the node is the maker. A pending setup does not affect a request of another
   taker, a request for another offer, or a trade in which the node is the taker. When the setup is no longer pending,
   the taker can take the offer again.
3. The maker drops a take offer request which is a message of the trade peer for a held trade (see
   [Sender of a trade message](trade-message-sender.md)): the request has the ID of the trade, the peer of the trade
   sent it, and it is addressed to the identity of the node in this trade. The maker does not verify such a request
   and sends no report. This applies to a second delivery of the request, to its replay at startup (see
   [Startup replay](../network/startup-replay.md)) and to a copy with changed content.
4. The maker verifies a take offer request with the ID of a held trade from any other sender, or to another identity
   of the node, like a request with an unknown trade ID, and reports a rejection to the sender.
5. The maker applies rule 1, creates the trade, registers its protocol and passes the request to the protocol in one
   step. It handles two requests which arrive at the same time one after the other, and in this step it drops a
   request with the ID of a held trade without a report, so no request creates a second trade with an ID. The
   creation of a trade by the taker is such a step as well. The shutdown of the trade service waits for a running
   step, and after that the maker creates no trade from a request.
6. Before the maker reports a rejection, it checks rule 3 again, in one step with rule 5.
7. If the request cannot be passed to the protocol, the maker removes the new trade and its protocol and writes the
   store without the trade. If this write fails, the state stays unsaved and the store writes it again (see
   [Store write results](../persistence/store-write-results.md)). The maker sends no report.

## Reasons

- Rule 1: the trade ID contains the take offer date, and the taker chooses this date (rule 1 of
  [Trade ID](trade-id.md)). So the check for a held trade ID does not limit how many setups one taker runs for one
  offer at the same time. Each setup costs the maker a stored trade, a protocol and calls of the MuSig daemon, and the
  user of the maker does not see the trade before it has a trade chat channel.
- Rule 2: a taker can take the same offer several times, so the limit is not one trade for an offer and a taker. It
  holds only as long as the earlier setup is pending.
- Rule 3: the maker reports a rejection with the trade ID of the request, and the node of the taker handles the report
  as a message of its trade peer and fails its trade. A copy of an accepted request can fail the verification, for
  example after the market price moved or after the maker removed the offer. Its report would fail the accepted trade.
- Rule 4: the node of the peer accepts a report only as a message of its trade peer. A report about a request which
  the peer did not send goes to another node. A report about a request which the peer sent to another identity of the
  maker comes from that identity, which is not the trade peer. So these reports cannot fail the trade. If the maker
  dropped every request with the ID of a held trade, a sender would get a report for an unknown trade ID and no report
  for the ID of a held trade. The trade ID is derived from the offer ID, the ID of the taker and the take offer date.
  So a user who knows an offer and a taker could learn whether and when this taker took this offer, and a peer could
  learn that another identity belongs to the node of its trade.
- Rules 5 and 6: without one step, two requests of one taker with different trade IDs could both pass rule 1, two
  deliveries of one request could both create its trade, and a copy which fails the verification could be reported
  while the original request is accepted.
- Rule 7: a trade which stays in `INIT` is a pending setup which never ends (see the first limitation).

## Compatibility

- `SETUP_ALREADY_PENDING` is the value 8 of `TradeProtocolFailure`. A node which does not know the value reads
  `UNKNOWN` and shows the text of the report as an unexpected failure.
- The persisted format does not change. A taker stores the failure of a report with its trade, so a store can contain
  the new value. A node of an earlier version reads it as `UNKNOWN`.
- MuSig is not released, so no released node sends or receives the new value.

## Limitations

- **A pending setup has no time limit.** A trade of the maker which stays in one of the two states blocks its offer
  for its taker: the taker gets `SETUP_ALREADY_PENDING` for every new request for this offer.
  - The trade stays in `INIT` when the trade service shuts down, or the node stops, after the maker stored the trade
    and before its protocol handled the request. After the restart nothing passes the request to the protocol again,
    and the maker drops a new delivery by rule 3. The stored trade is also in `INIT` when the node stops after the
    protocol handled the request and before the store was written again: the rate limit of the trade store can drop
    the write after the request (see [Store write results](../persistence/store-write-results.md)). In this case the
    maker has already answered the taker.
  - The trade stays in `MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES` when the taker does not send its next setup
    message.
  - The user of the maker cannot end such a setup. A trade in `INIT` without a trade chat channel is not in the list
    of the open trades, because the list shows a trade only together with its channel. For a trade in the list, the
    desktop offers the closing only after the trade failed or was completed.
  - The maker can publish the offer again. The new offer has another ID, so the taker can take it.
  - What would remove it: a time limit for the two states which the maker measures, and at startup the removal or the
    failure of a maker trade which is still in `INIT`.
  - Who decides: the developers.
- **The rule limits the setups at the same time, not their number.** A taker can end its pending setup with a failure
  report, which fails the trade of the maker in any state, and then take the offer again. Each of these trades stays
  a stored, failed trade of the maker.
  - What would remove it: a limit of the take offer requests of one taker for one offer in a time span.
  - Who decides: the developers.
- **Rejected requests and closed trades are not remembered.** The MuSig trade store keeps no IDs of rejected requests
  and of closed trades. When a request which the maker rejected, by rule 1 or by the verification, is delivered again
  and passes then, the maker creates the trade. The same holds for the request of a trade which the user closed, for
  example at the replay when the user turns MuSig off and on again. The trade of the taker with this ID has failed
  or is closed, so the new trade of the maker waits for a setup message which does not come (first limitation).
  - What would remove it: a stored record of the IDs of rejected requests and of closed trades.
  - Who decides: the developers.
- **A report can follow an accepted request.** The maker sends the report after the check of rule 6. When a copy of a
  request fails the verification while the original request is not accepted yet, and the maker accepts the original
  between the check and the sending, the report fails the accepted trade at the taker. This needs two deliveries of
  one request which the maker handles at the same time and which differ in the result of the verification.
  - What would remove it: the sending of the report inside the step, or a stored record of the accepted trade IDs
    which the sending checks.
  - Who decides: the developers.
- **After rule 7 the taker gets no answer, and the write is not ordered against other writes of the trade store.**
  The trade of the taker stays in its first state. A write which another trade started before the removal can reach
  the disk after the write of rule 7. The stored state then contains the removed trade until the next write of the
  store, and a node which stops before that has the trade in `INIT` after the restart (first limitation). The same
  holds after a failed write of rule 7 until the store is written again, at its next write or at shutdown.
  - What would remove it: a report with a failure which tells the taker to take the offer again, and writes of the
    trade store which always contain the newest state.
  - Who decides: the developers.
