# Trade Chat Channel Binding

## Authoritative data

The signed trade contract is the authority for the offer, the mediator and the peer of a Bisq Easy trade, and the trade is the authority for the own identity under which the trader takes part in it. The trade chat channel keeps its own copies of them. On the maker's node they come from the taker's first chat message, which is unsigned and freely chosen by the taker; the channel's own identity is the local identity to which that message is addressed.

Everything the application presents for a trade or acts upon must be derived from the contract or from local state. The API channel DTO carries the offer and the mediator of the contract, the open trades list reads the offer and the mediator from the trade and its contract, the trade details show the mediator of the contract, and a mediation request goes to the mediator of the contract.

The chat routes messages to the mediator by the channel's copy of the mediator, and it accepts a mediator response only from that copy. The copy equals the contract mediator for every trade that is not in a final state, because a mismatch fails the trade (see below). Only a trade in a final state can still have a channel whose offer or mediator differs from the contract, and a mediation request for such a trade is refused, so that one mediation is not split between two mediators.

## Matching rule

A trade chat channel matches its trade when:

- the channel offer equals the contract offer by full value equality (`Offer.equals` excludes only the version fields, so a copy serialized by another app version still matches);
- the channel mediator and the contract mediator have the same network ID, or both are absent (a user profile can change its nick name, terms and statement, so profiles are not compared by value);
- the channel peer's profile ID equals the ID of the trade peer's network ID (both are the hash of the public key, which also binds the channel peer to the identity which signed the contract);
- the ID of the channel's own identity equals the ID of the trader's identity in the trade (both are the hash of the own public key; on the maker's node the channel's identity comes from the receiver field of the taker's first chat message, which is not bound to the trade).

## Presence and progress

A trade may only progress while its chat channel exists and matches the contract. The rule is enforced in the trade protocol before the steps that move value or account data:

- seller sends payment account data;
- buyer sends the Bitcoin address;
- buyer confirms fiat sent;
- seller confirms fiat receipt;
- seller confirms Bitcoin sent.

Rejecting and cancelling are not gated, so a trade whose channel has not arrived yet can still be rejected or cancelled. Taking an offer is not gated as the taker's channel is created after the trade. Confirming the Bitcoin transaction is local bookkeeping and not gated.

## Failure reaction

A missing or mismatching channel at a gated step fails the trade with `TradeProtocolFailure.CHANNEL_DATA_MISMATCH`. The trade moves to `FAILED` and the peer is notified and moves to `FAILED_AT_PEER`. A failed trade ignores every later event. Older clients map the failure reason to `UNKNOWN`.

Every client offers the gated actions only when the trade chat channel exists: the desktop shows them only for a selected trade chat channel, the REST API answers with 404 (not found) when the channel is missing, before it performs any of them, and the mobile app in node mode requires the channel in each of the gated actions and lists a trade only when both the trade and its channel exist. An honest client therefore does not reach a gated step without the channel, and failing the trade on a missing channel is intended.

The trade service fails a mismatching pair as soon as both the trade and the channel exist, without a user action:

- at startup, for all stored pairs, which also covers channels written by earlier versions;
- when a channel appears for a stored trade;
- when a trade's protocol is created while its channel already exists.

Trades in a final state are not touched. A missing channel outside a gated step is not a failure: the channel may not have arrived yet, or the user may have left it after the trade.
