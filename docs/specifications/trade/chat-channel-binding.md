# Trade Chat Channel Binding

## Authoritative data

The signed trade contract is the authority for the offer, the mediator and the peer of a Bisq Easy trade, and the trade is the authority for the own identity under which the trader takes part in it. The trade chat channel keeps its own copies of them. On the maker's node they come from the taker's first chat message, which is unsigned and freely chosen by the taker; the channel's own identity is the local identity to which that message is addressed.

Everything the application presents for a trade or acts upon must be derived from the contract or from local state. The API channel DTO carries the offer and the mediator of the contract, the open trades list reads the offer and the mediator from the trade and its contract, the trade details show the mediator of the contract, and a mediation request goes to the mediator of the contract.

The chat routes messages to the mediator by the channel's copy of the mediator, and it accepts a mediator response only from that copy. The copy equals the contract mediator for every trade that is not in a final state, because a mismatch fails the trade (see below). Only a trade in a final state can still have a channel that does not match the trade and its contract. A mediation request for such a trade is refused when the channel's offer or mediator differs from the contract, so that one mediation is not split between two mediators, and when the channel's own identity and peer are not the two parties of the contract, because the mediator ignores such a request. A refused request sends nothing to the mediator or the peer.

## Channel on the mediator's node

On the mediator's node the verified mediation request is the authority for the trade chat channel. The channel's two traders are the requester and the peer of the request, and its mediator and its own identity are the mediator's identity. Before the request, anybody who knows the trade ID can send the mediator a trade chat message with an offer, and the chat then creates a channel with the sender as the only trader. When the mediator processes a request, or shows its case, it handles an existing channel of the trade as follows:

1. A channel whose own identity is the mediator's identity and which does not have two traders is replaced by the channel of the request. Its messages are dropped: honest traders send the mediator nothing before the request, and the request carries the chat of the trade. Messages of the traders which wait for the channel are added to the new channel.
2. A channel with two traders holds a mediation case and is kept, also when a later request names other parties. The mediator does not check the signatures of the contract, so otherwise a party could replace the channel of a case with a request which carries another contract with the same trade ID.
3. A channel of another identity of the node is kept. The node can trade in this trade under that identity, and a node has only one channel per trade.

Reason: the mediator profile never trades in a trade which it mediates, because the mediator selection excludes both traders. So a channel in which the mediator's identity has one trader was created by a chat message, not by a trade. Without rule 1 such a channel hides the case in the mediator's view, and the mediator's node drops the messages of the other trader (see [private channel membership](../chat/private-channel-membership.md)).

Limitations, which a node removes only when it creates trade channels from verified trade data instead of chat messages (a decision of the developers):

- A channel which a chat message created at another identity of the mediator's node is kept by rule 3. The sender must know that this identity belongs to the mediator's node.
- A message of a trader which reaches the mediator after such a channel was created and before the request is dropped by the peer check.
- A forged contract can name the mediator's profile as mediator of a trade in which this profile trades itself. Then rule 1 replaces the trade channel of that profile, and its trade fails with `CHANNEL_DATA_MISMATCH`. This needs a mediator who trades with the mediator profile.

Compatibility: no message or storage format changes. A channel which a chat message created before this version is replaced when the mediator opens the case. If the mediator's node imported the chat of a request into such a channel before this version, these messages are dropped with it.

## Matching rule

A trade chat channel matches its trade when:

- the channel offer equals the contract offer by full value equality (`Offer.equals` excludes only the version fields, so a copy serialized by another app version still matches);
- the channel mediator and the contract mediator have the same whole network ID, including the addresses, or both are absent (a user profile can change its terms and statement, so profiles are not compared by value; the addresses are compared because the chat sends the messages for the mediator to the addresses of the channel's copy, see rule 4 of [identity by key](../user/identity-by-key.md));
- the channel peer's profile ID equals the ID of the trade peer's network ID (both are the hash of the public key, which also binds the channel peer to the identity which signed the contract);
- the ID of the channel's own identity equals the ID of the trader's identity in the trade (both are the hash of the own public key; on the maker's node the channel's identity comes from the receiver field of the taker's first chat message, which is not bound to the trade).

## Presence and progress

A trade may only progress while its chat channel exists and matches the trade and its contract. The rule is enforced in the trade protocol before the steps that move value or account data:

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
