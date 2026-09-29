# Encoding of ECDSA signatures

This specification defines which encodings of an ECDSA signature Bisq 2 creates and accepts.

## Problem

An ECDSA signature is a pair of integers (r, s). If (r, s) is valid for a message and a key, then (r, n - s) is valid
as well, where n is the order of the curve. Anyone can compute the second form from the first, without the private
key. A signature is therefore malleable: a relay can change the signature bytes of a message, and the message stays
valid.

This matters wherever the bytes of a signature, or of a message which contains a signature, identify the message: for
example in a hash, a storage key, a duplicate check, or the preimage of another signature.

## Requirements

1. **Signing.** Every ECDSA signature which a node of this version creates is in low-S form: s <= n / 2. The node
   creates the signature, and if s > n / 2, it replaces s with n - s. Other signature algorithms (for example DSA) are
   not changed.
2. **Supported signing algorithms.** A node creates signatures only with ECDSA over SHA-256 in DER encoding and with
   DSA over SHA-256. The signing function accepts only one exact name for each and rejects every other name. This
   includes aliases which the cryptography provider accepts, such as another letter case or an object identifier.
3. **Encoding.** A signature is the DER encoding of the sequence (r, s). Verifiers reject every other encoding of the
   same pair, for example a longer length encoding, an integer with an unnecessary leading zero byte, or trailing
   bytes.
4. **Verification of existing signature formats.** A signature format which nodes of earlier versions create is
   verified without the low-S rule. It accepts both forms.
5. **Verification of new signature formats.** A signature format which is introduced together with, or after, low-S
   signing accepts only the low-S form. With rules 3 and 5, each signature of such a format has exactly one valid
   encoding.

Formats which follow rule 5:

- Signature V2 of the storage requests ([`../network/storage-request-signatures.md`](../network/storage-request-signatures.md)).

## Reasons

- Rule 1 has no compatibility cost: every verifier accepts both forms.
- Rule 2: whether a signature must be converted depends on the algorithm and its encoding. The provider accepts many
  names for the same algorithm, and several ECDSA variants (other hash functions, deterministic ECDSA, PLAIN and CVC
  encodings). A signing function which accepts arbitrary names would have to classify each of them correctly. If it
  did not, an ECDSA signature could skip rule 1. With a closed set of exact names, no such signature can be created.
- Rule 4: nodes of earlier versions create high-S signatures in about half of all cases. Rejecting them would reject
  about half of their messages.
- Rule 5 is possible only for formats which no released node creates without low-S signing. Once high-S signatures of
  a format exist in the network, requiring low S for it is a breaking change.

## Known limitations

- **Signatures of existing formats stay malleable.** Examples are signature V1 of the storage requests, contract
  signatures, and signatures of authorized data. Each of these must not be used to identify a message.
  - Why it cannot be removed now: see the reason for rule 4.
  - What removes it: a format change after which verifiers require the low-S form, for example when the network
    requires a minimum version whose nodes all sign in low-S form.
  - Who decides: the developers, for each signature format.
  - Signature V1 of the storage requests: from its cutoff date on, nodes accept only requests with signature V2, which
    covers the bytes of signature V1 (see
    [`../network/storage-request-signatures.md`](../network/storage-request-signatures.md#cutoff-date-of-signature-v1)).
