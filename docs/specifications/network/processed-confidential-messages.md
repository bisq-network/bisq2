# Processed confidential messages

`ConfidentialMessageService` keeps the confidential messages which it decrypted, together with the public key of their
sender.

## Rules

1. A decrypted message which the node keeps already is a duplicate. The node does not pass it to the listeners again.
2. The node keeps at most 10 000 processed messages, and at most 64 MiB of them, counted as the size of the decrypted
   messages. When a message is added, the node drops the oldest messages until both bounds hold again. It keeps the
   message which it added.
3. Services which register after a message was processed replay the kept messages at their start (see
   [Startup replay](startup-replay.md)).

## Reasons

- Rule 1: mailbox messages are sent in two versions for backward compatibility, so the node receives most of them twice,
  shortly after each other.
- Rule 1 compares messages with `equals` and computes no hash code of a received message, as the startup replay does
  (see [Startup replay](startup-replay.md)).
- Rule 2: anybody who knows the public key of a user can send confidential messages to that user, so without a bound
  the kept messages grow until the node restarts. A count alone does not bound the memory: a message can be up to the
  size limit of network messages (4 MiB), and its proof of work stops growing with its size above 20 000 bytes of cipher
  text (see [Message cost](message-cost.md)).
- Rule 2 drops the oldest messages: the two versions of a mailbox message arrive shortly after each other, and the
  replay at startup usually needs only the messages of the last minutes.

## Limitations

- The size bound counts the decrypted bytes. The objects on the heap are larger, by a factor which depends on the
  message type.
- Rule 1 compares a new message with each kept message, under the lock which also serves the registration of listeners.
  With 10 000 kept messages of the same type which differ only in their last field, one new message causes 10 000 full
  comparisons. Before the bound, the set of processed messages did the same comparisons and copied itself on each add.
- A message which was dropped and is received again is passed to the listeners again. Most services ignore it: the
  private chat by the message ID, the trade state machines by the processed events, the moderator by the stored
  reports, the message delivery status by the acknowledged IDs. A mediator which receives a mediation request again
  sends its response again and adds its system messages to the channel again; a trader which receives a mediator's
  response again adds its system message again. The MuSig trade, mediation and arbitration services and the
  timestamp service of the oracle node were not checked.
- Anybody can cause such a repeat: a mailbox message is stored in the network, so a peer can fetch it, send enough
  messages to drop it from the kept messages, and send it again. Before the bound, the same was possible after each
  restart of the node.
- A message processed before a service started can be dropped before the service replays it, if more than 10 000
  messages or more than 64 MiB arrive in between. The node removes a mailbox message from the network after it
  processed it, so such a message is lost.

## Compatibility

The rules change no message and no persisted data. The processed messages exist only in memory.
