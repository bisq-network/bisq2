# Rejecting a Bisq Easy trade

A trader can reject a Bisq Easy trade before the trade data was exchanged. The trader who rejects sends a reject
message to the trade peer, and the trade ends in the final state `REJECTED`. The peer's trade ends in `PEER_REJECTED`.
After that, the trader can close the trade, which moves it to the trade history. Later in the trade, a trader cancels
the trade instead.

## Rules

1. The maker can reject the trade in these states:
   - `INIT`;
   - the state after the maker sent the take offer response, before the seller sent the payment account data or the
     buyer sent the Bitcoin address;
   - the state after the maker received the payment account data or the Bitcoin address of the taker, before it
     sent the take offer response.
2. The taker can reject the trade in these states:
   - `INIT`;
   - the state after the taker sent the take offer request;
   - the state after the taker received the take offer response, before the taker sent its own trade data;
   - the state after the taker sent its own trade data, before it received the take offer response.
3. Each state in which the desktop shows the reject button is a state from which the protocol of the trader's role
   accepts the rejection.

## Reasons

- `INIT` in rules 1 and 2: a trade normally leaves `INIT` right after the node created it. The maker's protocol
  processes the take offer request, and the taker's protocol sends it. But the trade store drops a write that follows
  the previous write too soon, and writes the latest state only with a later write or at a normal shutdown. If the
  node stops in another way, for example at a crash of the operating system, the store can keep the trade in `INIT`
  while the trade chat channel exists. Nothing moves such a trade out of `INIT`: the maker does not receive the take
  offer request again, and the taker does not take the offer again. Before `INIT` was a source state of the rejection,
  the desktop showed the reject button for such a trade, but the protocol queued the rejection and never applied it.
  The trade stayed in the open trades for good, and the user could neither reject nor close it.
- Rule 3: a rejection which the protocol does not apply only queues the event (rule 6 of
  [Queued trade messages](../queued-trade-messages.md)). The trade log message about the rejection is still sent
  (see [Trade log messages of user actions](../trade-log-of-user-actions.md)), so the peer reads that the trade was
  rejected while both trades stay open.

## Compatibility

The reject message is the same in all states. A node of an earlier version processes it as before: a trade which
waits for the take offer response, or a maker's trade before it sent its trade data, ends in `PEER_REJECTED`. A trade
in a later state queues it. A node which has no trade with the trade ID keeps the message as a pending trade message
(see [Pending trade messages](../pending-trade-messages.md)).
