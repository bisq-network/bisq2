# Encoding of EC public keys

This specification defines which encoding of an EC public key Bisq 2 accepts where the encoded bytes identify the key.

## Problem

An EC public key is a point on the curve secp256k1. Nodes send it as an X.509 `SubjectPublicKeyInfo` structure.
The key parser accepts several encodings of the same point, for example:

- a compressed point instead of an uncompressed point;
- a hybrid point (prefix byte 06 or 07);
- a DER length in long form, or a BER length of indefinite form;
- a bit string with unused bits;
- explicit curve parameters instead of the name of the curve.

Anyone can create these encodings from a public key without the private key. A signature which is valid for one
encoding is valid for all encodings, because the signature is verified against the point.

This matters wherever the bytes of the key, and not only the point, are used: for example in a hash of the key (owner
hash, profile ID), a storage key, or the preimage of a signature. With several encodings, one key has several
identities, and a third party can choose between them.

## Requirements

1. **Canonical encoding.** The canonical encoding of an EC public key is the DER encoding of the X.509
   `SubjectPublicKeyInfo` with the named curve secp256k1 and an uncompressed point. This is the encoding which
   BouncyCastle creates with `getEncoded()`.
2. **Bisq creates only canonical keys.** Every EC public key which Bisq 2 creates, sends, or stores as encoded bytes
   is in the canonical encoding. This holds for all released versions. All creation paths use the named curve
   secp256k1 and encode the key with `getEncoded()`: generation of a new key pair, derivation of the public key from a
   private key, and loading of a stored key. A key in another encoding therefore comes from a modified client or was
   re-encoded by a third party. New code which creates EC public keys must keep this property.
3. **Received keys.** A node accepts a public key which it receives as encoded bytes only in the canonical encoding.
   It rejects the message which contains another encoding. This applies to these fields:

   | Message | Field | The bytes are used in |
   | --- | --- | --- |
   | `AddAuthenticatedDataRequest` | `ownerPublicKeyBytes` | storage key V2, publisher hash, signature V2 |
   | `RemoveAuthenticatedDataRequest` | `ownerPublicKeyBytes` | storage key V2, publisher hash, signature V2 |
   | `RefreshAuthenticatedDataRequest` | `ownerPublicKeyBytes` | storage key V2, publisher hash, signature V2 |
   | `AddMailboxRequest` | `senderPublicKeyBytes` | publisher hash, signature V2 |
   | `MailboxSequentialData` | `receiversPubKeyBytes` | storage key V2 |
   | `RemoveMailboxRequest` | `receiverPublicKeyBytes` | storage key V2, receiver hash, signature V2 |
   | `AuthorizedData` | `authorizedPublicKeyBytes` | owner hash, list of authorized keys |
   | `ConfidentialData` | `senderPublicKey` | owner hash of mailbox data |
   | `AuthorizeAccountTimestampV2Payload` | `publicKey` | preimage of the account timestamp hash |

4. **Keys which are stored as key objects** (`PubKey`, `ContractSignatureData`) are encoded again with `getEncoded()`
   when they are serialized or hashed. The encoding of the key object must be canonical. A node rejects a message
   whose key object has another encoding.

## Reasons

- Signature V1 of remove requests covers only the data hash, not the key. Without rule 3, a relay can replace the key
  of a remove request with another encoding. The signature stays valid, and the request gets another storage key V2.
  A node which has no entry under that key stores the request as a new entry, so that it can reject a later add
  request with a lower sequence number. So a relay can create many entries from one remove request. Each entry counts
  toward the size limit of the store. Before the cutoff date of signature V1, a relay can also strip signature V2,
  because receivers accept signature V1 alone
  (see [`../network/storage-request-signatures.md`](../network/storage-request-signatures.md)). Therefore signature
  V2 does not prevent this before that date.
- For the other fields, a signature or a hash check already binds the bytes, so only the owner of the key can choose
  another encoding. Rule 3 still applies to them, so that each key has exactly one identity. Then storage key V2 has
  exactly one value for each data hash and owner, as [`../network/storage-keys.md`](../network/storage-keys.md)
  requires.
- Rule 4: parsing and encoding again with `getEncoded()` does not always create the canonical encoding. It does for a
  compressed point or a DER length in long form. It does not for a key with explicit curve parameters, which keeps
  them, or for a key of another curve. So the received bytes are not checked, but the key object is. The size limit
  of a key already rejects explicit curve parameters, but a key of another curve (for example secp256r1) fits into it.
  A key of another curve is a different key, not another encoding of the same key, so only its owner can create it.
  Rule 4 still applies, so that each key which a node accepts is a key which Bisq creates, with exactly one encoding.
- `AuthorizeAccountTimestampV2Payload.publicKey`: the oracle node checks that the account timestamp hash is the hash
  of the salted fingerprint hash and these key bytes. The hash identifies the account timestamp. Other nodes compute
  the hash from the canonical encoding of the account key. The sender signs the payload, so only the sender can choose
  the encoding. Rule 3 applies, so that one account key has exactly one account timestamp hash.
- The canonical check parses the key, decodes the curve point, and encodes the key again. It is not a cheap check. A
  node runs the size check of the key first, so that it never parses oversized input.
- Rule 3 has no compatibility cost for messages which nodes create, because of rule 2.
  Nodes of earlier versions accept other encodings and relay them; nodes of this version reject them.
- A persisted entry with another encoding is dropped when the store is loaded, because the store skips entries which
  fail validation. Only messages which were created with a modified client can have such an entry.

## Not covered

- Public keys of other algorithms (DSA keys of imported Bisq 1 accounts, Tor Ed25519 keys) and keys which Bisq 1
  data contains as Base64 or hex strings.
