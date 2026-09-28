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

## Preimage of signature V1

| Request | Preimage of signature V1 | Not covered |
| --- | --- | --- |
| `AddAuthenticatedDataRequest` | `AuthenticatedSequentialData.serializeForHash()` | `ownerPublicKeyBytes` (only its hash in the sequential data), and the fields of the distributed data annotated with `@ExcludeForHash` |
| `RemoveAuthenticatedDataRequest` | the hash of the data | `className`, `created`, `sequenceNumber`, `version`, `metaData` |
| `RefreshAuthenticatedDataRequest` | the `AddAuthenticatedDataRequest` which the receiver rebuilds (see [Refresh requests](#refresh-requests)) | as for the add request |
| `AddMailboxRequest` | `MailboxSequentialData.serializeForHash()` | `version` and `className` of `MailboxData`, and its `metaData` in versions 1 to 3 |
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
- `metaData` is deprecated and stays out of the preimage of signature V2 permanently. `serializeForHash()` contains
  `metaData` only in version 0 and in versions above 3 of `MailboxData`, `RemoveMailboxRequest` and
  `RemoveAuthenticatedDataRequest`. Version 0 is no longer in use, and in versions above 3 `metaData` will be retired.
  So no version needs `metaData` in the preimage of signature V2, and the `SIGNATURE_V2_SCHEMA` of
  these classes does not contain it for any version.

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

## Verification

1. If `signatureV2` is not empty, the receiver verifies only signature V2.
2. If `signatureV2` is empty, the receiver verifies signature V1. This accepts requests from nodes of earlier
   versions.
3. A request must contain at least one non-empty signature. Each non-empty signature must have a valid EC signature
   encoding.

Reason for rule 1: signature V2 covers signature V1. A second verification would not protect more fields.

## Compatibility

- `signatureV2` and the `version` of the add requests are annotated with `@ExcludeForHash`. The `className` of the
  remove and refresh requests stays annotated. The proof of work payload (`serializeForHash()`) of a request of this
  version is therefore identical to the payload which a node of an earlier version computes after it drops the unknown
  fields. Without these annotations, nodes of earlier versions would reject the proof of work of every request from
  nodes of this version.
- Nodes of earlier versions ignore `signatureV2` and verify signature V1. Nodes of this version still create signature
  V1 for them.
- The storage keys do not change.

## Known limitations

- **A relay can remove signature V2.** Receivers accept requests with only signature V1 (verification rule 2). A relay
  can therefore remove `signatureV2`, change a field which signature V1 does not cover (see the first table), and set
  the `version` of an add request to 0. Receivers then verify signature V1 and accept the request. Until receivers
  reject requests without signature V2, signature V2 does not protect these fields against a relay. The storage layer
  therefore keeps its own checks for these fields, for example the admission of class names per store type and the
  registered MetaData (see [`storage-metadata.md`](storage-metadata.md)).
  - Why it cannot be removed now: nodes of earlier versions send only signature V1. Rejecting such requests would
    separate these nodes from the network.
  - What removes it: an activation after which nodes reject requests without signature V2, for example at a fixed date
    or when the network requires a minimum version. After the activation, signature V1 can be dropped.
  - Who decides: the developers, when they plan the activation.
- **Signature V1 is not verified when signature V2 is present.** The owner can create a request with a valid signature
  V2 and an invalid signature V1. Nodes of this version accept it; nodes of earlier versions reject it. Only the owner
  can do this, and it affects only the owner's own data.
