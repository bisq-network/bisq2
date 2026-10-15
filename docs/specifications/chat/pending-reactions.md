# Pending reactions of private chats

This specification defines how a node keeps a reaction to a private chat message which arrives before the message or
before its channel. It applies to two-party chats and to the chats of Bisq Easy and MuSig open trades.

## Background

Private chat messages and their reactions travel through the mailbox of the network, and the mailbox does not keep the
order of messages. So a reaction can arrive before the message to which it reacts, or before the first message of its
channel. The node keeps such a reaction as a pending reaction and processes it again when it adds a received message
to a channel.

## Rules

1. A node keeps a received reaction as a pending reaction if it does not have the channel or the message of the
   reaction. When it adds a received message to a channel, it processes all pending reactions again. A reaction which
   then finds its message is applied and is no longer pending. A reaction whose message is still missing stays
   pending. A reaction which rule 7 of [Private channel membership](private-channel-membership.md) rejects is ignored
   when it arrives and is never pending.
2. A pending reaction is applied only if its sender is a peer of the channel and its receiver is our identity in the
   channel, as [Private channel membership](private-channel-membership.md) defines. The node checks this when it
   processes the reaction, also after it was pending. A pending reaction which this check rejects is no longer
   pending.
3. A node keeps at most 100 pending reactions. When a reaction arrives while the node keeps 100 pending reactions, the
   node drops the oldest pending reaction and keeps the new one. A reaction which is already pending does not change
   the number or the order of the pending reactions.
4. A pending reaction which is older than the time to live of its type is dropped when the node processes the pending
   reactions.
5. When a node adds a reaction to the pending reactions, it tries once more to apply it. So a message which another
   thread added at the same time is not missed.
6. Processing the pending reactions works on a copy of them. A reaction which is processed and stays pending, and a
   reaction which arrives during the processing, are kept. The processing does not fail when it keeps reactions
   pending.

## Reasons

- Rule 3: anybody who knows the network ID of a user can send reactions for made-up channels or messages to that user.
  Such a reaction never finds its message, so without a bound it stays in memory for the time to live of its type
  (up to 15 days), and the memory of the node grows without limit. A legitimate reaction finds its message shortly
  after it arrives, so 100 pending reactions leave room for all of them.
- Rule 3 drops the oldest reaction because a legitimate reaction finds its message shortly after it arrives. An old
  pending reaction is most likely one which never finds its message.
- Rule 6: earlier versions removed reactions from the set of pending reactions while they iterated over it. With two or
  more pending reactions, the iteration failed after the first reaction. The node then failed to process every later
  private message after it had added the message: it did not apply the other pending reactions, and the open trade
  chats did not remove the message from their pending chat messages. Two reactions for made-up channels were enough to
  cause this until they expired. At startup, the same failure stopped the replay of the messages which the node had
  received before its chat services started.

## Limitations

- A sender who floods a node with reactions for made-up channels can push a legitimate pending reaction out before its
  message arrives. The node does not receive this reaction again. The bound limits the memory of the node; it does not
  protect a legitimate pending reaction against such a flood.

## Compatibility

- The rules change no message and no persisted data. The pending reactions exist only in memory.
