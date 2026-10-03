# Relay of storage requests

This specification defines which received storage requests a node sends on to its peers. The replacement rules which
decide whether a store accepts a request are defined in [`storage-request-ordering.md`](storage-request-ordering.md).

## Rules

1. A store accepts a request when it stores the request. For a removal, this includes three cases:
   - the removal replaces the add request of the data;
   - the store holds no entry for the storage key, and it keeps the removal for a later add request. The store
     keeps such a removal only within the limit of rule 12 of [`storage-metadata.md`](storage-metadata.md#size-limit);
   - the removal replaces a stored removal, as the replacement rules allow.
2. A request which leaves the store unchanged is not accepted, also when it is valid. Examples are a request which the
   store holds already, a removal which does not replace the stored removal, and an add request whose sequence number
   is not higher.
3. A node relays a request which it received by broadcast when its store accepted the request. The deprecated field
   `metaData` carries the registered MetaData (see [`storage-metadata.md`](storage-metadata.md#compatibility)).
4. A node does not relay a request of an inventory response.
5. The applications get the removal of data only in the first case of rule 1, and only for data which its owner
   published (see rule 11 of [`storage-ownership.md`](storage-ownership.md)). Storing a removal without data does not
   notify them.

Reasons:

- Rule 1, second case: a removal can arrive before its add request, for example when the owner removes data shortly
  after it published it. Other nodes can hold the add request, so the removal must reach them.
- Rule 1, third case: a removal with signature V2 replaces a stored removal with only signature V1 (see
  [`storage-request-ordering.md`](storage-request-ordering.md)). It restores the sequence number or the creation date
  which the owner signed. If a node which holds the V1 removal did not relay the V2 removal, the first such node would
  stop it, and the nodes behind that node would keep the V1 removal.
- Rule 2: a node which receives a request again does not relay it again. Each node relays a request at most once, so a
  broadcast ends. An unchanged removal must not count as accepted: every relay would send it again to its peers.
- Rule 5: the store notifies the applications of data which it removes. A removal without data changes nothing which
  the applications show.

## Compatibility

Nodes of earlier versions relay a removal only when it removes data. Rules 1 and 3 need no new message and no new
field.

## Related documents and code

- [`storage-request-ordering.md`](storage-request-ordering.md): when a request replaces a stored request.
- `DataStorageResult.isSuccess()`: the store accepted the request (rule 1).
- `DataService.processAddDataRequest` and `DataService.processRemoveDataRequest` (rules 3 and 4).
