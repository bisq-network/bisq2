# Private channel membership

A private chat channel (two-party chats, Bisq Easy and MuSig open trade chats) has a fixed set of peers: the other
user of a two-party chat, or the traders and dispute agents of a trade, without our own identity.

## Rules

1. A received message or reaction is added to an existing channel only if its sender is a peer of the channel and its
   receiver is our identity in that channel. Otherwise the node ignores it.
2. This applies to the channel found by the channel ID of the message, and to the channel which an open trade service
   returns for the trade ID of the message.
3. A message which names an existing channel never creates another channel, also when rule 1 rejects it.
4. The sender is the sender profile of the message. It is bound to the key which encrypted the message, and the
   author ID of a private message is derived from it.

## Reason

The sender chooses the channel ID and the trade ID of its message. Without rule 1, a user who learned the ID of a
channel, for example a trade ID, could add messages to the conversation of other users, for example to a trade chat.

## Compatibility

Unmodified nodes send messages only to channels in which they are a peer, so their messages pass.

## Not covered

The channel ID is not derived from the participants when a message is parsed, because channels of users with a Turkish
or Azerbaijani system locale which were created by version 2.1.7 or earlier have IDs with a dotless i.
