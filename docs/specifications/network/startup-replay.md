# Startup replay

## Rule

At startup a service replays data which the node received from other users before the service was initialized:

1. the confidential messages which the node processed before, for example from the mailbox. The private chat, the
   Bisq Easy and MuSig trades, mediation, arbitration, moderation and the timestamp service of the oracle node replay
   them;
2. the data of the network store which a service handles: public chat messages, user profiles, MuSig offers, the
   authorized data which `AuthorizedBondedRolesService` applies at its start, processes again after a failure and
   replays to a listener added after its initialization, and in the oracle node the registration requests which it
   recovers and the user profiles which it indexes.

The service processes each replayed item on its own (`StartupReplay.forEach`). When the processing of an item throws a
runtime exception, the node logs the type of the item and the exception and continues with the next item. The start of
the service does not fail because of one item. An `Error`, for example an out of memory error, is not caught.

## Delivery of new messages

The node passes each new confidential message to each confidential message listener (see
[Confidential messages](confidential-messages.md)) in tasks of its own. A task is a unit of work which the node runs in
a thread pool. There is one task for the message alone, and one task for the message with the public key of its sender.
When a listener fails with an exception, only this task is affected. The node logs the exception of the failed task
with its stack trace. The log entry does not contain the message. The same holds for the other notifications of the
network layer, for example of new connections.

## Reasons

- The data comes from other users. A runtime exception while one item is processed, for example from a defect or from
  a check which a later version adds, must not stop the start of the node. Before this rule such an item made
  `initialize()` of the service throw, and the application start ended in the FAILED state, without trades, mediation
  or the API service.
- The live delivery already processes each item on its own: `ConfidentialMessageService` calls each listener through an
  executor, and `DataService` calls each listener in a try/catch. The replay follows the same rule.
- Checks of single items stay. For example the chat ignores a message whose channel ID cannot be migrated before it
  reads the ID (see `../chat/message-types.md`). This rule covers what such checks do not cover, for example a reader
  which a later change adds before the check.
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

## Not covered

Local data which a service restores at startup, for example its own trades, is not part of this rule.

The service for message delivery states replays no confidential messages. It needs no replay, because the node passes
each confidential message to this service when it decrypts the message (rule 1 of
[Message acknowledgement](message-acknowledgement.md)).
