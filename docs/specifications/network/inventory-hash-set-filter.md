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

## Requests without signature V2

5. In a `HASH_SET_V2` filter, the entry of a removal without signature V2 has sequence number 0, as the entry of
   append-only data. The entry of an add request without signature V2 has its negated sequence number. A filter which
   lists the entry of an add request with its positive sequence number also lists the add request without signature V2
   with the same key and the negated number. This applies to authenticated data and to mailbox data. In a `HASH_SET`
   filter, every entry keeps the sequence number of its request, as nodes of earlier versions compute it.

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
  number 0, and no entry with a positive number has the number of a copy.
- A relay of an earlier version also removes signature V2 from an add request. The complete request replaces such a
  copy, although both have the same sequence number (see
  [`storage-request-ordering.md`](storage-request-ordering.md#complete-request-of-a-copy-without-signature-v2)). With
  the sequence number, both would have the same entry, and a responder with the complete request would not send it to
  a requester with the copy. Before the cutoff date, the requester would then hold only the copy, and remove it at that
  date. With the negated number, the entries differ, and the responder sends the complete request.
- The add requests of a node of an earlier version have no signature V2 at all. Their versions keep different entries,
  because the number is negated and not replaced by 0. So a requester still gets the newer version of such data.
- A copy never replaces the complete request. A requester which lists the complete request does not need the copy, so
  the responder does not send it to that requester, and a copy which a responder sends anyway counts as listed for
  rule 6.

Cost: a requester which holds the removal with signature V2 receives the copy from a responder which holds only the
copy, and rejects it. This ends when the responder gets the removal with signature V2 from any peer, and at the latest
when the copy expires or the store removes it at the cutoff date of signature V1 (see
[`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)).

## Repeated requests

A response has a size limit. A response which reached it has the flag `maxSizeReached`: the responder has more
requests which the requester does not have. During the initial requests, the requester then asks the same peer again.
Its next filter also lists the requests which it stored from the response, so the next response contains other
requests.

6. After the requester processed a response which reached the size limit, it checks its filter. If the filter which it
   would send to that peer now lists none of the requests of the response, the requester does not request inventory
   from that peer anymore. During the initial requests it asks another peer, and periodic requests do not select that
   peer.

Reason: the filter lists only the requests which the requester holds. If it lists none of the requests of a full
response, the peer sends the same response again. Without rule 6, the requester would repeat the request without end,
and the requests after that response would never arrive from that peer. This happens in ordinary cases, for example:

- The requester holds a removal, and the responder missed it and still holds the earlier add request. The requester
  rejects the add request.
- The responder is a node of an earlier version, and the requester is a node of this version which sent the request
  shortly before the cutoff date of signature V1 and gets the response after it. It rejects every request without
  signature V2. From the cutoff date on, rule 8 prevents such requests.
- The responder is a node of an earlier version which holds authorized data of keys which this version does not
  accept, for example data which another key than an oracle node key authorized (rule 16 of
  [`../bonded-roles/authorized-data.md`](../bonded-roles/authorized-data.md)). The requester rejects it.

Reason for the check of the filter: the requester can hold a request of a response although its stores accepted
nothing from that response. Several initial requests run at the same time with the same filter, so peers with the same
data send the same response. The requester stores the requests of the first response. The stores report the requests
of the later responses as already received. A broadcast can also deliver a request before the response arrives. In
these cases the next filter lists the requests, and the next response of the peer differs. A rule which counts only
the requests which the stores accepted from a response would stop the requests to honest peers, often to the seed
nodes.

Limitation: the requester does not get the requests after that response from that peer. It gets them from other peers,
or by broadcast. A requester of an earlier version does not follow rule 6.

## Entries which a node cannot parse

7. A requester skips an entry of an inventory response which it cannot parse, and processes the other entries of the
   response. A skipped entry is not in the filter of the requester, so for rule 6 it counts as a request which the
   filter does not list. A full response of which the requester can parse no entry therefore stops the requests to
   that peer.

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

## Size of the filter

9. A filter contains at most 100 000 entries. A node rejects a filter with more entries.
10. A requester which holds more requests puts a random selection of 100 000 of its entries into the filter.

Reasons:

- Rule 9 bounds the size of an inventory request. An entry needs 24 to 30 bytes: 22 bytes for the hash field, 0 to 6
  bytes for the sequence number, and 2 bytes for the repeated field. A full filter needs at most 3.0 MB, so the request
  fits the size limit of a message, 4 MiB (see [`message-framing.md`](message-framing.md)). The filter of a node had
  38 280 entries when the limit was set (measured on 2026-10-02), so the limit leaves room for more than twice as
  many.
- Rule 10: before rule 10, a requester with more requests than the limit created a filter which its own validation
  rejected, so it could not request inventory at all.

Compatibility: nodes of earlier versions accept and send filters with fewer than 200 000 entries. Nodes of this
version reject their filters with more than 100 000 entries. They drop such a request and keep the connection (rule 6
of [`validation-at-parsing.md`](validation-at-parsing.md#rules)). If the request also exceeds the size limit of a
message, they close the connection (see [`message-framing.md`](message-framing.md#compatibility)). Such a filter needs
a node with more than 100 000 stored requests.

## Removals which a requester of an earlier version cannot store

11. For a `HASH_SET` filter, the responder does not send a removal of authenticated data if it holds an add request
    with the same storage key V1 and a higher sequence number. For a `HASH_SET_V2` filter, it sends such a removal.

Reasons:

- A requester of an earlier version holds one request per storage key V1 and accepts for it only a higher sequence
  number. If the responder holds an add request `(K1, m)` and a removal `(K1, n)` with `n < m`, the requester either
  holds `(K1, m)`, or it receives it in the same response, because the responder selects add requests before
  removals. In both cases it ends with `(K1, m)` and cannot keep the removal. Its filter therefore never lists the
  removal, and without rule 11 the responder would send it in every response. This assumes that the requester accepts
  the add request. If it rejects it, the removal could replace an older entry `(K1, k)` with `k < n` which the
  requester holds; with rule 11 that entry stays until it expires. For the migration of rule 14 of
  [`storage-ownership.md`](storage-ownership.md), nodes of earlier versions accept the add request of the authorized
  key.
- This is the normal case after the migration of the publisher of authorized data (rule 14 of
  [`storage-ownership.md`](storage-ownership.md)): the responder holds the removal of the network identity with
  sequence number `n + 1` and the entry of the authorized key with `n + 2`, both with storage key V1 `K1`. A requester
  which missed the migration holds `(K1, n)`. It receives `(K1, n + 2)`, which replaces its entry, so it does not need
  the removal either.
- On 2026-10-06, the oracle nodes held 4,760 entries which the migration would replace, 4,732 of them profile age
  timestamps. Their removals have about 1.58 MB, 79 % of the default size limit of a response (2,000 KB). With both
  oracle nodes publishing, they reach about 98 %. Without rule 11, every response to a requester of an earlier
  version would carry them for the time to live of the data (30 days for timestamps). With a few more entries, the
  removals alone would fill every response. A requester of an earlier version then repeats its request without end
  and receives no mailbox data from responders of this version, because it does not follow rule 6.
- A requester of this version stores requests under storage key V2. It needs the removal, because the removal and the
  add request have different storage keys V2.

Rule 11 compares only with add requests. A removal is not suppressed because of another removal with a higher
sequence number: the responder cannot know which one the requester holds.

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
  not make progress. A requester of this version stops such requests ([rule 6](#repeated-requests)); a requester of an
  earlier version does not.
  - The removals of the migration of the publisher of authorized data are the main case. Rule 11 removes it
    ([removals which a requester of an earlier version cannot store](#removals-which-a-requester-of-an-earlier-version-cannot-store)).
    The review 2026-09-29_16-18-18-fable-5.1 found it by reading the code (finding F8, case A).
  - Remaining case: the responder holds a removal of another owner and no add request with a higher sequence number
    for the same storage key V1, while the requester holds an add request which that removal cannot replace.
  - Why it cannot be removed now: the filter of a node of an earlier version contains no owner, and its code cannot be
    changed. In the remaining case the responder cannot know which owner's request the requester holds.
  - What removes it: ending the support of `HASH_SET` when nodes of earlier versions are no longer supported.
  - Who decides: the developers. Until then, nodes of earlier versions get the inventory with the ownership semantics
    of storage key V1, not those of V2.
- **Cost of `HASH_SET` responses.** The stores hold the requests under storage key V2. For a `HASH_SET` filter the
  responder computes storage key V1 of every request for every such response. If the response contains removals, rule
  11 computes storage key V1 of every add request once more. A cache is possible if measurements show a need.
- **Requesters with more requests than the filter limit.** The filter of such a requester does not list all of its
  requests ([rule 10](#size-of-the-filter)). The responder therefore also sends requests which the requester already
  holds, and the requester receives them again. Each filter is a new random sample of the requests which the requester
  holds. The check of [rule 6](#repeated-requests) therefore uses all requests which the requester holds, not only the
  sample, because each of them can be in the next filter. A full response which contains only such requests does not
  stop the requests to that peer: the next filter is another sample, so the next response usually differs.
  - Why it is accepted now: it affects only nodes with more than 100 000 stored requests. Before rule 10, such a node
    could not request inventory at all.
  - What removes it: a filter whose size does not grow with the number of requests. The filter type `MINI_SKETCH` is
    defined for this purpose, but it is not implemented.
  - Who decides: the developers.
