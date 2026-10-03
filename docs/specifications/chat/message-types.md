# Chat Message Types

## Rule

The type of a chat message decides how a client shows the message, for example as a text bubble,
as a notice that a user left the chat or as a line of the trade log. The sender chooses the type. A
client therefore accepts from the network only the types that clients send in that kind of chat,
and a chat channel holds only messages of these types.

## Supported types

| Kind of chat | Message class | Supported types |
|---|---|---|
| Public chats and offerbook | `CommonPublicChatMessage`, `BisqEasyOfferbookMessage` | `TEXT` |
| Private chat between two users | `TwoPartyPrivateChatMessage` | `TEXT`, `LEAVE` |
| Bisq Easy trade chat | `BisqEasyOpenTradeMessage` | `TEXT`, `LEAVE`, `TAKE_BISQ_EASY_OFFER`, `PROTOCOL_LOG_MESSAGE` |
| MuSig trade chat | `MuSigOpenTradeMessage` | `TEXT`, `LEAVE`, `PROTOCOL_LOG_MESSAGE` |

Each message class defines its supported types (`ChatMessage.hasSupportedChatMessageType`).

`CHAT_RULES_WARNING` and `EXPIRED_MESSAGES_INDICATOR` are notices which the desktop client creates
for display only. They are never sent, and no channel holds them. A message of one of these types
from the network never comes from an unmodified client.

## Received messages

A received message of an unsupported type is ignored before the chat acts on it: it is not shown,
it creates no notification, it does not open a private channel and it adds no contact. This
applies to private messages, including mailbox messages, and to public messages, including those
loaded from the network data store at startup (`ChatChannelService.isValid`).

## Channel IDs

The sender also chooses the channel ID of a message. Private chats between two users
(`TwoPartyPrivateChatMessage`) and the public discussion and support chats
(`CommonPublicChatMessage`) migrate the channel IDs of older versions to current channels when they
read the ID (`TwoPartyPrivateChatChannel.Migration`, `SubDomain`). A received message whose channel
ID cannot be migrated is ignored in the same place as a message of an unsupported type
(`ChatChannelService.isValid`, check `ChatMessage.hasValidChannelId`). The chat never reads the
channel ID of such a message, also not when the message expires, so the message does not stop the
processing of other messages, for example when a client starts. The message is not rejected at
parsing, because a newer version can add channels whose IDs this version cannot migrate.

## Messages added to a channel

Every message added to a channel is checked as well (`ChatChannel.addChatMessage`). This covers
received messages, the client's own messages, notices which a client adds itself, messages which a
mediator imports from a mediation request, and messages of stored private channels. A stored
private channel therefore loads without messages of unsupported types.

A private channel also holds `PROTOCOL_LOG_MESSAGE` messages of its own identity. The client adds
such notices itself, for example that the peer is banned (`PrivateChatChannel`). A received message
of an own identity is ignored (`PrivateChatChannelService.isValid`).

## Compatibility

The rule does not change the protocol or the storage format. A type that is added to a kind of chat
later must be supported by receivers in a release before the first release that sends it. A type
value that a client does not know is read as `TEXT`. Channel IDs of older versions stay valid, as
the migration maps them to current channels.

Messages of unsupported types which a client stored before this rule are dropped when the channel
loads. A private channel that such a message opened stays, possibly empty, and so does the contact
entry. Notifications created before this rule are not removed by it.

## Not covered

Clients without this rule still show received messages of any type.
