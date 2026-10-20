# Trade log messages of user actions

Some user actions of a Bisq Easy trade send data to the trade peer twice: in the trade protocol message, and in a trade
log message in the trade chat. These actions are: the seller sends the payment account data, the buyer sends the
Bitcoin address, the buyer confirms the fiat payment, the seller confirms the fiat receipt, and the seller confirms the
Bitcoin payment with an optional payment proof.

## Rules

1. The trade service reports whether the protocol applied such an action. It throws `TradeActionNotAppliedException`
   when the trade has no protocol, when the trade is in a final state before the action, or when the trade is
   `FAILED` or `FAILED_AT_PEER` after the action.
2. A caller sends the trade log message of the action only when the call returns normally. The desktop app then shows
   a warning instead. The API returns an error.

## Reason

The protocol does not throw when it does not apply an event: the state machine ignores an event in a final state, and
the protocol turns a failure into the `FAILED` state. Without rule 1 the caller sent the trade log message anyway, so a
peer which had cancelled, rejected or failed the trade still received the payment account data, the Bitcoin address or
the payment proof.

## Not covered

An action which arrives before the state which accepts it is queued by the state machine and is not refused. Its trade
log message is sent. When an action of the same type is queued already, the state machine drops the later action
(rule 6 of [Queued trade messages](queued-trade-messages.md)). The later action is not refused either, so its trade log
message is sent, although the protocol applies only the first action.
