# States of a rejected or cancelled trade

This specification defines which final state a Bisq Easy trade reaches when one of the traders rejects or cancels it,
and how this state relates to the role which interrupted the trade.

## Background

A trader can reject a trade before the exchange of the payment data, and cancel it after that. The node of each trader
records the role of the trader which interrupted the trade, maker or taker.

## Rules

1. When the user of the node rejects the trade, the trade moves to `REJECTED`. When the node applies the reject of its
   trade peer, the trade moves to `PEER_REJECTED`.
2. When the user of the node cancels the trade, the trade moves to `CANCELLED`. When the node applies the cancel of
   its trade peer, the trade moves to `PEER_CANCELLED`.
3. The state and the recorded role name the same trader.

## Reasons

- Rules 1 and 2: the state is visible outside the trade protocol. The API returns it, and the mobile trade
  notification takes its text from it, for example "Trade with Bob is cancelled by peer." for `PEER_CANCELLED`.
  Earlier versions moved the trade to `PEER_CANCELLED` at the own cancel and to `CANCELLED` at the cancel of the peer.
  So the notification after the own cancel named the peer, and the notification after the cancel of the peer did not.
- Rule 3: the desktop and Bisq Connect take the trader which interrupted the trade from the recorded role. For
  example, they show "Report to mediator" only after a cancel of the peer.

## Compatibility

- The rules change no message and no persisted data. The state is local to the node, so a peer with an earlier version
  is not affected.
- A stored trade keeps its state. For a trade which an earlier version cancelled, the state can name the wrong trader.
  The recorded role names the right trader in all versions, so a reader which needs to know who cancelled the trade
  uses the role.
