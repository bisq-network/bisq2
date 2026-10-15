# Storage keys

This specification defines the keys under which the network storage holds data requests. It covers the stores of
authenticated data, mailbox data, and append-only data.

A store holds at most one request per storage key. The key decides:

- which add request a remove or refresh request applies to;
- which requests are duplicates;
- which sequence number a new request of an owner must exceed.

Replacement rules are defined in [Ordering of storage requests](storage-request-ordering.md).

## Storage key V1

Storage key V1 is the key of earlier versions. It is the hash of the stored data:
`RIPEMD160(SHA256(data.serializeForHash()))` (`DigestUtil.hash`).

| Request | Data |
| --- | --- |
| `AddAuthenticatedDataRequest` | the `AuthenticatedData` |
| `AddMailboxRequest` | the `MailboxData` |
| `AddAppendOnlyDataRequest` | the `AppendOnlyData` (the bare message, without the `Any`) |
| `RemoveAuthenticatedDataRequest`, `RefreshAuthenticatedDataRequest`, `RemoveMailboxRequest` | none: the request carries the hash in its field `hash` |

Storage key V1 does not contain an owner. Two owners who publish the same data share one key.

## Storage key V2

Storage key V2 is `RIPEMD160(preimage)`. The preimage is a byte concatenation, not a canonical encoding of the
request:

```
preimage = tag || 0x00 || dataHash || ownerPublicKeyBytes
```

- `tag`: an ASCII domain separation tag per store (table below). It contains no 0x00 byte, so the terminator ends it.
- `dataHash`: the 20-byte data hash (see [Data hash](#data-hash)).
- `ownerPublicKeyBytes`: the encoded public key of the owner of the entry. Append-only data has no owner and leaves
  it out.

| Store | Tag | Owner public key |
| --- | --- | --- |
| Authenticated data (add, remove, refresh) | `bisq.AuthenticatedDataRequest.storageKeyV2` | `ownerPublicKeyBytes` of the request |
| Mailbox data (add, remove) | `bisq.MailboxRequest.storageKeyV2` | the receiver: `receiversPubKeyBytes` of the add request, `receiverPublicKeyBytes` of the remove request |
| Append-only data | `bisq.AddAppendOnlyDataRequest.storageKeyV2` | none |

Requirements:

1. Every preimage has exactly one decomposition. The tag ends at the first 0x00 byte, the data hash has a fixed length
   of 20 bytes, and the public key is the last element. A request with a data hash of another length is invalid.
2. The add, remove, and refresh requests of one entry compute the same key. All requests of one store therefore use
   the same tag, the same data hash, and the same owner key.
3. The preimage already contains a hash of the data (RIPEMD160 of SHA256), so the key applies RIPEMD160 only once.
4. A request has no canonical encoding for the `STORAGE_KEY` purpose. Its storage key is not an encoding of the
   request, and a call with this purpose fails.

Reason for the owner key: entries of different owners with the same data do not share a key. A request signed with
another key cannot find, replace, or block the entry of the owner. For example, a remove request signed with another
key creates its own entry, and the entry of the owner stays. The owner key does not decide which key may publish the
data; [`storage-ownership.md`](storage-ownership.md) defines that rule.

Reason for a byte concatenation instead of a canonical schema of the request: the preimage has only fixed parts and
one variable part at the end, so no field tags or lengths are needed.

### Data hash

The data hash is `RIPEMD160(SHA256(data.encodeCanonical(STORAGE_KEY)))`, the canonical `STORAGE_KEY` encoding of
the stored data (`AuthenticatedData`, `MailboxData`, `AppendOnlyData`). The requests and the sequential data
(`AuthenticatedSequentialData`, `MailboxSequentialData`) have no `STORAGE_KEY` encoding.

For authenticated and mailbox data, the data hash is also the hash which remove and refresh requests carry. Nodes of
earlier versions create these requests with `RIPEMD160(SHA256(data.serializeForHash()))`, so the `STORAGE_KEY`
encoding of `AuthenticatedData` and `MailboxData` must be byte-identical to their `serializeForHash()`. Then the data
hash also equals storage key V1. For `AuthorizedData` this requires that the `STORAGE_KEY` encoding leaves out the
authorization signature, as `serializeForHash()` does when `AuthorizedData` is the root of the preimage. Otherwise the
add request of authorized data would get another key than its remove request, and republishing with a new signature
would create a new entry.

Data of higher-level modules inside `AuthenticatedData` (`DistributedData`) has no canonical encoding yet. Its
`STORAGE_KEY` encoding contains its protobuf hash serialization (see
[`../encoding/canonical-encoding.md`](../encoding/canonical-encoding.md#data-of-other-modules)).

Reason for a 20-byte data hash for all stores, also for append-only data, which has no remove requests: the preimage
of authenticated and mailbox data needs the fixed length for an unambiguous layout, and one rule for all stores is
simpler. A SHA256 data hash would not make the key stronger: the key itself is a 160-bit RIPEMD160 hash.

The `STORAGE_KEY` encoding of append-only data uses the decoded object, not the kept `Any` bytes (see [`distributed-data-payload-bytes.md`](distributed-data-payload-bytes.md)): fields annotated with
`@ExcludeForHash` and fields which the node does not know are not part of the key. Otherwise anyone could create
several keys for the same data by adding such fields, and the store would hold each variant.

## Persisted data

Earlier versions persisted the stores with storage key V1. When a node loads a store, it computes the key of every
persisted request again and stores the request under it. It does not trust the persisted key. If two persisted requests
have the same key, the node keeps the one which the store would keep: the higher sequence number, else the later
creation date. This happens only if the persisted keys were not the keys of their requests.

Otherwise the node keeps every persisted request which has not expired, unless a rule of the store removes it when the
store is loaded (for example rule 8 of [`storage-ownership.md`](storage-ownership.md)). This includes:

- Remove requests. A remove request rejects add requests of its key whose sequence number is not higher. A node which
  dropped remove requests at startup would accept a replayed add request of removed data again.
- Add requests of the same data of different publishers. They have different keys. A selection of one of them, for
  example the one with the highest sequence number, could drop the entry of the owner.

Earlier versions removed at startup all remove requests of the stores of bonded roles, oracle nodes, market prices and
reputation data, and kept only one add request of equal data in these stores. That cleanup removed duplicates of a
defect of 2024, when the storage key contained the authorization signature. The key is computed again at load (see
above), so such duplicates do not exist anymore.

## Compatibility

- Storage keys are local to a node, with one exception: the inventory filter. Its type defines which storage key it
  contains (see [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md)). No other message contains them. Nodes
  of different versions therefore can use different keys.
- Remove and refresh requests of earlier versions carry the V1 data hash and the owner key. A node of this version
  computes the V2 key from them.

## Encoding of the owner key

The same public key has several valid encodings (for example a compressed and an uncompressed point). Storage key V2
contains the bytes of the owner key, so each encoding would give another key. For remove requests, signature V1 does
not cover the owner key, so a relay could store one remove request under the keys of several encodings. Nodes
therefore accept only the canonical encoding of every owner key (see
[`../security/ec-public-key-encoding.md`](../security/ec-public-key-encoding.md)).
