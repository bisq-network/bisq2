# State derived from network data

This specification defines how long a node keeps state which it derives from network data: copies and indexes of
stored data, scores, buffers, and state which it keeps per author of received data.

## Problem

Anybody can add network data at a low cost. A new author needs only a key pair, a user profile needs a proof of work of
about 0.1 seconds, and a public chat message or reaction needs one add request. The publisher also chooses the
`created` date of its data. Data with a date near the end of its time to live leaves the stores within about 10
minutes, when the stores prune expired data, and frees its place under the limit of the store.

Version 2.1.13 and earlier versions created derived state for every item, but did not remove it when the source data
went away or when the author became idle. An attacker could therefore grow the memory of every desktop node until
restart, while the stores stayed within their limits.

## Principle

A node keeps derived state only while its source exists:

- state which follows stored data is removed on the event which removes the data: removal by the owner, expiry or
  pruning;
- state which a node keeps per author is removed when the author has no event within the time window for which the
  state is used;
- a buffer which keeps data until a service has started is not filled after the service has started.

Removing state whose source no longer exists does not change what the node shows or decides. A node which restarts with
the same network data has the same state.

## Chat notifications of pruned messages

A node creates a notification for a received chat message. When a store prunes a chat message, the node removes the
notification of that message.

1. At startup, the stores prune their persisted data when they read it. The node may read its notifications after that.
   So the node keeps the notification IDs of messages which the stores prune before the notification service has
   started, and removes these notifications when the service starts.
2. After the notification service has started, the node removes the notification of a pruned message at once. It does
   not keep the ID.

Reason for rule 2: the node kept the ID of every pruned message until shutdown. A publisher can make its messages expire
within minutes, so the IDs grew without a limit. The stores keep at most 1 000 pruned requests for their observers, but
this copy was not bounded.

## Compatibility

The rules change no network message and no persisted data. They change only state in memory.
