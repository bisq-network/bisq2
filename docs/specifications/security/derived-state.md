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
the same network data has the same state. Where a bound also removes state whose source still exists, the section
below states the bound and its effect.

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

## Rate limit of chat authors

A node limits how many public chat messages and reactions of one author it accepts: at most 3 within one second, 30
within one minute, 120 within one hour and 300 within one day. A message which exceeds a limit flags its author. The
node ignores the messages of a flagged author, and the node of a moderator reports a newly flagged author to the
moderators.

1. The node keeps the times of the messages of an author only while one of them is within the last day. An older time
   counts in no limit. When the node processes a message, it removes the authors without such a time, at most once per
   minute.
2. The node keeps the times of at most 10 000 authors. When it needs to keep the times of one more author, it first
   removes the author whose times it used least recently.
3. For each author, the node keeps at most the newest 301 times, which is one more than the highest limit. A limit
   counts the times within its window, and these are always the newest times, so more times do not change a decision.
4. An author stays flagged at most while the node keeps the times of the author. The author loses the flag when the node
   removes the times (rules 1 and 2), or when a later message of the author no longer exceeds a limit.

Rules 1 and 3 do not change a decision. Rule 2 can make the node forget the recent times of an author when more than
10 000 authors send messages within one day. The node then counts this author like a new author, so the limit can only
become less strict for this author. The limits work per author and do not stop an attacker who uses many authors, so
rule 2 does not weaken the protection against such an attacker.

Reason: the node kept the times and the flag of every author until shutdown. A new author needs only a key pair, so an
attacker could grow both without a limit. Honest chat traffic has far fewer than 10 000 authors per day. With the bound
of rule 2 and the 301 times of rule 3, the times take at most about 70 MB.

## Reputation

Five sources derive a reputation score from authorized data which oracle nodes publish: proof of burn, bonded
reputation, account age, signed witness and profile age. A source assigns the data to a user profile by a key which it
derives from the profile ID. The total score of a profile is the sum of the scores of the sources.

1. When the store removes a publication of reputation data, the source removes the data, unless another stored
   publication carries equal data, for example the publication of another oracle node (for the other types of
   authorized data, see rule 18 of [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md)). A publication which the source
   did not accept, for example one of a key without the oracle node role, removes no data.
2. After a removal, the source computes the data and the score of the profile again from the data which is still
   published, as a node which starts with this data would. For example, when the oldest profile age of a profile is
   removed, the next older profile age of another oracle node applies. When the unlock marker of a bond is removed and
   its lockup is still published, the lockup counts again. When no data remains, the source removes the score of the
   profile.
3. Data which waits for its user profile is removed with its last publication. A profile which arrives later does not
   get a score from removed data.
4. The total score of a profile exists while at least one source has a score for the profile. A score can be 0, for
   example the score of a profile age.
5. When the profile store removes a user profile, the sources forget the profile, unless they have data for it. A score
   stays while its data exists, also while the profile store does not hold the profile. A user profile expires after
   15 days, reputation data after 30 days.
6. The node keeps a nickname in its index of nicknames while a stored profile has this nickname. When a newer version
   of a profile has another nickname, the profile no longer counts for the old nickname.

Account age and signed witness already removed their data and scores with their last publication (see
[account-age.md](../reputation/account-age.md)). Rules 3 to 6 apply to them as well.

Reason: the sources kept every user profile which the node ever received, and the data, scores and nicknames whose
source the stores had removed. Anybody can publish a user profile for a proof of work of about 0.1 seconds and can make
it expire within minutes. The nickname is not part of the proof of work of the profile, so the owner can publish
versions of a profile with other nicknames without new work for the profile.

## Compatibility

The rules change no network message and no persisted data. They change only state in memory.

## Not covered

- This specification does not define a general rule for every collection which other users can grow: a bound or an
  expiry, an eviction order which one sender cannot use to push out the entries of others, and a removal path for every
  add path. Such a rule needs a decision of the developers.
- Other collections which other users can grow are not covered here, for example the private chat channels, the
  waiting private chat reactions and the delivery states of messages.
