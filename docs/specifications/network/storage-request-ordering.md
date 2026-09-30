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

## Authenticated removals during signature migration

Signature V1 of an authenticated removal does not cover its sequence number. A relay can change that number without
invalidating the signature. Signature V2 covers it. A node accepts a V1 removal only before the cutoff date of
signature V1, and from that date on the store removes the V1 removals which it holds (see
[storage-request-signatures.md](storage-request-signatures.md#cutoff-date-of-signature-v1)). Until then, these rules
apply to an already stored authenticated removal:

- An incoming removal with a verified signature V2 replaces a V1 removal regardless of their sequence numbers.
  Comparing against the V1 number would let a relay prevent the upgrade by choosing `Integer.MAX_VALUE`.
- A stored V2 removal is replaced by another removal only when the incoming removal has a verified signature V2 and
  a strictly greater sequence number. A V1 removal must not discard the stored authenticated ordering information.
- An incoming V1 removal leaves any stored removal unchanged. Repeated removals are reported as already removed;
  only the allowed replacements change the stored entry.
- An add must exceed a stored V2 removal's sequence number. A stored V1 removal's untrusted sequence number does
  not block an otherwise valid add.

These rules govern replacement, not signature verification: an invalid signature V2 cannot upgrade a legacy entry.
Mailbox removals use the separate terminal rule below.

Before the cutoff date of signature V1, the protocol accepts V1 requests for compatibility. These transition rules do
not remove the signature-stripping limitation described in
[storage-request-signatures.md](storage-request-signatures.md#known-limitations). The cutoff date removes it.

## Sequence number of a new authenticated request

The owner creates a new add, remove, or refresh request with the sequence number of the entry in its own store plus
one. It uses 1 if its store holds no entry for the storage key, or a removal without signature V2.

Reason: a relay can set the sequence number of a V1 removal, for example to `Integer.MAX_VALUE`, and the owner's store
can hold such a removal. If the owner continued from that number, it could not create any request for the data
anymore, or its own signed requests would carry a number which the relay chose. The rules above accept a request with
any sequence number in place of a V1 removal, so nodes of this version accept the request with sequence number 1.

Consequences, until the cutoff date of signature V1:

- A node of an earlier version compares with the sequence number of the V1 removal which it holds. It rejects the new
  add of the owner until that removal expires.
- If the V1 removal replaced an add of the owner in the owner's store, a node which still holds that add rejects each
  new request of the owner whose sequence number is not higher, until that add expires.

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
