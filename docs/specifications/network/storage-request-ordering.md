# Ordering of storage requests

These rules govern replacement of requests under the same [storage key](storage-keys.md).
Signature verification follows [storage-request-signatures.md](storage-request-signatures.md).
All incoming requests must pass the applicable signature, ownership, type, and expiry checks before replacement.
A node relays a received request only if these rules let it change the stored entry (see
[storage-request-relay.md](storage-request-relay.md)).

## Authenticated add requests

A stored authenticated add request has a trustworthy sequence number with either signature version.
A new add, remove, or refresh request must have a strictly greater sequence number to replace it.
An identical add is a duplicate and does not change the stored entry or notify listeners.

A refresh produces a stored add request with the refreshed sequence number and creation date. Replaying an older
add must not undo that refresh. The same ordering rule applies after removal and republication of the data.

A copy of a refresh request which a node has applied has the sequence number of the stored add request. The node
does not apply it again, does not notify listeners, and does not relay it (see
[storage-refresh-requests.md](storage-refresh-requests.md)).

## Replaced add request of a stored removal

A store records for each stored removal whether the removal replaced an add request in this store. For such a removal,
the store records the sequence number of the replaced add request. This record is the replaced add request of the
removal.

Requirements:

1. When a removal replaces a stored add request, the store records the sequence number of that add request with the
   removal. This applies to authenticated data and to mailbox data, and to removals with and without signature V2.
2. When a removal replaces a stored removal, the store keeps the record of the stored removal. If the stored removal
   had no record, the new removal has none either.
3. A removal which the store accepts without entry has no record.
4. The record exists only while the store holds the removal. When an add request replaces the removal, or when the
   store removes the removal, the store removes the record.
5. The store persists the record with the removal, so the record stays after a restart.
6. The record is local. It is not part of the removal, and a node does not send it. A removal which a node of an
   earlier version persisted has no record.

Reasons:

- A removal contains only the hash of the removed data. A store which held the add request had checked the data: its
  owner, its type and, for authorized data, its authorization. After the removal replaced the add request, the store
  holds only the removal. Without the record, the store could not tell a removal of data which it checked from a
  removal for data which it never held.
- The record does not need to name the key which signed the removal. The storage key contains the public key of the
  owner (see [storage-keys.md](storage-keys.md#storage-key-v2)). A removal signed with another key gets another
  storage key, so it never replaces the add request of the owner.
- Requirement 6: the record states what this node checked. A field in the removal would be set by the sender or a
  relay. No signature could cover it, because the store sets it after the owner signed the removal.
- One fact, not a score: the record states that the store checked the data, and it holds the signed sequence number
  of that data. A separate flag would be a second representation of the same fact, and the two could disagree. A score
  would mix this fact with a rule which can change, and a persisted score would then have the wrong value.

## Authenticated removals during signature migration

Signature V1 of an authenticated removal does not cover its sequence number. A relay can change that number without
invalidating the signature. Signature V2 covers it. A node accepts a V1 removal only before the cutoff date of
signature V1, and from that date on the store removes the V1 removals which it holds (see
[storage-request-signatures.md](storage-request-signatures.md#cutoff-date-of-signature-v1)). Until then, the rules of
this section apply.

### Trusted sequence number

A store orders the requests for a storage key by the trusted sequence number of the stored entry. The trusted sequence
number is a sequence number which the node verified with a signature, so a relay cannot choose it:

| Stored entry | Trusted sequence number |
| --- | --- |
| Add request | Its sequence number. Both signature versions cover it. |
| V2 removal | Its sequence number. |
| V1 removal with a [replaced add request](#replaced-add-request-of-a-stored-removal) | The sequence number of the replaced add request. |
| V1 removal without replaced add request | 0 |

Requirements:

1. A request replaces a stored entry only if it raises the trusted sequence number, or if it is the removal of the
   stored add request. The rules below apply this to each case.
2. The trusted sequence number of a V1 removal comes from the record of its replaced add request. So it stays after a
   restart, it is local, and it is 0 for a V1 removal which a node of an earlier version persisted (see
   [Replaced add request of a stored removal](#replaced-add-request-of-a-stored-removal)).

### Replacement rules

- An add request replaces a stored removal only if its sequence number is higher than the trusted sequence number of
  the removal. So a node does not accept again the add request which a V1 removal replaced, and no earlier one. A V1
  removal with the trusted sequence number 0 rejects no add request.
- The sequence number of a stored V1 removal itself is never used. A relay can therefore not reject the add requests
  of the owner by choosing `Integer.MAX_VALUE`.
- A V1 removal replaces a stored add request only if its sequence number is higher than that of the add request. A
  relay can raise this number, so the check protects no data. It rejects an unchanged copy of an earlier removal after
  the owner published the data again.
- An incoming removal with a verified signature V2 replaces a V1 removal only if its sequence number is higher than the
  trusted sequence number of the V1 removal. It is not compared with the sequence number of the V1 removal: a relay
  could prevent the upgrade by choosing `Integer.MAX_VALUE`.
- A stored V2 removal is replaced by another removal only when the incoming removal has a verified signature V2 and
  a strictly greater sequence number. A V1 removal must not discard the stored authenticated ordering information.
- An incoming V1 removal leaves any stored removal unchanged. Repeated removals are reported as already removed;
  only the allowed replacements change the stored entry.

These rules govern replacement, not signature verification: an invalid signature V2 cannot upgrade a legacy entry.
Mailbox removals use the separate terminal rule below.

Reasons:

- Requirement 1: every accepted request raises the trusted sequence number of the entry, or replaces an add request
  with its removal. A set of requests can therefore change an entry only a limited number of times, in any order of
  arrival. A node relays a request only when the request changes the entry (see
  [`storage-request-relay.md`](storage-request-relay.md)), so a broadcast ends.
- The rule for add requests: without it, an add request and its V1 removal replace each other. The removal replaces
  the add request because its sequence number is higher. The add request replaces the removal because the sequence
  number of the removal is not trusted. When copies of both requests are on the way at the same time, each node
  stores and relays them again and again, and the applications get the data added and removed each time. This can
  happen without an attacker, when the owner removes data a few seconds after it published the data. A peer can also
  start it at any time with an add request and a removal which it received earlier. With the rule, the node still does
  not depend on the sequence number of the V1 removal: the number of the replaced add request is signed.
- The rule for a removal with signature V2: a V2 removal whose sequence number is not higher is older than the add
  request which the V1 removal replaced. The owner created it before it published the data again. If it replaced the
  V1 removal, the store would accept that add request again, because the sequence number of the add request is higher
  than that of the older V2 removal. Then the V1 removal would replace the add request again, and so on.
- Requirement 2: the owner of the data continues from the trusted sequence number (see
  [Sequence number of a new authenticated request](#sequence-number-of-a-new-authenticated-request)). If a node lost
  the number at a restart, the owner would start again with 1, and the nodes which hold the number would reject its
  requests.

Limitation: a node can get a V1 removal before the add request, for example when the owner removes data shortly after
it published the data. The node stores the removal with the trusted sequence number 0 and accepts the add request when
it arrives. It holds the data until it gets the removal again, for example with an inventory response. Then the removal
replaces the add request, and the node does not accept that add request again.

- Why it cannot be removed before the cutoff date: the node verified no sequence number for this storage key. If it
  used the sequence number of the V1 removal, a relay could reject the add requests of the owner.
- What removes it: the cutoff date of signature V1. A removal with signature V2 rejects the add request in any order of
  arrival.

Before the cutoff date of signature V1, the protocol accepts V1 requests for compatibility. These transition rules do
not remove the signature-stripping limitation described in
[storage-request-signatures.md](storage-request-signatures.md#known-limitations). The cutoff date removes it.

## Sequence number of a new authenticated request

The owner creates a new add, remove, or refresh request with the trusted sequence number of the entry in its own store
plus one. It uses 1 if its store holds no entry for the storage key.

Reason: a relay can set the sequence number of a V1 removal, for example to `Integer.MAX_VALUE`, and the owner's store
can hold such a removal. If the owner continued from that number, it could not create any request for the data
anymore, or its own signed requests would carry a number which the relay chose. The trusted sequence number of such a
removal is the sequence number of the owner's add request which the removal replaced. A request with the next number
is accepted by the nodes in which the same removal replaced that add request, and by the nodes which still hold that
add request.

Consequences, until the cutoff date of signature V1:

- A node of an earlier version compares with the sequence number of the V1 removal which it holds. It rejects the new
  add of the owner until that removal expires.
- If the owner's store holds a V1 removal with the trusted sequence number 0, the owner continues with 1. This is the
  case for a removal which the owner created with an earlier version, and for a removal which arrived when the store
  held no add request. A node which holds an add request of the owner, or a V1 removal with a trusted sequence number
  above 0, rejects the requests of the owner whose sequence number is not higher, until its entry expires. The owner's
  store verified no sequence number for such a removal, so it has no number from which it could continue.

### The highest sequence number

If the trusted sequence number is `Integer.MAX_VALUE`, no higher sequence number exists. Until the entry expires, no
request of the owner for this storage key can replace it, in the store of the owner and in every store which holds the
entry. The node of the owner then creates the request with `Integer.MAX_VALUE` and logs a warning. Its own store
rejects the request, so the node does not broadcast it.

Reason: the trusted sequence number comes only from numbers which the owner signed, so only a modified client of the
owner or an edited store creates such an entry. The request fails like every other request which the store rejects.
An exception at the creation of the request would reach the caller as an unexpected error instead.

## Mailbox requests

Mailbox add requests have sequence number 1. A mailbox removal has the fixed sequence number `Integer.MAX_VALUE`,
regardless of signature version. This value is derived locally, not supplied by a peer.

While the removal remains stored, no add for that storage key may replace it. This applies whether the removal
arrives before or after the add, and whether the removal has signature V1 only or signature V2. A removal with
signature V1 only, which a node accepted before the cutoff date of signature V1, remains stored until it expires (see
[storage-request-signatures.md](storage-request-signatures.md#cutoff-date-of-signature-v1)).
No mailbox removal can have a higher sequence number.
Different add requests with the same sequence number must not replace each other either.

A later removal replaces a stored removal only in one case: the stored removal has only signature V1, and the incoming
removal has a verified signature V2. All other removals leave the stored removal unchanged; this includes a V1 removal
after a V2 removal and a second V2 removal.

Reason for the upgrade: signature V1 does not cover `created`. A relay can strip signature V2 and move `created`
back, so that the stored removal expires and is pruned early. After pruning, a replayed add which has not expired
would be accepted again. The V2 removal restores the lifetime which the receiver signed. Only the receiver can create
it, because the storage key contains the receiver's public key (see [storage-keys.md](storage-keys.md#storage-key-v2)).
This does not help a node which receives only the modified V1 removal; that remains part of the limitation described
in [storage-request-signatures.md](storage-request-signatures.md#known-limitations).
