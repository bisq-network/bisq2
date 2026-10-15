# Refresh requests

This specification defines how the owner of authenticated data extends the lifetime of the data with a
`RefreshAuthenticatedDataRequest`, and how nodes apply and distribute this request. Mailbox data and append-only data
have no refresh request.

## Purpose

A stored add request expires when the time since its creation date is longer than the time to live of its type (see
[`storage-metadata.md`](storage-metadata.md)). The owner can publish the data again with a new add request. This sends
the complete data to all nodes.

A refresh request has the same effect without the data. It gives the stored add request a new sequence number and a
new creation date. It is used for data which its owner renews often. Example: a user profile is refreshed while its
user is active. The creation date of its entry then shows when the user was last active.

## Terms

- **Refresh request**: a `RefreshAuthenticatedDataRequest`. It contains the hash of the data, the public key of the
  owner, the new sequence number, the new creation date, the version, the class name of the data, and both signatures.
  It does not contain the data.
- **Refreshed add request**: the add request which a node stores after it applied a refresh request. It consists of
  the data of the stored add request and the values of the refresh request.

## Rules

### Creation

1. The owner creates the add request which the refresh produces: the data, the next sequence number and the current
   time as creation date. The owner signs this add request with both signatures and copies the signatures, the public
   key, the sequence number, the creation date and the version into the refresh request (see
   [`storage-request-signatures.md`](storage-request-signatures.md#refresh-requests)).
2. The node of the owner applies the refresh request to its own store. It broadcasts the request only if its store
   applied it.

### Admission

A node applies a refresh request only if all of the following rules hold.

3. The node holds an add request under the storage key of the refresh request (see
   [`storage-keys.md`](storage-keys.md)). A refresh request never creates a store or an entry. If the node holds a
   remove request under the key, it does not apply the refresh request.
4. The refresh request has signature V2. This rule also applies before the cutoff date of signature V1.
5. The refresh request has not expired.
6. The sequence number of the refresh request is higher than the one of the stored add request (see
   [`storage-request-ordering.md`](storage-request-ordering.md)).
7. The node builds the refreshed add request from the data of its stored add request and the values of the refresh
   request. Signature V2 of the refreshed add request is valid.
8. The publisher of the refreshed add request is the owner of the data (rule 7 of
   [`storage-ownership.md`](storage-ownership.md)).

When a node applies a refresh request, it does the following:

9. It replaces the stored add request with the refreshed add request.
10. It sets the publish date of the data to the new creation date, if the data has a publish date.
11. It notifies the applications that the data was refreshed.

### Distribution

12. A node which receives a refresh request from a peer relays it if the node applied it. It does not relay a
    refresh request which it did not apply.
13. Nodes do not store refresh requests, and inventory responses do not contain them. A node ignores a refresh
    request in an inventory response.
14. A node which did not get a refresh request gets the refreshed add request with an inventory response. The
    sequence number of the refreshed add request differs from the one which the node holds, so the inventory filter
    of the node does not contain it (see [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md)).

## Reasons

- Rule 2: a refresh request which the store of the owner rejects cannot be applied by other nodes either, for example
  if the store holds no add request of the data.
- Rule 3: a refresh request contains no data, so a node without the data has nothing to refresh. The class name of
  the request comes from the peer. A store which a node created for it would stay empty.
- Rule 4: a node of a version up to 2.1.13 signs only the hash of the data in a refresh request. This signature covers
  neither the sequence number nor the creation date. A relay could change both. With the highest possible sequence
  number, the owner could not publish, refresh or remove the data anymore until the entry expires. Nodes of these
  versions also do not relay refresh requests. So no node of an earlier version creates or relays a refresh request
  which signature V1 of the refreshed add request verifies. Only a relay which removes signature V2 on purpose does.
  Accepting such a request would let this relay replace a stored add request with signature V2 by one without it.
- Rule 7: the signatures of a refresh request are those of the refreshed add request. So the refreshed add request
  which a node stores is valid for every node which gets it with an inventory response. The signature also binds the
  refresh request to the data: it does not verify for other data.
- Rule 12: a broadcast arrives from several peers. A node applies the first copy. For the later copies the sequence
  number is not higher than the stored one (rule 6), so the node does not relay them, and the relaying ends. A request
  which a node cannot verify is not relayed either.
- Rule 13: the refreshed add request contains everything which a refresh request contains, and the data. A node
  which misses the data cannot use a refresh request.

## Compatibility

- The wire format does not change.
- **Nodes of versions up to 2.1.13 as receivers.** They ignore received refresh requests and do not relay them. They
  get the refreshed add request with an inventory response of a node of this version, or a new add request when the
  owner publishes the data again. Signature V1 of the refreshed add request is valid for them.
- **Nodes of versions up to 2.1.13 as owners.** Nodes of this version do not apply their refresh requests (rule 4).
  The result is `signatureV2Missing`, which is not a severe failure. The data of these owners is renewed on other
  nodes only when the owner publishes it again with an add request. This is the behavior of all versions up to 2.1.13,
  because no version applied received refresh requests.
- **Refreshed add requests of owners of versions up to 2.1.13.** The store of such an owner keeps the signature of
  the previous add request in the refreshed add request. This signature is not valid for the new sequence number and
  creation date. Nodes reject such an add request in inventory responses of the owner. Its cause is in the released
  versions and cannot be removed in this version.
- A refresh request is relayed only by nodes of this version which hold the data. While many nodes run an earlier
  version, a refresh request can miss nodes of this version. They get the refreshed add request with an inventory
  response (rule 14).

## Known limitations

- **Copies with other payload bytes.** Signature V2 covers the exact bytes of the data (see
  [`distributed-data-payload-bytes.md`](distributed-data-payload-bytes.md)). A node whose stored add request contains
  other bytes than the one of the owner cannot verify the refreshed add request (rule 7) and does not apply the
  refresh request. This happens if a node of an earlier version relayed the add request and dropped a field which it
  does not know. The node gets the refreshed add request of the owner with an inventory response, which replaces its
  copy.
  - Why it cannot be removed: a refresh request does not contain the data, so the node cannot correct its copy with
    it.

## Verification

- A node applies a refresh request which it receives from a peer, notifies the applications and relays the request.
  The stored refreshed add request has valid signatures, and a node without the data accepts it.
- A node does not relay a copy of a refresh request which it has applied.
- A node applies and relays neither a refresh request in which a relay changed the sequence number or the creation
  date, nor one without signature V2, nor one of a version up to 2.1.13. This holds before and after the cutoff date
  of signature V1.
- A node without the data does not apply or relay a refresh request and creates no store.
- A node does not apply a refresh request for data which was removed.
- A node which holds a copy of the add request without signature V2 applies the refresh request of the owner. The
  refreshed add request has signature V2.

## Related documents and code

- [`storage-request-signatures.md`](storage-request-signatures.md): the signatures of a refresh request.
- [`storage-request-ordering.md`](storage-request-ordering.md): the sequence numbers.
- `RefreshAuthenticatedDataRequest`, `AuthenticatedDataStorageService.refresh`,
  `StorageService.onRefreshDataRequest`, `DataService.processRefreshDataRequest`, `DataNetworkService`.
