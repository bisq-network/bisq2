# Queued trade messages

This specification defines how a node keeps a trade message which the protocol of an existing trade cannot process
yet. It applies to Bisq Easy and to MuSig. The terms are those of [Sender of a trade message](trade-message-sender.md).

## Terms

- An event is an input of the protocol of a trade. It is either a trade message of the trade peer or a local event.
  A local event comes from the node itself, for example an action of the user, a confirmation of a Bitcoin
  transaction, or a timeout.
- The event queue of a trade holds the events which the protocol did not process yet: the protocol has a transition
  for the type of the event, but not from the current state of the trade.
- A queued trade message is a trade message in the event queue of a trade.

## Background

The mailbox of the network does not keep the order of messages (see
[Pending trade messages](pending-trade-messages.md)). So a trade message can arrive while the trade is in a state from
which its protocol has no transition for the message. The protocol then keeps the message in the event queue. After
each change of the state, it applies the queued events again to the new state. When the trade reaches a final state,
the protocol clears the event queue.

Example: the seller as maker has sent its payment account data and waits for the Bitcoin address of the buyer. The
buyer receives the account data, pays, and sends the confirmation that it sent the fiat payment. If this confirmation
arrives before the Bitcoin address, the protocol of the seller queues it. When the Bitcoin address arrives, the
protocol processes the address and then the queued confirmation.

[Pending trade messages](pending-trade-messages.md) are different: they arrive before their trade exists, and the
trade service keeps them. Queued trade messages belong to a trade which exists, and its protocol keeps them.

## Rules

1. The node persists the queued trade messages of a trade together with the trade. When the node loads the trade, the
   event queue of the trade contains these messages again.
2. When the node loads a trade, it skips a queued trade message which it cannot parse. It keeps the trade and the
   other queued trade messages of the trade (rule 4 of [Validation at parsing](../network/validation-at-parsing.md)).
3. At startup, the trade service applies the queued events of each stored trade once, after it knows the trade
   restrictions of the stored alerts. The protocol processes a queued event which has a transition from the current
   state of the trade, as it does after a change of the state. The other events stay in the event queue.
4. During a halt of trading, or when the version of the node is below the minimum version for trading, the trade
   service does not apply the queued events at startup. They stay in the event queue, and the protocol applies them
   after the next change of the state.
5. When the queued events of one trade fail at startup, the node logs the failure and continues with the next trade.
   The start of the trade service does not fail, as for the items of [Startup replay](../network/startup-replay.md).

## Reasons

- Rule 1: when the node decrypted a mailbox message, it removes the message from the network, and it acknowledges a
  message which requests an acknowledgement. The node keeps the processed messages only in memory (see
  [Startup replay](../network/startup-replay.md) and
  [Processed confidential messages](../network/processed-confidential-messages.md)). The sender does not send the
  message again. Before rule 1, a restart of the node lost the queued trade messages, and the trade waited without end
  for a message which it had received. In the example above, the seller never learned that the buyer had sent the fiat
  payment.
- Rule 2: a later version can add an integrity rule which a persisted message breaks (rule 3 of
  [Validation at parsing](../network/validation-at-parsing.md)). If such a message made the trade fail at parsing, the
  trade store would skip the whole trade, and the next write of the store would remove the trade from the disk.
- Rule 3: when the protocol has finished an event, no queued event has a transition from the current state, because
  the protocol applies the queued events after each change of the state. A stored trade can still have such an event,
  in two cases. First, the node writes the store in another thread than the protocol. A write can contain the new
  state of a trade and still the queued event which the protocol processes next. If the node stops before the next
  write, the store keeps this combination. Second, a later version can add a transition from the stored state. In both
  cases, the event would otherwise wait for a change of the state which only the event itself causes.
- Rule 4: the queued trade messages come from the trade peer. The security managers halt trading, or require a
  minimum version for trading, to stop the handlers of the protocol. The node ignores a new trade message during such
  a restriction (see "Same restrictions as a new message" in [Startup replay](../network/startup-replay.md)), so it
  does not process a queued one either.
- Rule 5: one stored trade must not stop the start of the node or the other trades.

## Compatibility

- The queued trade messages are a new field of the persisted trade: `queuedTradeMessages`, number 32 of the message
  `Trade` in `trade.proto`. The number is higher than every number which the message `Trade` used in the last
  release.
- A store of an earlier version has no queued trade messages. This version loads its trades with an empty event queue,
  as before.
- An earlier version which loads a store of this version ignores the field. It loses the queued trade messages, as it
  did before.
- The rules change no network message.

## Known limitations

- **Local events.** The node does not persist the local events of the event queue. A restart loses them, as before.
  - Why it remains: local events have no persisted form. The change is limited to the trade messages, because the node
    cannot receive them again.
  - What would remove it: a persisted form for the local events of the protocols.
  - Who decides: the developers.
- **Processed event types.** The protocol records the type of each event which changed the state of the trade, and it
  does not queue an event of a recorded type. It keeps the recorded types only in memory. So after a restart, it
  queues a copy of a message which it processed before the restart, for example the second copy of a mailbox message,
  and the node persists the copy with the trade. The copy stays in the event queue until the trade reaches a final
  state.
  - Why it remains: the change persists only the queued trade messages, which the trade needs to continue.
  - What would remove it: persisting the processed event types together with the trade.
  - Who decides: the developers.
- **Redaction of trade data.** The Bisq Easy trade service redacts the payment account data of a trade some days after
  the trade, as the setting for the redaction of trade data defines. The redaction does not cover the queued trade
  messages. A queued trade message can contain the payment account data of the trade peer. It stays on the disk until
  the trade reaches a final state, and a trade which never reaches a final state keeps it.
  - Why it remains: the change is limited to the persistence of the event queue.
  - What would remove it: the redaction also removes the queued trade messages of a trade whose date for the
    redaction has passed.
  - Who decides: the developers.
- **Restriction at startup.** A queued event which has a transition from the stored state waits during a restriction
  of trading at startup (rule 4). When the restriction ends, the node does not apply the queued events. It applies
  them at the next change of the state or at the next start without a restriction.
  - Why it remains: the change applies the queued events only at startup and after a change of the state, as the
    protocol did before.
  - What would remove it: the trade services apply the queued events again when a restriction of trading ends.
  - Who decides: the developers.

## Not covered

How often the trade services write their store. They write it at most once per second and at a regular shutdown. If
the node stops without a regular shutdown, it can lose a queued trade message which arrived shortly before, as it can
lose every other recent change of a trade.
