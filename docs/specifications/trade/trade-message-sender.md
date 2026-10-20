# Sender of a trade message

This specification defines which trade messages a node passes to the protocol of a trade. It applies to Bisq Easy and
to MuSig.

## Terms

- A trade message is a message of the trade protocol which one trader sends to the other trader. It carries the ID of
  the trade and the network ID of its sender. The layer for confidential messages has verified that the sender signed
  the message with the key of this network ID. The node accepts a trade message only inside a confidential message
  (see [Confidential messages](../network/confidential-messages.md)).
- The trade peer is the other trader of a trade.
- The protocol of a trade is the state machine which handles the events and the messages of the trade.

## Rules

1. A node passes a trade message to the protocol of a trade only if the message carries the ID of this trade and the
   sender of the message is the trade peer of this trade. The node compares the ID of the sender's network ID with the
   ID of the network ID which it stored for the trade peer. The address maps of the two network IDs can differ (see
   [identity by key](../user/identity-by-key.md)).
2. A node drops every other trade message for an existing trade. Such a message does not change the trade, and the
   node does not keep it for later. The node logs the type of the message and no content.
3. The node checks rule 1 when it processes the message. A message whose trade has no protocol yet is kept as a
   pending message, and the node checks it when it passes the message to the protocol of its trade.
   [Pending trade messages](pending-trade-messages.md) defines how the node keeps such messages.
4. The take offer request creates the trade at the maker, so the maker has no trade peer to compare with. The checks
   of the take offer process apply to it instead.
5. A node passes a trade message to the protocol of a trade only if the receiver of the message is the node's own
   network ID of this trade. The node compares the ID of the receiver's network ID with the ID of the network ID which
   it stored for itself as a trader of the trade. Rules 2 and 3 apply to a message which breaks this rule.

## Reasons

- The state machine treats an exception of a message handler as a failure of the trade. It moves the trade to a final
  failure state and reports the error to the trade peer. The message handlers throw for a message which the trade
  peer did not send. Without rule 1, everybody who knows the ID of a trade could end this trade with one message.
- A trade ID is not a secret. Both traders know it, a mediator learns it with a mediation request, and it appears in
  log files and trade exports.
- Rule 3: the node cannot check a message before the trade exists. So the check must run when the message is
  processed, not only when it arrives.
- Rule 5: a node can host several identities, and the trade peer knows the network IDs of other identities from their
  offers and profiles. The layer for confidential messages binds the receiver of a message to the key which decrypted
  it, but it does not know the trade. Without rule 5, the peer could send a message of the trade to another identity
  of the node, and the reaction of the trade, for example its progress or an error report, would tell the peer that
  both identities are hosted on one node.
- Rule 1 compares keys because the layer for confidential messages binds the sender to its key, not to its addresses.
  The network ID which a node stored for its trade peer comes from the offer or from the take offer request. The peer
  can have added a transport since then, and its messages then carry its key with another address map.

## Compatibility

- The rules change no message and no persisted data. Unmodified nodes of all versions send trade messages only to
  their trade peer, to the network ID which they stored for it, with their own network ID as sender. A node sends under the network ID which its trade peer stored
  for it (rule 3 of [identity by key](../user/identity-by-key.md)), because nodes of version 2.1.13 compare the whole
  network ID.
- The message handlers keep their own check of the trade ID and the sender. It still applies to a caller which passes
  a message to the protocol directly.
