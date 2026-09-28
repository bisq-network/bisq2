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

## Configuration

Nodes of this version support both filter types, prefer `HASH_SET_V2` and fall back to `HASH_SET`:

- Features: `INVENTORY_HASH_SET` and `INVENTORY_HASH_SET_V2`. Both are needed: a node uses only types whose feature it
  advertises.
- Preferred filter types: `HASH_SET_V2`, then `HASH_SET`.

| Requester | Responder | Result |
| --- | --- | --- |
| this version | this version | `HASH_SET_V2`, storage key V2 |
| earlier version | this version | `HASH_SET`, the responder derives storage key V1 from its requests |
| this version | earlier version | `HASH_SET`, the requester derives storage key V1 from its requests |
| earlier version | earlier version | unchanged |

Reason for the fallback: as long as the seed nodes and most peers run earlier versions, a node which requests only
`HASH_SET_V2` finds no responder and receives no inventory. The fallback keeps nodes of this version synchronized
during the update of the network. Its cost is described in [Limitations](#limitations).

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
  not make progress. This follows from the code paths; it is not reproduced in a test.
  - Why it cannot be removed now: the filter of a node of an earlier version contains no owner, and its code cannot be
    changed. The responder cannot know which owner's request the requester holds.
  - What removes it: ending the support of `HASH_SET` when nodes of earlier versions are no longer supported.
  - Who decides: the developers. Until then, nodes of earlier versions get the inventory with the ownership semantics
    of storage key V1, not those of V2.
- **Cost of `HASH_SET` responses.** The stores hold the requests under storage key V2. For a `HASH_SET` filter the
  responder computes storage key V1 of every request for every such response. A cache is possible if measurements show
  a need.
