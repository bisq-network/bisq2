# Storage keys

This specification defines the keys under which the network storage holds data requests. It covers the stores of
authenticated data, mailbox data, and append-only data.

A store holds at most one request per storage key. The key decides:

- which add request a remove or refresh request applies to;
- which requests are duplicates;
- which sequence number a new request of an owner must exceed.

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
key creates its own entry, and the entry of the owner stays.

Reason for a byte concatenation instead of a canonical schema of the request: the preimage has only fixed parts and
one variable part at the end, so no field tags or lengths are needed.

### Data hash

The data hash is `RIPEMD160(SHA256(data.encodeCanonical(STORAGE_KEY)))`, the canonical `STORAGE_KEY` encoding of
the stored data (`AuthenticatedData`, `MailboxData`). The requests and the sequential data
(`AuthenticatedSequentialData`, `MailboxSequentialData`) have no `STORAGE_KEY` encoding. `AppendOnlyData` has no
canonical encoding yet: its data hash is `RIPEMD160(SHA256(appendOnlyData.serializeForHash()))`.

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

The hash serialization of append-only data uses the decoded object, not the kept `Any` bytes (see [`distributed-data-payload-bytes.md`](distributed-data-payload-bytes.md)): fields annotated with
`@ExcludeForHash` and fields which the node does not know are not part of the key. Otherwise anyone could create
several keys for the same data by adding such fields, and the store would hold each variant.

## Compatibility

- Storage keys are local to a node, with one exception: the inventory filter (see below). No other message contains
  them. Nodes of different versions therefore can use different keys.
- Remove and refresh requests of earlier versions carry the V1 data hash and the owner key. A node of this version
  computes the V2 key from them.

## Unresolved questions

- **Inventory filter.** An inventory request contains the storage keys of the requests which the requester already
  has. The responder leaves out the requests whose keys are in the filter. Nodes of earlier versions send V1 keys, and
  nodes of this version store their requests under V2 keys. Between nodes of different versions no key matches, so
  the responder sends all requests up to the size limit of the inventory, and the requester may miss requests beyond
  that limit. Options: keep V1 keys in the filter until nodes of earlier versions are no longer supported, or send both
  keys. A V1 filter does not distinguish owners, so a requester which has a copy of the data from another owner would
  not receive the copy of the owner. The developers decide the option.
- **Encoding of the owner key.** The same public key has several valid encodings (for example a compressed and an
  uncompressed point). For add requests, the owner key must match the signed `pubKeyHash`, so a relay cannot change its
  encoding. For remove requests without a stored entry, signature V1 does not cover the key. A relay can then store
  the remove request under keys of other encodings of the same key. Requiring the encoding which all Bisq 2 nodes
  create removes this.
