# Sender of a trade message

This specification defines which trade messages a node passes to the protocol of a trade. It applies to Bisq Easy and
to MuSig.

## Terms

- A trade message is a message of the trade protocol which one trader sends to the other trader. It carries the ID of
  the trade and the network ID of its sender. The layer for confidential messages has verified that the sender signed
  the message with the key of this network ID.
- The trade peer is the other trader of a trade.
- The protocol of a trade is the state machine which handles the events and the messages of the trade.

## Rules

1. A node passes a trade message to the protocol of a trade only if the message carries the ID of this trade and the
   sender of the message is the trade peer of this trade.
2. A node drops every other trade message for an existing trade. Such a message does not change the trade, and the
   node does not keep it for later. The node logs the type of the message and no content.
3. The node checks rule 1 when it processes the message. A message which arrived before the trade existed is kept,
   and the node checks it when it processes the message again.
4. The take offer request creates the trade at the maker, so the maker has no trade peer to compare with. The checks
   of the take offer process apply to it instead.

## Reasons

- The state machine treats an exception of a message handler as a failure of the trade. It moves the trade to a final
  failure state and reports the error to the trade peer. The message handlers throw for a message which the trade
  peer did not send. Without rule 1, everybody who knows the ID of a trade could end this trade with one message.
- A trade ID is not a secret. Both traders know it, a mediator learns it with a mediation request, and it appears in
  log files and trade exports.
- Rule 3: the node cannot check a message before the trade exists. So the check must run when the message is
  processed, not only when it arrives.

## Compatibility

- The rules change no message and no persisted data. Unmodified nodes of all versions send trade messages only to
  their trade peer, with their own network ID as sender.
- The message handlers keep their own check of the trade ID and the sender. It still applies to a caller which passes
  a message to the protocol directly.
