# Reject by the trade peer

This specification defines in which states the node of a Bisq Easy trader applies a reject of its trade peer. The
terms are those of [Queued trade messages](../queued-trade-messages.md).

## Background

A trader can reject a trade until both of the following hold: it has received the take offer response, as taker, or
sent it, as maker; and payment data was sent or received. The payment data is the payment account data of the seller
and the Bitcoin address of the buyer. After that, the trader can only cancel the trade. After its reject, the node of
the trader sends no further trade message for the trade.

The mailbox of the network does not keep the order of messages, and both traders act at the same time. So the reject
of the peer can arrive after the node has received other messages of the peer, or after its user has sent payment
data.

## Rules

1. A node applies the reject of its trade peer in every state in which the peer can still have sent it, except in the
   case of rule 2:
   - A taker applies it until it has received the payment data of the maker. The maker can no longer reject once it
     has sent its payment data.
   - A maker applies it in every state before the fiat payment. The taker can send its payment data before it
     receives the take offer response, and reject after that.
2. A buyer as maker which has confirmed that it sent the fiat payment keeps a reject of the seller queued.
3. In the other states, the node keeps a reject of its trade peer queued.

## Reasons

- Rule 1: otherwise the node queues a reject which crossed with its own messages. The reject stays queued until the
  trade reaches a final state, and the node waits for a peer which has left the trade. A buyer as maker which received
  the account data of the seller before the reject could even confirm a fiat payment for a rejected trade. Two
  examples:
  - The buyer as taker has the take offer response and sends its Bitcoin address while the seller as maker rejects.
  - The seller as taker sends its account data before it receives the take offer response, and then rejects. The
    buyer as maker receives the take offer request, the account data and the reject in this order.
- Rule 2: the buyer may have paid. The desktop offers the buyer mediation for the open trade, to get the payment
  back, but not for a rejected trade.
- Rule 3: an honest peer cannot have sent a reject in these states. A peer which wants to leave the trade there
  cancels it. The desktop lets the user report a cancel of the peer to the mediator, because a cancel after the
  exchange of payment data started can violate the trade rules.

## Limitations

- The node cannot tell a reject which crossed with its payment data from a reject which a modified client sends after
  it received the payment data. In the states of rule 1 in which the node has already sent its payment data, such a
  client can leave the trade with a reject instead of a cancel. The desktop then shows a reject and, unlike for a
  cancel, offers no report to the mediator.

## Compatibility

- The rules change no message and no persisted data.
- A stored trade which queued a reject of the peer in one of the states of rule 1 applies it when the trade service
  applies the queued events at startup (rule 3 of [Queued trade messages](../queued-trade-messages.md)).
- A peer with an earlier version keeps a reject queued in the states which this specification adds.
