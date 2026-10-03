# Pending trade messages

This specification defines how a node keeps trade messages which arrive before the trade exists. It applies to Bisq
Easy and to MuSig. The terms are those of [Sender of a trade message](trade-message-sender.md).

## Background

A node creates a trade when it takes an offer, or when it receives the take offer request of a taker. Trade messages
travel through the mailbox of the network, and the mailbox does not keep the order of messages. So a trade message can
arrive before the take offer request which creates its trade. The node keeps such a message as a pending message and
processes it again when it processes the next message of a trade.

## Rules

1. A node keeps a trade message whose trade does not exist as a pending message. It processes the pending messages
   again when it processes a trade message of an existing trade. A pending message which the node then passes to the
   protocol of its trade is no longer pending. A pending message of an existing trade whose sender is not the trade
   peer is dropped, as [Sender of a trade message](trade-message-sender.md) defines.
2. A node keeps at most 100 pending messages. When a message arrives while the node keeps 100 pending messages, the
   node drops the oldest pending message and keeps the new one. A message which is already pending does not change
   the number or the order of the pending messages.
3. A log entry about a pending message names the type of the message and the trade ID. It does not contain the
   content of the message. The trade ID is sanitized for the log, because its sender controls it.
   The same applies when the state machine of a trade handles a message: its log entries and exception messages
   name only the type of the event, also when the trade is in a final state or the handler fails.

4. The chat of Bisq Easy open trades keeps a chat message without offer, whose trade channel does not exist yet, as a
   pending chat message. Rules 2 and 3 apply to these pending chat messages as well, with their own bound of 100.

## Reasons

- Rule 2: anybody can send trade messages with made-up trade IDs to a node. Such a message never gets a trade, so
  without a bound it stays in memory until the node restarts, and the memory of the node grows without limit. A node
  has only a few trades whose messages can arrive before the trade exists, so 100 pending messages leave room for all
  of them.
- Rule 2 drops the oldest message because a message of a legitimate trade gets its trade shortly after it arrives. An
  old pending message is most likely one which never gets a trade.
- Rule 3: trade messages carry private data of the traders: the payment account data of the seller, the Bitcoin
  address of the buyer, a transaction ID, or a payment proof. Users share their log files with support. A log file
  must not reveal the trade data of a trade peer.

## Limitations

- A sender who floods a node with trade messages for made-up trade IDs can push a pending message of a legitimate
  trade out of the pending messages before the trade exists. The node does not receive this message again. The bound
  limits the memory of the node; it does not protect the pending message of a legitimate trade against such a flood.
  Without the bound the flood would instead grow the memory of the node without limit.

## Compatibility

- The rules change no message and no persisted data. The pending messages exist only in memory.
