# Payload bytes of distributed data

## Purpose

Data of higher-level modules (offers, user profiles, chat messages, bonded roles, account data) passes
through the data storage of the network module as `DistributedData`. A newer version can add fields to
such data. A node that does not know a field must not remove it when it relays, persists, or serves the
data. A signature over the data must be verifiable by such a node.

This specification defines how nodes keep the bytes of this data, and which preimages contain these
bytes.

## Scope

The protobuf fields of type `google.protobuf.Any` that carry `DistributedData`:

- `AuthenticatedData.distributedData`, for default authenticated data and for authorized data;
- `AddAppendOnlyDataRequest.appendOnlyData`.

Not in scope: mailbox data, whose payload is encrypted and signed as cipher text, and the fields of the
network classes themselves.

## Root cause

Before this rule, a node decoded the `Any` into its object model and encoded the model again whenever it
wrote the data: when it relayed a request, persisted its data store, or answered an inventory request.
The model contains only the fields that the version of the node knows. A field that a newer version
added was therefore lost at the first node of an older version. Signatures and storage keys have the
same cause: every node computes them from its own model, so a node that does not know a field computes
other bytes than the signer.

## Retention

- A node keeps the received `Any`: its type URL and its value bytes. The value bytes are not decoded and
  encoded again.
- Whenever the node writes the data (relay, persistence, inventory response), it writes the kept `Any`
  unchanged.
- The decoded object is a view of the kept bytes. It is used to process the data, but never to rebuild
  the bytes.
- Data that the node creates itself is serialized once, completely, when the holder is created. All later
  writes and preimages use these bytes.
- The kept `Any` has exactly two fields: the type URL and the value. Unknown fields of the `Any` message
  itself are not kept, because `google.protobuf.Any` has no other fields and no preimage covers them.
- The type URL must be `type.googleapis.com/` followed by the full name of the protobuf message, as
  protobuf's `Any.pack` creates it and all Bisq versions send it. A node rejects data with another type
  URL. Rationale: the type URL is part of the kept bytes and of the `SIGNATURE` preimage. Type URLs with
  another prefix resolve to the same type, so accepting them would let a relay change the kept bytes
  without changing the decoded data.
- Two holders are equal only if their kept bytes are equal. Of two copies with the same storage key and
  the same sequence number but different payload bytes, a node keeps the copy that it received first:
  the sequence number check rejects the second copy.

### Retained payload size

The retained payload value may be at most **10,240 bytes larger** than the complete protobuf serialization
of the decoded distributed data. Reject a holder that exceeds this limit before it can enter storage
or be relayed. The same rule applies when loading persisted holders. Accepted bytes stay unchanged;
the size check must not truncate or rebuild the retained payload.

The reference is the complete serialization, including known fields annotated with `@ExcludeForHash`,
not the hash serialization. The allowance applies once to the whole payload, including extra bytes in
nested messages. It measures retained byte overhead, not the size of an individual `UnknownFieldSet`:
repeated encodings of known singular fields and other redundant wire encodings can also disappear
during decoding. Checking only unknown fields would leave these ways to enlarge a stored entry open.

Rationale: domain validation and cost factors constrain the decoded data. Retaining an unbounded
amount of additional data bypasses those constraints and can create an entry larger than an entire
inventory response. The limit bounds the additional resource use introduced by retention. Existing
domain limits still govern known data; this rule is not a total message-size limit or authentication.

Compatibility: a newer node's payload is rejected by this node if the extra bytes exceed the allowance,
even when the newer node considers them valid fields. Previously persisted oversized payloads are also
rejected during decoding. No bytes, storage keys, or signature preimages change for accepted data.

## Preimages

| Preimage | Payload bytes in it | Reason |
| --- | --- | --- |
| Complete serialization (`serialize()`), canonical `DEFAULT` purpose | The kept `Any` | Relay and persistence without loss. |
| Canonical `SIGNATURE` purpose | The kept `Any`: type URL and exact value bytes | A signature over it covers every field of the payload, including fields that the verifier does not know and fields annotated with `@ExcludeForHash`. |
| Hash serialization (`serializeForHash()`), canonical `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` purposes | The `Any` rebuilt from the decoded object without the fields annotated with `@ExcludeForHash`, as before this rule | Nodes of all versions must compute the same data hash of the storage key (see [`storage-keys.md`](storage-keys.md)), `signatureV1` preimage, proof of work payload, contract hash, and inventory order. |

In the canonical encoding, the kept `Any` is the message `string type_url = 1; bytes value = 2` with the
kept values. The type URL binds the type of the payload.

Signature V2 of `AddAuthenticatedDataRequest` uses the `SIGNATURE` purpose (see
[`storage-request-signatures.md`](storage-request-signatures.md)). Before the cutoff date of signature V1, receivers
accept requests with only `signatureV1`. Signature V2 therefore protects the payload bytes from the cutoff date on
(see [`storage-request-signatures.md`](storage-request-signatures.md#cutoff-date-of-signature-v1)).

## Adding a field to a `DistributedData` type

The following facts result from the rules above:

1. Nodes of this version and later keep the field, also when they do not know it.
2. Nodes released before this rule drop the field when they relay, persist, or serve the data. The field
   reaches all nodes only after such nodes are no longer supported.
3. If the field is not annotated with `@ExcludeForHash`, nodes that do not know it compute another hash
   serialization: another storage key, and neither `signatureV1` nor the proof of work verifies. These
   nodes reject the data.
4. If the field is annotated with `@ExcludeForHash`, all nodes compute the same hash serialization. Then
   `signatureV1`, the storage key, and the proof of work do not protect the field.
5. The `SIGNATURE` preimage contains the field in both cases. A signature over it protects the field on
   every node of this version and later.

Consequence: when a signature over the `SIGNATURE` preimage is required, a new field is annotated with
`@ExcludeForHash`, and that signature protects it. Until then, each new field needs a decision: include
the field in the hash serialization and set it only when the nodes that do not know it are no longer
supported, or accept a field that no signature protects.

## Limitations during the transition

1. **Nodes released before this rule** drop fields that they do not know (fact 2). Their code cannot be
   changed. A minimum version that excludes them removes this limitation; the developers decide it as part
   of the activation plan.
2. **Unauthenticated payload bytes.** Until a signature over the `SIGNATURE` preimage is required, the
   payload bytes that the decoded object does not contain are not authenticated. A relay can add, change,
   or remove such fields without invalidating `signatureV1`, the storage key, or the proof of work. Nodes
   of this version keep and relay the change, but do not interpret it. A later version that defines such a
   field must rely on it only when a signature over the `SIGNATURE` preimage verifies. Retained payload
   overhead is bounded by the size rule above; the cost factor and the proof of work payload are still
   computed from the decoded object. Cause: no required signature covers the kept bytes. A required
   signature over the `SIGNATURE` preimage removes it (for example `signatureV2` of the add request); the
   developers decide its activation.
3. **Append-only data** has no signature, and its storage key covers only the decoded object (see
   [`storage-keys.md`](storage-keys.md)). Payload fields that the node does not know are not protected by
   any mechanism. A signature over the exact bytes removes this limitation. A storage key over the exact
   bytes does not: it would store each variant of the bytes as a separate entry, instead of rejecting the
   variant.
4. **Authorization signature.** The signature of authorized data (`AuthorizedData.signature`) covers only
   the hash serialization of the distributed data. Fields annotated with `@ExcludeForHash` and fields that
   the verifier does not know are not covered. An authorization signature over the `SIGNATURE` preimage of
   the payload removes this limitation. The authorized signers (for example oracle nodes) must then create
   it; the developers decide the activation.
5. **Copies of different versions.** Because a node keeps the first copy of a sequence number, a copy that
   a node released before this rule has truncated can stay on a node until the owner publishes the
   complete data with a higher sequence number. A refresh does not replace it, because a refresh request
   does not contain the payload.
6. **Unknown types.** A node still rejects data of a `DistributedData` type that it cannot resolve.
   Relaying such data as opaque bytes would be a separate decision.

## Verification

- Tests with payload data that contains a field unknown to the node show that the field survives
  decoding, relaying over two hops, persistence of the data store, and an inventory response; that the
  hash serialization and the storage key equal those of a node that knows only the model; and that the
  `SIGNATURE` encoding contains the field, stays equal after relaying, and changes when a relay changes
  the field.
- Tests show that type URLs other than the one of `Any.pack` are rejected, and that unknown fields of the
  `Any` message are not kept.
- Tests cover the retained-byte allowance at its boundary and above it for default authenticated,
  authorized, and append-only data, including nested unknown fields and redundant known fields.
  Oversized requests must fail during decoding, including when restored from a data store. Accepted
  payloads must retain their exact bytes and their existing hash serialization.

## Related documents and code

- [`../encoding/canonical-encoding.md`](../encoding/canonical-encoding.md): encoding purposes and the
  encoding of data of higher-level modules.
- `network/network/src/main/java/bisq/network/p2p/services/data/storage/DistributedDataAny.java`: the
  checks of a received `Any` and its canonical schema.
- `DistributedDataAnyTest` in the test sources of the network module.
