# Startup replay

## Rule

At startup a service replays data which the node received from other users before the service was initialized:

1. the confidential messages which the node processed before, for example from the mailbox. The private chat, the
   Bisq Easy and MuSig trades, mediation, arbitration, moderation, and in the oracle node the timestamp service and the
   request service of the Bisq 1 bridge (bonded role registrations, account age, signed witness and account timestamp
   requests) replay them;
2. the data of the network store which a service handles: public chat messages, user profiles, MuSig offers, the
   authorized data which `AuthorizedBondedRolesService` applies at its start, processes again after a failure and
   replays to a listener added after its initialization, and in the oracle node the registration requests which it
   recovers and the user profiles which it indexes.

The service processes each replayed item on its own (`StartupReplay.forEach`). When the processing of an item throws a
runtime exception, the node logs the type of the item and the exception and continues with the next item. The start of
the service does not fail because of one item. An `Error`, for example an out of memory error, is not caught.

## Registration of a confidential message listener

A processed message is a confidential message which the node decrypted and checked in this run (rule 4 of
[Confidential messages](confidential-messages.md)). The node keeps the most recent processed messages in memory until
it shuts down (see [Processed confidential messages](processed-confidential-messages.md)).

A confidential message listener registers and receives the replay of the processed messages in one step. The private
chat, the Bisq Easy trade, the Bisq Easy mediator, the mediation request, the moderation, and in the oracle node the
timestamp service and the request service of the Bisq 1 bridge do so. For them, these rules apply in addition to the rule above:

- Exactly once: the listener receives each processed message exactly once. It receives the messages which the node
  processed before the registration in the replay, and each later message as a new message. A message which the node
  processes while the listener registers reaches the listener either in the replay or as a new message. This holds for
  the messages which the node still keeps: a message which the node dropped is not replayed, and it is processed and
  passed again if it is received again (see [Processed confidential messages](processed-confidential-messages.md)).
- Same calls as a new message: the replay passes each message in the same way as a new message, to the listener method
  for the message alone and to the listener method for the message with the public key of its sender. Each of these
  calls is processed on its own, as each replayed item in the rule above.
- Same restrictions as a new message: a listener applies the state which decides whether it accepts a message before it
  registers. The Bisq Easy and MuSig trade services apply the trade restrictions of the stored emergency alerts
  (trading halted, minimum version for trading) before they receive the replay, so they ignore a replayed trade
  message during a halt as they ignore a new one.
- Order: the replay passes the messages one after the other, in the order in which the node processed them. It runs in
  the thread of the registration and ends before the registration returns. A new message can reach the listener at the
  same time in another thread.
- No hash codes: the replay copies the processed messages without computing their hash codes.

## Delivery of new messages

The node passes each new confidential message to each confidential message listener (see
[Confidential messages](confidential-messages.md)) in tasks of its own. A task is a unit of work which the node runs in
a thread pool. There is one task for the message alone, and one task for the message with the public key of its sender.
When a listener fails with an exception, only this task is affected. The node logs the exception of the failed task
with its stack trace. The log entry does not contain the message. The same holds for the other notifications of the
network layer, for example of new connections.

## Reasons

- Item 1: a message can arrive before the service which handles it starts, for example from the mailbox while the node
  starts. When the node decrypted a mailbox message, it removes the message from the network, so the message does not
  arrive again. Without the replay, the service never processes it. The request service of the Bisq 1 bridge lost its
  replay when it was rewritten for the gRPC bridge, so the oracle node did not process the requests from the mailbox.
- The data comes from other users. A runtime exception while one item is processed, for example from a defect or from
  a check which a later version adds, must not stop the start of the node. Before this rule such an item made
  `initialize()` of the service throw, and the application start ended in the FAILED state, without trades, mediation
  or the API service.
- The live delivery already processes each item on its own: `ConfidentialMessageService` calls each listener through an
  executor, and `DataService` calls each listener in a try/catch. The replay follows the same rule.
- Checks of single items stay. For example the chat ignores a message whose channel ID cannot be migrated before it
  reads the ID (see `../chat/message-types.md`). This rule covers what such checks do not cover, for example a reader
  which a later change adds before the check.
- Exactly once: messages arrive before the services start. The node processes the mailbox messages of its first
  inventory responses while it starts, and peers can send messages directly. When the node decrypted a mailbox message,
  it removes the message from the network, and it acknowledges a message which requests an acknowledgement. The sender
  does not send the message again, so a listener which misses a message never receives it. If the node replayed the
  earlier messages first and registered the listener after that, a message which the node processes between the two
  steps would be lost for the listener or reach it twice. With the registration in one step, a listener does not have
  to recognize a second copy of a message.
- Same calls as a new message: some services decide with the key of the sender, for example the moderator with the key
  of the reporter.
- Same restrictions as a new message: the security managers halt trading, or require a minimum version for trading,
  when a defect of the trade protocol is found. A node which starts during such an alert with a trade message in its
  mailbox would otherwise process the message, for example create a trade for a take offer request or run the handler
  which the alert is meant to stop. The trade services registered the observer of the alerts after the listener, so
  during the replay they saw no restriction.
- No hash codes: the hash code of a message from another user runs code of the message class. For example, the hash
  code of a two party chat message reads its channel ID, and reading a channel ID which cannot be migrated threw until
  the reading was made total (see `../chat/message-types.md`). A copy into a hash-based set computes the hash codes of
  all messages before the replay processes the first one. So one such message stopped the replay of all messages and
  the start of the service, before the rule above could isolate the item. A copy without hashing keeps the replay
  independent of the hash codes of the received types.
- Log of a failed task: the task keeps the exception of the listener, and nobody reads it. Without the log entry, a
  message which a service could not process disappears without a trace. The log entry does not contain the message,
  because messages carry private data of other users, for example chat texts and payment account data, and users share
  their log files with support. The message of an exception does not contain received values (rule 7 of
  [Validation at parsing](validation-at-parsing.md)).

## Compatibility

The rule does not change the protocol or the storage. An item whose processing failed is not removed. It fails again at
the next start and is logged again.

The other rules of this specification do not change the protocol or the storage either. They change only how a node
passes what it received to its services.

A listener which registers in one step receives the earlier messages in the order of processing. Before, the hash-based
copy defined no order. A new message can now reach such a listener while it still receives the earlier messages. New
messages already reach the listeners in parallel tasks, so this adds no new kind of concurrency.

## Known limitations

- **Listeners which do not register in one step.** The MuSig trade, mediation and arbitration services replay a
  hash-based copy of the processed messages and register after the replay. For them, a message which the node
  processes between the two steps is lost or arrives twice, and a message whose hash code throws stops their whole
  replay.
  - Why it remains: the work on MuSig is deferred.
  - What would remove it: the registration in one step, as for the other services.
  - Who decides: the developers.

## Not covered

Local data which a service restores at startup, for example its own trades, is not part of this rule. A stored trade
can keep trade messages of its trade peer which its protocol could not process yet. The trade services apply them at
startup as [Queued trade messages](../trade/queued-trade-messages.md) defines.

The service for message delivery states replays no confidential messages. It needs no replay, because the node passes
each confidential message to this service when it decrypts the message (rule 1 of
[Message acknowledgement](message-acknowledgement.md)).
