# Private channel membership

A private chat channel (two-party chats, Bisq Easy and MuSig open trade chats) has a fixed set of peers: the other
user of a two-party chat, or the traders and dispute agents of a trade, without our own identity.

## Rules

1. A received message or reaction is added to an existing channel only if its sender is a peer of the channel and its
   receiver is our identity in that channel. Otherwise the node ignores it.
2. This applies to the channel found by the channel ID of the message, to the channel which an open trade service
   returns for the trade ID of the message, and to a message which the MuSig trade chat kept as pending until the
   channel of its trade was created. A pending message which these rules reject is no longer pending.
3. A message which names an existing channel never creates another channel, also when rule 1 rejects it.
4. The sender is the sender profile of the message. It is bound to the key which encrypted the message, and the
   author ID of a private message is derived from it.
5. A received message of a Bisq Easy or MuSig trade chat is added to a trade channel only if its trade ID is the trade
   ID of the channel (`BisqEasyOpenTradeChannelService`, `MuSigOpenTradeChannelService`). The channel ID is not compared, because the channel ID of a message can
   differ from the ID of the channel in a dotless i (see "Not covered").
6. A mediation request of a trader carries only those messages of the trade channel whose trade ID is the trade ID of
   the channel, whose channel ID is the channel ID of that trade ID (`BisqEasyOpenTradeChannel.createId`,
   `MuSigOpenTradeChannel.createId`) and whose sender is one of the two traders, because the mediator ignores a request
   with any other message (`BisqEasyMediationRequestService.requestMediation`,
   `MuSigTraderMediationService.requestMediation`).

## Reason

The sender chooses the channel ID and the trade ID of its message. Without rule 1, a user who learned the ID of a
channel, for example a trade ID, could add messages to the conversation of other users, for example to a trade chat.

Without rules 5 and 6, a trade peer could add a message with the trade ID of another trade to the trade chat of the
other trader. That message would end up in each mediation request of the other trader, and the mediator ignores a
request with such a message, so one message would block the mediation of the trade. Rule 6 also covers channels which
already hold such a message, and the messages of the dispute agents, which the channel holds after a dispute agent
responded to a request.

## Compatibility

Unmodified nodes send messages only to channels in which they are a peer, and they take the trade ID and the channel ID
of a trade chat message from the channel, so their messages pass rules 1 and 5. Rule 6 leaves out only messages of the
dispute agents and messages with a channel ID with a dotless i (see "Not covered"), with which the mediator ignores a request
as well.

## Not covered

The channel ID is not derived from the participants when a message is parsed, because channels of users with a Turkish
or Azerbaijani system locale which were created by version 2.1.7 or earlier have IDs with a dotless i.
