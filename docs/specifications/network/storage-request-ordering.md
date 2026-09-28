# Ordering of storage requests

These rules govern replacement of requests under the same [storage key](storage-keys.md).
Signature verification follows [storage-request-signatures.md](storage-request-signatures.md).
All incoming requests must pass the applicable signature, ownership, type, and expiry checks before replacement.

## Authenticated add requests

A stored authenticated add request has a trustworthy sequence number with either signature version.
A new add, remove, or refresh request must have a strictly greater sequence number to replace it.
An identical add is a duplicate and does not change the stored entry or notify listeners.

A refresh produces a stored add request with the refreshed sequence number and creation date. Replaying an older
add must not undo that refresh. The same ordering rule applies after removal and republication of the data.

## Authenticated removals during signature migration

Signature V1 of an authenticated removal does not cover its sequence number. A relay can change that number without
invalidating the signature. Signature V2 covers it. These rules apply to an already stored authenticated removal:

- An incoming removal with a verified signature V2 replaces a V1 removal regardless of their sequence numbers.
  Comparing against the V1 number would let a relay prevent the upgrade by choosing `Integer.MAX_VALUE`.
- A stored V2 removal is replaced by another removal only when the incoming removal has a verified signature V2 and
  a strictly greater sequence number. A V1 removal must not discard the stored authenticated ordering information.
- An incoming V1 removal leaves any stored removal unchanged. Repeated removals are reported as already removed;
  only the allowed replacements change the stored entry.
- An add must exceed a stored V2 removal's sequence number. A stored V1 removal's untrusted sequence number does
  not block an otherwise valid add.

These rules govern replacement, not signature verification: an invalid signature V2 cannot upgrade a legacy entry.

The protocol still accepts V1 requests for compatibility. These transition rules do not remove the signature-stripping
limitation described in [storage-request-signatures.md](storage-request-signatures.md#known-limitations).
