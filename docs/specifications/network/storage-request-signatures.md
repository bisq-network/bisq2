# Signatures of storage requests

This specification defines the signatures of the requests which add, remove, or refresh signed data in the network
storage: `AddAuthenticatedDataRequest`, `RemoveAuthenticatedDataRequest`, `RefreshAuthenticatedDataRequest`,
`AddMailboxRequest`, and `RemoveMailboxRequest`. `AddAppendOnlyDataRequest` has no signature.

Each of these requests can carry two signatures:

- **Signature V1** (field `signature`) is the signature of earlier versions. Its preimage leaves out several fields of
  the request.
- **Signature V2** (field `signatureV2`) was added in v2.1.14. Its preimage covers every field of the request which a
  relay must not change.

Both signatures are created with the key of the owner (for mailbox add requests the sender, for mailbox remove requests
the receiver). The canonical encoding that produces the preimage of signature V2 is defined in
[`../encoding/canonical-encoding.md`](../encoding/canonical-encoding.md).

Nodes accept a request without signature V2 only before the cutoff date of signature V1, 2027-01-01 (see
[Cutoff date of signature V1](#cutoff-date-of-signature-v1)).

## Preimage of signature V1

| Request | Preimage of signature V1 | Not covered |
| --- | --- | --- |
| `AddAuthenticatedDataRequest` | `AuthenticatedSequentialData.serializeForHash()` | `ownerPublicKeyBytes` (only its hash in the sequential data), and the fields of the distributed data annotated with `@ExcludeForHash` |
| `RemoveAuthenticatedDataRequest` | the hash of the data | `className`, `created`, `sequenceNumber`, `version`, `metaData` |
| `RefreshAuthenticatedDataRequest` | the `AddAuthenticatedDataRequest` which the receiver rebuilds (see [Refresh requests](#refresh-requests)) | as for the add request |
| `AddMailboxRequest` | `MailboxSequentialData.serializeForHash()` | `version`, `className` and `metaData` of `MailboxData` |
| `RemoveMailboxRequest` | the hash of the mailbox data | `className`, `created`, `version`, `metaData` |

The fields which signature V1 does not cover are annotated with `@ExcludeForHash`, or they did not exist when the
signature was defined.

## Requirements for signature V2

1. A node of this version creates both signatures for every request. It signs signature V1 first, because the preimage
   of signature V2 contains signature V1.
2. The preimage of signature V2 is the canonical encoding of the request for the `SIGNATURE` purpose. It is a new
   message, not the protobuf message of the request:
   - Field 1 is a domain separation tag: the string `bisq.<simple class name of the request>.signatureV2`. A signature
     for one request type therefore cannot be valid for another request type.
   - The other fields are all fields of the request except `signatureV2` itself and the deprecated `metaData`. This
     includes signature V1, the public key, and the `version` of the request.
   - Nested objects are encoded for the `SIGNATURE` purpose as well. Their fields annotated with `@ExcludeForHash` are
     included. For `MailboxData` these are `version` and `className`; its deprecated `metaData` is left out. Data of
     higher-level modules is included with the exact bytes which the node received (see
     [`distributed-data-payload-bytes.md`](distributed-data-payload-bytes.md)).
3. The preimage never contains `signatureV2`. The creator computes the preimage before the signature exists, and the
   receiver must compute the same bytes.
4. The field layout of each preimage is fixed. The `SIGNATURE_V2_SCHEMA` of each request defines it. A change of the
   layout is a protocol change.
5. Request version 2 is the first version with signature V2. The add requests carry a `version` field since v2.1.14.
   A request of an earlier version has version 0 (add requests) or 1 (remove and refresh requests).
6. A request with signature V2 must have version 2 or higher. The remove and refresh requests and `MailboxData` must
   have version 1 or higher, also without signature V2. Parsing rejects a request which breaks this rule.

   Reason: version 0 is the format of releases before the version field existed. Since v2.1.0, nodes send version 1
   for the remove and refresh requests and for `MailboxData`, without signature V2. The add requests carry a version
   since v2.1.14. Every node which signs with signature V2 sets version 2. So such a request comes from no supported
   sender.

| Request | Fields which signature V2 covers, besides the tag |
| --- | --- |
| `AddAuthenticatedDataRequest` | `version`, `authenticatedSequentialData`, `ownerPublicKeyBytes`, `signature` |
| `RemoveAuthenticatedDataRequest` | `version`, `className`, `created`, `sequenceNumber`, `hash`, `ownerPublicKeyBytes`, `signature` |
| `AddMailboxRequest` | `version`, `mailboxSequentialData` (with `version` and `className` of `MailboxData`), `senderPublicKeyBytes`, `signature` |
| `RemoveMailboxRequest` | `version`, `className`, `created`, `hash`, `receiverPublicKeyBytes`, `signature` |

Reasons for leaving out `metaData`:

- No node of this version uses the received value for a decision. The storage layer takes the MetaData from its own
  registry (see [`storage-metadata.md`](storage-metadata.md)). Nodes of earlier versions use it, but they do not
  verify signature V2.
- A node replaces the received `metaData` with its registered MetaData before it relays or persists the request (see
  [`storage-metadata.md`](storage-metadata.md#compatibility)). If signature V2 covered `metaData`, this replacement
  would invalidate it.
- `metaData` is deprecated and stays out of the preimage of signature V2 permanently. It is derived from the class
  name, which signature V2 covers, and no hash contains it in any version (see
  [`storage-metadata.md`](storage-metadata.md#compatibility)). So a relay which changes `metaData` changes nothing which
  any node uses for a decision, and nodes of earlier versions receive the registered value again from the next node of
  this version.

Reason for including signature V1: if signature V2 did not cover signature V1, a relay could replace signature V1.
Nodes of earlier versions and nodes of this version would then decide differently about the same request.

### Refresh requests

A refresh request has no preimage of its own. The owner creates an `AddAuthenticatedDataRequest` with the new sequence
number and creation date, signs both signatures over it, and copies the signatures, the public key, the sequence
number, the creation date, and the version into the refresh request.

The receiver rebuilds the `AddAuthenticatedDataRequest` from its stored entry and these values, and verifies it. The
rebuilt request must use the version of the refresh request. Otherwise the preimage of signature V2 differs from the
one of the owner, and the refresh fails.

The `className` of a refresh request is not covered by a signature. It only selects the store. A store of another type
has no entry with the hash of the request, so a changed value makes the refresh fail.

A refresh request needs signature V2, also before the cutoff date of signature V1. The receiver verifies signature V2
of the rebuilt request.

Reason: a node of a version up to 2.1.13 signs only the hash of the data in a refresh request. This signature covers
neither the sequence number nor the creation date, and it does not verify for the rebuilt request. Nodes of these
versions also do not relay refresh requests. So only a relay which removes signature V2 on purpose creates a refresh
request without signature V2 whose signature V1 verifies. Accepting such a request would let this relay replace a
stored add request with signature V2 by one without it.

The rules by which nodes apply and relay refresh requests are defined in
[`storage-refresh-requests.md`](storage-refresh-requests.md).

## Verification

1. If `signatureV2` is not empty, the receiver verifies only signature V2.
2. If `signatureV2` is empty, the receiver verifies signature V1, but only before the cutoff date of signature V1
   (rule 5). This accepts add and remove requests from nodes of earlier versions. It does not apply to refresh
   requests, which need signature V2 (see [Refresh requests](#refresh-requests)).
3. A request must contain at least one signature. Before the cutoff date of signature V1, it must contain signature
   V1, also if it contains signature V2. From the cutoff date on, signature V1 is optional. Each non-empty signature
   must have a valid EC signature encoding. The node checks this rule whenever it builds a request, with its own
   time: when it parses a received request, when it loads a persisted request, and when it creates a request (see
   [`validation-at-parsing.md`](validation-at-parsing.md)). Rule 3 does not require signature V2. The stores require
   it from the cutoff date on (rule 5).
4. Signature V2 must be in low-S form. Signature V1 may be in either form. The rules are defined in
   [`../security/ecdsa-signature-encoding.md`](../security/ecdsa-signature-encoding.md).

Reason for rule 1: signature V2 covers signature V1. A second verification would not protect more fields.

Reason for rule 3: nodes of earlier versions cannot parse a request without signature V1. Before the cutoff date, a
request without signature V1 would therefore reach only nodes of this version, and nodes of earlier versions would
drop each inventory response which contains it. Nodes of this version create both signatures (rule 7), so only a
modified client creates such a request. From the cutoff date on, the node does not use signature V1 (rule 5).

Reason why rule 3 does not require signature V2: the rule applies to every request which a node builds, not only to a
received request.

- The creator of a request builds the request without signature V2 first, because the preimage of signature V2 is the
  canonical encoding of that request. If rule 3 required signature V2 from the cutoff date on, no node could create a
  request from that date on.
- The mailbox store keeps remove requests without signature V2 after the cutoff date (rule 9). A node parses the
  persisted requests when it loads a store. If rule 3 required signature V2, a restart would drop these remove
  requests.
- The rule "signature V2 is required from the cutoff date on" then has one owner: the stores (rule 5). They use their
  clock, which a test can set.

Reason for rule 4: a relay can change the S value of a signature without the private key. If signature V2 accepted
both forms, a request would have two valid encodings of signature V2. Signature V1 accepts both forms because nodes of
earlier versions create high-S signatures in about half of all cases. Nodes of this version create both signatures in
low-S form.

## Cutoff date of signature V1

The cutoff date is 2027-01-01 00:00 UTC (`SignedRequest.SIGNATURE_V1_CUTOFF_DATE`). Until this date, nodes accept
requests without signature V2, so that nodes of earlier versions can take part in the network.

5. From the cutoff date on, a node accepts a request only if it has signature V2. The node rejects a request without
   signature V2 and does not verify signature V1. The result is `signatureV2Missing`. The rule applies to add and
   remove requests, from broadcasts and from inventory responses. A refresh request needs signature V2 also before
   the cutoff date (see [Refresh requests](#refresh-requests)).
6. The node compares the cutoff date with its own time. The creation date of the request is not used.
7. A node of this version creates both signatures also after the cutoff date.
8. The stores apply rule 5 with the clock of the store. A node can still parse a request without signature V2 from
   the cutoff date on (verification rule 3), but no store accepts it. So the node does not store it, does not relay
   it and does not give its data to the applications.
9. From the cutoff date on, a store removes the add requests without signature V2 which it holds. The store of
   authenticated data also removes the remove requests without signature V2. The mailbox store keeps them until they
   expire. A node does this when it loads a store, and with the task which removes expired entries. Applications get
   the removal of the data in the same way as at expiry.

Reasons:

- Rule 5 removes the cause of the first two limitations in [Known limitations](#known-limitations): a relay can no
  longer remove signature V2 and change a field which signature V1 does not cover.
- Rule 6: signature V1 of the remove requests does not cover `created`. If the rule used the creation date, a relay
  could set an earlier date, and nodes would accept changed remove requests until the cutoff date plus the time to live
  of the type, which is up to 100 days. Signature V1 of the add requests covers `created`. But a rule which used the
  creation date only for add requests would accept the add requests of a node of an earlier version and reject its
  remove requests. Then data which its owner removed would stay in the stores.
- Rule 7: nodes of earlier versions verify only signature V1, and the preimage of signature V2 contains signature V1.
- Rule 8: whether signature V2 is required depends on the time of the node, not only on the request. Such a rule
  belongs to the component which owns the state (rule 5 of [`validation-at-parsing.md`](validation-at-parsing.md)).
  A request which a store accepted before the cutoff date is still in the store. The stores remove such requests
  (rule 9). See also the reason why verification rule 3 does not require signature V2.
- Rule 9: a store would otherwise hold requests which it does not accept, and which other nodes do not accept from its
  inventory responses. A request which a relay changed before the cutoff date would also stay until it expires. A
  remove request contains no data. In the mailbox store it rejects the add request of the removed message until it
  expires. Without it, a receiver which removed a message with a node of an earlier version would get the message
  again. In the store of authenticated data, a remove request without signature V2 rejects only the add request which
  it replaced, and earlier ones (see [`storage-request-ordering.md`](storage-request-ordering.md)). From the cutoff
  date on, the store rejects an add request without signature V2 in any case. An add request with signature V2 is of a
  node of this version. Its removal has signature V2 and replaces the copy without signature V2 when the node gets it.
  So the store does not keep the remove request without signature V2 (see also [Open points](#open-points)).

### Conditions for the cutoff date

The cutoff date is part of the release. A later release can set a later date only for the nodes which install that
release. Nodes which stay on this version keep the date. So the following must hold before the cutoff date:

- Oracle and seed nodes run this version and have published their data again (see rules 14 and 15 of
  [`storage-ownership.md`](storage-ownership.md)).
- Security managers, moderators and release managers have published their data again with this version. This is the
  data with a time to live of 100 days which no migration publishes again: alerts, banned user profiles, release
  notifications, the difficulty adjustment and the minimum reputation score.
- Most users run this version. A node of an earlier version cannot publish data to nodes of this version from the
  cutoff date on (see [Compatibility](#compatibility)).

### After the cutoff date

The following can be removed when no supported version accepts requests without signature V2:

- the verification of signature V1 (verification rule 2) and the exception for the high-S form of signature V1;
- the rules for V1 removals in [`storage-request-ordering.md`](storage-request-ordering.md);
- rule 9, when every request without signature V2 has expired. This is 100 days after the cutoff date, which is the
  longest time to live of a type.

The creation of signature V1 (rule 7) can be removed when nodes of earlier versions are no longer supported. They
reject a request without signature V1.

## Compatibility

- `signatureV2` and the `version` of the add requests are annotated with `@ExcludeForHash`. The `className` of the
  remove and refresh requests stays annotated. The proof of work payload (`serializeForHash()`) of a request of this
  version is therefore identical to the payload which a node of an earlier version computes after it drops the unknown
  fields. Without these annotations, nodes of earlier versions would reject the proof of work of every request from
  nodes of this version.
- Nodes of earlier versions ignore `signatureV2` and verify signature V1. Nodes of this version still create signature
  V1 for them.
- From the cutoff date of signature V1 on, nodes of earlier versions are not supported. Nodes of this version do not
  accept the requests which nodes of earlier versions create. Applications of nodes of this version then do not get the
  data of these nodes: user profiles, offers, chat messages, and the messages which they send to a mailbox. Nodes of
  v2.1.13 send a confidential message only to the mailbox when they have no connection to the receiver yet, also when
  the receiver is online. Such a message is lost too, for example the take offer request of a taker of v2.1.13, or a
  message of a trade which is open across the cutoff date. Nodes of earlier versions still accept the requests of
  nodes of this version.
- Nodes of earlier versions do not know `signatureV2`. They remove it when they relay, persist or serve a request.
  From the cutoff date on, a node of this version therefore accepts a request only if it got the request from its
  owner, or if all nodes which relayed it are nodes of this version. For the same reason, it no longer requests
  inventory from nodes of earlier versions from that date on (rule 8 of
  [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#configuration)).
- Signature V2 does not change the storage keys. The storage keys are specified in
  [`storage-keys.md`](storage-keys.md).

## Known limitations

- **Before the cutoff date, a relay can remove signature V2.** Receivers accept requests with only signature V1
  (verification rule 2). A relay can therefore remove `signatureV2`, change a field which signature V1 does not cover
  (see the first table), and set the `version` of an add request to 0. Receivers then verify signature V1 and accept
  the request. The storage layer therefore keeps its own checks for these fields, for example the admission of class
  names per store type and the registered MetaData (see [`storage-metadata.md`](storage-metadata.md)).
  - Why it cannot be removed before the cutoff date: nodes of earlier versions send only signature V1. Rejecting such
    requests separates these nodes from the network.
  - What removes it: the cutoff date (rule 5).
- **Before the cutoff date, signature V1 is malleable in requests without signature V2.** A relay can replace the S
  value of signature V1 (verification rule 4). The request stays valid and has the same storage key and sequence
  number, so a node which stores the original rejects the changed copy. In requests with signature V2, the change
  invalidates signature V2, because signature V2 covers signature V1. The cutoff date removes this limitation as well.
- **Signature V1 is not verified when signature V2 is present.** The owner can create a request with a valid signature
  V2 and an invalid signature V1. Nodes of this version accept it; nodes of earlier versions reject it. Only the owner
  can do this, and it affects only the owner's own data.
- **From the cutoff date on, a request without signature V1 is valid for nodes of this version.** A modified client
  can create such a request. Nodes of this version accept, store and serve it. Nodes of earlier versions cannot parse
  it. A node of an earlier version which requests inventory with `HASH_SET` from a node of this version which holds
  it drops the whole response (see
  [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#entries-which-a-node-cannot-parse)).
  - Why it is accepted: from the cutoff date on, nodes of this version accept no request of nodes of earlier versions
    (rule 5). Signature V1 can only become optional at this date, so that a later version can stop creating it.
  - What removes it: the end of the support of `HASH_SET` (see
    [`inventory-hash-set-filter.md`](inventory-hash-set-filter.md#limitations)), or a rule which requires signature V1
    as long as nodes of this version answer requests with `HASH_SET`.
  - Who decides: the developers.
- **The cutoff date depends on the time of the node.** A node whose clock shows a time before the cutoff date still
  accepts requests without signature V2. A node whose clock is ahead requires signature V2 earlier.
  - Why it cannot be removed: a request contains no time which a node can trust more than its own clock. Signature V1
    does not cover the creation date of remove requests (reason for rule 6).
- **Nodes of earlier versions** accept requests without signature V2 also after the cutoff date. They stay affected
  by the first two limitations until they update.

## Open points

The following weaknesses can be removed by a change of other rules. The developers have not decided these changes yet.

- **Copies without signature V2 of requests of this version.** A node of an earlier version removes signature V2 when
  it relays a request. Before the cutoff date, a node of this version which gets such a copy first stores it. It
  rejects the complete request which arrives later, because both have the same sequence number. At the cutoff date,
  the store removes the copy (rule 9), and the applications get the removal of data whose owner runs this version. The
  node gets the complete request again with an inventory response of a node which holds it, or when the owner
  publishes the data again.
  - What removes it: a rule in [`storage-request-ordering.md`](storage-request-ordering.md) by which a request with
    signature V2 replaces the stored copy of the same request without signature V2. With this rule, the stores hold
    the complete requests before the cutoff date.
- **Removed data of nodes of this version after the cutoff date.** A node can hold only the copy without signature V2
  of a removal of a node of this version, because it got the removal only from nodes of earlier versions. At the
  cutoff date the store removes the copy (rule 9). After that, the node accepts the add request with signature V2 again
  which the removal replaced, until it gets the removal with signature V2.
  - What removes it: the store of authenticated data keeps its remove requests without signature V2 until they expire,
    as the mailbox store does. The cost is that of the last point of this list.
- **Remove requests without signature V2 in inventory responses.** Until they expire, the mailbox stores serve their
  remove requests without signature V2 (rule 9) in inventory responses. Nodes of this version reject them each time.
  This ends 15 days after the cutoff date.
  - What removes it: from the cutoff date on, a node does not add requests without signature V2 to an inventory
    response for a filter of type `HASH_SET_V2`.
