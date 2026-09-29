# Inventory hash set filter

This specification defines how a node tells a peer which storage requests it already has when it requests the
inventory, and which storage key the filter uses. The storage keys are defined in [`storage-keys.md`](storage-keys.md).

## Filter

An inventory request contains a filter. The hash set filter is a list of entries `(key, sequenceNumber)`, one per
request which the requester holds. The sequence number is 0 for append-only data, which has none. The responder sends
every request whose entry `(key, sequenceNumber)`, computed with the same key, is not in the filter. Both the key and
the sequence number must match.

The filter type defines which key the entries contain:

| Filter type | Feature | Key of an entry |
| --- | --- | --- |
| `HASH_SET` | `INVENTORY_HASH_SET` | storage key V1 |
| `HASH_SET_V2` | `INVENTORY_HASH_SET_V2` | storage key V2 |

Both types use the same protobuf message. Only the filter type distinguishes them.

`HASH_SET` and `INVENTORY_HASH_SET` are deprecated since 2.1.14. Nodes keep supporting them only for nodes of earlier
versions: to answer their requests, and to request inventory from them (see [Configuration](#configuration)).

Requirements:

1. The responder computes the entries of its own requests with the key of the received filter type. The filter type
   does not change the storage keys of the responder, and the responses contain the complete requests, never keys.
   The requester computes its own keys from the received requests.
2. The responder selects the key by the type of the received filter, not by the features of the peer. A peer which
   supports both types chooses one per request.
3. A node rejects a filter whose type it does not know, or whose type does not fit the filter message. It never
   interprets it as another type, because the key would be wrong.
4. For each request, a node uses the first type of its preferred list which the peer supports and for which the node
   itself advertises the feature. The order of the preferred list decides, not the order of the peer's features. A
   type which is not in the list is never used. Peers which support none of the listed types are not used for
   inventory requests.

Reason: a filter entry is only meaningful together with the key it contains. Nodes of this version store requests
under storage key V2, nodes of earlier versions under V1. If both used one filter type, no entry would match between
the versions, and the responder would send all requests up to the size limit of the inventory.

## Removals without signature V2

5. In a `HASH_SET_V2` filter, the entry of a removal without signature V2 has sequence number 0, as the entry of
   append-only data. This applies to the removals of authenticated data and of mailbox data. In a `HASH_SET` filter, the
   entry keeps the sequence number of the removal, as nodes of earlier versions compute it.

Reasons:

- A relay of an earlier version removes signature V2 from a removal and keeps its sequence number. With the sequence
  number, the copy and the removal with signature V2 would have the same entry. A responder with the removal with
  signature V2 would then not send it to a requester with the copy. But the removal with signature V2 replaces the copy
  (see [`storage-request-ordering.md`](storage-request-ordering.md)): it restores the sequence number or the creation
  date which the owner signed, and which a relay can change in the copy. With 0, the entries differ, and the responder
  sends it.
- Signature V1 does not cover the sequence number of a removal, so the number identifies nothing. Two copies have equal
  entries also if a relay changed the number of one of them. A copy never replaces a stored removal, so a responder
  does not need to send it to a requester which holds a copy.
- The signed sequence numbers of stored requests are positive, so no other entry with the same key has sequence
  number 0.

Cost: a requester which holds the removal with signature V2 receives the copy from a responder which holds only the
copy, and rejects it. This ends when the responder gets the removal with signature V2 from any peer, and at the latest
when the copy expires or the store removes it at the cutoff date of signature V1 (see
[`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)).

## Repeated requests

A response has a size limit. A response which reached it has the flag `maxSizeReached`: the responder has more
requests which the requester does not have. During the initial requests, the requester then asks the same peer again.
Its next filter also lists the requests which it stored from the response, so the next response contains other
requests.

6. If the stores of the requester accepted none of the requests of a response which reached the size limit, the
   requester does not request inventory from that peer anymore. During the initial requests it asks another peer, and
   periodic requests do not select that peer.

Reason: the filter lists only the requests which the requester stored. If the requester rejects all requests of a full
response, its next filter is the same, and the peer sends the same response again. Without rule 6, the requester would
repeat the request without end, and the requests after that response would never arrive from that peer. This happens
in ordinary cases, for example:

- The requester holds a removal, and the responder missed it and still holds the earlier add request. The requester
  rejects the add request.
- The responder is a node of an earlier version, and the requester is a node of this version which sent the request
  shortly before the cutoff date of signature V1 and gets the response after it. It rejects every request without
  signature V2. From the cutoff date on, rule 8 prevents such requests.

Limitation: the requester does not get the requests after that response from that peer. It gets them from other peers,
or by broadcast. A requester of an earlier version does not follow rule 6.

## Entries which a node cannot parse

7. A requester skips an entry of an inventory response which it cannot parse, and processes the other entries of the
   response. A skipped entry counts as a request which the stores did not accept (rule 6).

Reason: the entries are requests of other nodes, which the responder only stores. A responder of another version can
hold a request which the requester cannot parse, and the responder cannot know which requests the requester can parse.
One such entry must not remove the other entries of the response.

Limitation: a requester of an earlier version drops the whole response when it cannot parse one entry.

### Diagnostics for skipped entries

The requester reports at most three failed entry conversions per response. Nested resolvers must not emit separate
failure diagnostics that bypass this limit. These diagnostics contain only the request type and its serialized size.
They do not include received field values, raw exception messages or exception causes, which can contain those values.

Reason: one response can contain many entries from an unknown type. Logging every failure amplifies the processing
and disk cost of that response. Users also share log files, so received values must not be copied into the diagnostics.

## Configuration

Nodes of this version support both filter types, prefer `HASH_SET_V2` and fall back to `HASH_SET`:

- Features: `INVENTORY_HASH_SET` and `INVENTORY_HASH_SET_V2`. Both are needed: a node uses only types whose feature it
  advertises.
- Preferred filter types: `HASH_SET_V2`, then `HASH_SET`.

| Requester | Responder | Result |
| --- | --- | --- |
| this version | this version | `HASH_SET_V2`, storage key V2 |
| earlier version | this version | `HASH_SET`, the responder derives storage key V1 from its requests |
| this version | earlier version | `HASH_SET`, the requester derives storage key V1 from its requests; from the cutoff date of signature V1 on, no request (rule 8) |
| earlier version | earlier version | unchanged |

8. From the cutoff date of signature V1 on (see
   [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)), a node does not use
   `HASH_SET` for its own requests. So it does not request inventory from peers which support only `HASH_SET`. It still
   answers requests with `HASH_SET`. The node compares the cutoff date with its own time.

Reason for the fallback: as long as the seed nodes and most peers run earlier versions, a node which requests only
`HASH_SET_V2` finds no responder and receives no inventory. The fallback keeps nodes of this version synchronized
during the update of the network. Its cost is described in [Limitations](#limitations).

Reason for rule 8: only nodes of earlier versions need `HASH_SET`. They do not know signature V2, so all signed requests
which they serve lack it, and from the cutoff date on a node of this version rejects all of them. A request to such a
peer then only costs traffic, and a response below the size limit would count as a completed inventory request. A node
which stays on this version keeps this rule after the cutoff date, so it must be part of this version.

## Limitations

- **Requests of this version with `HASH_SET`.** Against a responder of an earlier version, a node of this version
  sends a `HASH_SET` filter and gets the ownership semantics of storage key V1: an entry `(K1, n)` hides the requests
  of all owners with sequence number `n` for the same data. The node can miss the request of one owner while it holds
  the request of another owner. It receives the missing request with a later `HASH_SET_V2` request. The selection of
  peers for inventory requests does not prefer peers which support `HASH_SET_V2`, so such a request can also go to a
  peer of an earlier version while a peer of this version is connected.
  - Why it is accepted now: without the fallback, nodes of this version receive no inventory until the seed nodes and
    enough peers are updated.
  - What removes it: removing `HASH_SET` from the preferred list when enough nodes support `HASH_SET_V2`. Preferring
    peers which support `HASH_SET_V2` in the peer selection reduces it earlier.
  - Who decides: the developers.

- **Several owners with one storage key V1.** Requests of different owners for the same data have the same storage
  key V1 and different storage keys V2. A `HASH_SET` filter cannot tell them apart: an entry `(K1, n)` hides the
  requests of all owners with sequence number `n`. A request of another owner with another sequence number is sent to
  the requester of the earlier version, which may not be able to store it, for example a remove request of another
  owner for data which the requester holds. The requester's filter then does not change, and it can receive the same
  request again with the next response. If such requests fill a response up to the size limit, repeated requests do
  not make progress. This follows from the code paths; it is not reproduced in a test. A requester of this version
  stops such requests ([rule 6](#repeated-requests)); a requester of an earlier version does not.
  - Why it cannot be removed now: the filter of a node of an earlier version contains no owner, and its code cannot be
    changed. The responder cannot know which owner's request the requester holds.
  - What removes it: ending the support of `HASH_SET` when nodes of earlier versions are no longer supported.
  - Who decides: the developers. Until then, nodes of earlier versions get the inventory with the ownership semantics
    of storage key V1, not those of V2.
- **Cost of `HASH_SET` responses.** The stores hold the requests under storage key V2. For a `HASH_SET` filter the
  responder computes storage key V1 of every request for every such response. A cache is possible if measurements show
  a need.
