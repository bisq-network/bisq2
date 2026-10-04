# Startup replay

## Rule

At startup a service replays data which the node received from other users before the service was initialized:

1. the confidential messages which the node processed before, for example from the mailbox. The private chat, the
   Bisq Easy and MuSig trades, mediation, arbitration, moderation, the message delivery status and the timestamp service
   of the oracle node replay them;
2. the data of the network store which a service handles: public chat messages, user profiles, MuSig offers, the
   authorized data which `AuthorizedBondedRolesService` applies at its start, processes again after a failure and
   replays to a listener added after its initialization, and in the oracle node the registration requests which it
   recovers and the user profiles which it indexes.

The service processes each replayed item on its own (`StartupReplay.forEach`). When the processing of an item throws a
runtime exception, the node logs the type of the item and the exception and continues with the next item. The start of
the service does not fail because of one item. An `Error`, for example an out of memory error, is not caught.

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

## Compatibility

The rule does not change the protocol or the storage. An item whose processing failed is not removed. It fails again at
the next start and is logged again.

## Not covered

Local data which a service restores at startup, for example its own trades, is not part of this rule.
