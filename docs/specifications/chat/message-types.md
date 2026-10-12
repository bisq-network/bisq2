# Chat Message Types

## Rule

The type of a chat message decides how a client shows the message, for example as a text bubble,
as a notice that a user left the chat or as a line of the trade log. The sender chooses the type. A
client therefore accepts from the network only the types that clients send in that kind of chat,
and a chat channel holds only messages of these types. A node does not store or relay public
messages of other types either.

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

A public message of another type than `TEXT` fails at parsing (`PublicChatMessage.verify`, rule 2 of
[`../network/validation-at-parsing.md`](../network/validation-at-parsing.md)). A node therefore does
not store, relay or serve it, and a store skips it when it loads. The expired messages indicator,
which the desktop client creates as a public message, passes the rule: its type has no protobuf
value, so no node sends or receives it. The type of a private message is checked only by the chat
of its receiver, as nodes which relay or store a private message see only its encrypted form.

## Channel IDs

The sender also chooses the channel ID of a message. Private chats between two users
(`TwoPartyPrivateChatMessage`) and the public discussion and support chats
(`CommonPublicChatMessage`) migrate the channel IDs of older versions to current channels when they
read the ID (`TwoPartyPrivateChatChannel.Migration`, `SubDomain`). A received message whose channel
ID cannot be migrated is ignored in the same place as a message of an unsupported type
(`ChatChannelService.isValid`, check `ChatMessage.hasValidChannelId`). Reading the channel ID of
such a message does not fail: the message keeps the ID which it carries. Code which handles a
message before the chat checks it reads the channel ID as well, for example the network layer when
it copies its set of received messages into a hash-based set. So the message does not stop the
processing of other messages, for example when a client starts.

A public discussion or support message (`CommonPublicChatMessage`) whose channel ID does not name a
channel of `SubDomain` fails at parsing. The channel IDs of earlier versions still name such
channels, because `SubDomain` resolves and migrates them. A node therefore does not accept, store,
relay or serve a message with another channel ID.

Reasons:

- Unmodified clients of all versions publish public discussion and support messages only in these
  channels, so the rule follows rule 3 of
  [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md).
- Nodes up to version 2.1.13 cannot read the channel ID of such a message: their getter throws, and
  their chat stops at its start when it loads a stored message with such an ID. Before this rule,
  nodes of this version stored, relayed and served such a message, so one message of a modified
  client stopped the start of every node of version 2.1.13 which received it, for the 10 days of
  its lifetime.
- A private message between two users keeps the tolerant reading of its channel ID: nodes which
  relay or store it see only its encrypted form, so they cannot check the ID.

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
later must be supported by receivers in a release before the first release that sends it. The same
holds for a public discussion or support channel: a release must know its `SubDomain` before the
first release publishes messages in it, because nodes without it drop these messages and do not
relay them. A type
value that a client does not know is read as `TEXT`. Channel IDs of older versions stay valid, as
the migration maps them to current channels.

Messages of unsupported types which a client stored before this rule are dropped when the channel
loads. A private channel that such a message opened stays, possibly empty, and so does the contact
entry. Notifications created before this rule are not removed by it.

Unmodified clients of all versions publish public messages only as `TEXT`, so the check at parsing
follows rule 3 of [`../network/validation-at-parsing.md`](../network/validation-at-parsing.md).
Nodes of earlier versions still store and relay public messages of other types. A node of this
version skips such an entry of an inventory response, and drops such a message which it receives
from the network without closing the connection.

## Not covered

Clients without this rule still show received messages of any type. Nodes of version 2.1.13 and
earlier still accept a public message with a channel ID which they cannot read, and relay it to
each other, so such a message can still stop the start of such a node. They receive public messages of
other types only from nodes of earlier versions or from the modified client which creates them.
