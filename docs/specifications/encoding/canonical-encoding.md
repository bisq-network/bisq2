# Canonical encoding

## Scope

This specification defines the canonical encoding in Bisq 2: the deterministic byte format for hash and
signature preimages that the framework `bisq.common.encoding.canonical` produces. It defines the
required properties, the wire rules, the supported subset of protobuf, the error behavior, and the rules
for changing an encoding.

How Java model structures (composition, inheritance, polymorphic fields, envelope messages, data of
other modules) map to canonical messages is specified in
[`canonical-model-mapping.md`](canonical-model-mapping.md).

### Status in Bisq 2

- The framework was copied from Bisq 1, where it replaced protobuf serialization in existing hash and
  signature preimages. Bisq 2 extends it with ZigZag integer fields (`sint32`, `sint64`), the
  `canonical` field, and the `encodedMessage` field, and it passes the encoding purpose to nested
  objects (see [Encoding purpose](#encoding-purpose)).
- The network module classes that the data storage uses implement a canonical encoding:
  `MetaData`, `AuthenticatedData` (`DefaultAuthenticatedData`, `AuthorizedData`),
  `AuthenticatedSequentialData`, `MailboxData`, `MailboxSequentialData`, `ConfidentialMessage`, and all
  `DataRequest` types. `ConfidentialData` of the security module, which `ConfidentialMessage` contains,
  implements it as well.
- The classes with fields annotated with `@ExcludeForHash` (`MailboxData`, the add, remove, and
  refresh requests of authenticated data, and the add and remove requests of mailbox data) leave
  these fields out of their `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` schemas. Because the
  purpose reaches nested objects, the `STORAGE_KEY` encodings of all storage data classes equal their
  `serializeForHash()`, including `AuthorizedData`. Only the stored data has a `STORAGE_KEY` encoding:
  the `DataRequest` types and the sequential data classes have none, because their storage keys are not
  encodings of the request (see [`../network/storage-keys.md`](../network/storage-keys.md)). The
  `PROOF_OF_WORK` encodings of the messages (all `DataRequest` types and `ConfidentialMessage`) equal
  their `serializeForHash()` without exception.
- The `SIGNATURE` purpose of the add and remove requests is the preimage of signature V2 (see
  [`../network/storage-request-signatures.md`](../network/storage-request-signatures.md)). These
  requests and `MailboxData` have a `SIGNATURE_V2_SCHEMA` that contains their `@ExcludeForHash`
  fields. The other classes use their default schema for `SIGNATURE`, which contains data of
  higher-level modules with its exact bytes.
- The production call sites of canonical encoding are signature V2 and the data hash of storage key V2
  (see [`../network/storage-keys.md`](../network/storage-keys.md)). Storage key V1, the preimages of
  signature V1, proof of work payloads, trade contract hashes, and the inventory order still use
  `serializeForHash()`. Switching such a call site is a protocol change; see
  [Changing an encoding](#changing-an-encoding).
- `AppendOnlyData` extends `Canonical`: its implementations must support canonical encoding (see
  [Data of other modules](#data-of-other-modules)).
- The classes of trade contracts (the contract module and the classes that a contract contains, for
  example `Offer`) have no canonical encoding yet. The `TRADE_CONTRACT` purpose defines the encoding
  that they must implement (see
  [Byte parity when a call site changes](#byte-parity-when-a-call-site-changes)).
- Other classes of higher-level modules (for example offers, user profiles, chat messages) have no
  canonical encoding yet. The network classes hold them in a `google.protobuf.Any` and keep the received
  bytes of that `Any`. The `DEFAULT` and `SIGNATURE` purposes contain these bytes; the `STORAGE_KEY`,
  `PROOF_OF_WORK`, and `TRADE_CONTRACT` purposes contain the protobuf serialization for hashes; see
  [Data of other modules](#data-of-other-modules).

Out of scope: protobuf serialization of network messages and persisted data, and preimages built by
explicit byte concatenation. Persisted containers get no canonical encoding, because no hash or
signature preimage contains them. Example: `DataStore`, the persisted map of the data storage, does not
implement `Canonical`.

## Purpose

Protobuf serialization is not canonical. The same logical message has many valid encodings, and the
bytes produced by a protobuf library depend on implementation details such as map iteration order,
the handling of unknown fields, and the library version. A hash or signature over such bytes can
change although the protected data did not change, and two nodes can produce different bytes for the
same data.

Canonical encoding removes this dependency. Bisq defines the exact bytes itself, in Java code that
Bisq owns, instead of relying on the output of the protobuf library.

## Required properties

### Determinism

Encoding the same object state must produce the same bytes on every node that runs the same schemas.
The bytes must depend only on:

- the values of the encoded fields;
- the order of list elements as held by the model object;
- for map fields, the entry order that the schema declares (see [Map fields](#map-fields)).

The bytes must not depend on the protobuf runtime, on the iteration order of an arbitrary Java map,
on object identity, on caches, or on the thread, locale, or time. Data of higher-level modules is the
exception (see [Data of other modules](#data-of-other-modules)): its kept bytes are part of the object
state and are written unchanged, but they were produced by a protobuf serialization, and the
`STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` purposes use its protobuf serialization for
hashes until those classes support canonical encoding.

### Complete encoding

The default encoding of a class contains all its non-transient fields, with the field numbers and
protobuf types of its protobuf message, and with the envelopes that `serialize()` writes for the
class. The canonical bytes are therefore identical to `serialize()` (`toProto(false).toByteArray()`,
protobuf-java 4.28 in its default mode), also for a class that holds data of a higher-level module,
because both write the kept bytes of that data. The tests of each schema verify this identity.

The default encoding is not identical to `serializeForHash()`:

- `serializeForHash()` omits fields annotated with `@ExcludeForHash` (for example `metaData` and
  `version` of the remove, refresh, and mailbox classes). The default encoding contains them. The
  `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` encodings leave them out (see
  [Encoding purpose](#encoding-purpose)).
- `AuthorizedData.serializeForHash()` omits the signature, so that republishing keeps the storage key.
  The `STORAGE_KEY` encoding of `AuthorizedData` omits it as well. All other canonical encodings of
  `AuthorizedData` contain the signature: `AuthorizedData` is nested in their preimages, where
  `toProto(true)` contains it.
- `serializeForHash()` rebuilds data of a higher-level module from the decoded object, without its
  fields annotated with `@ExcludeForHash` and without the fields that the node does not know. The
  default and `SIGNATURE` encodings contain the kept bytes of that data.

Rationale: a preimage must cover every field that a third party must not change. A field that is left
out of a preimage for a specific purpose must be left out by the schema for that purpose (see
[Encoding purpose](#encoding-purpose)), not by the complete encoding.

### Byte parity when a call site changes

A hash or signature call site may only switch from `serializeForHash()` to canonical encoding if the
canonical bytes for that call site are identical to the bytes that nodes of older versions compute, or
if the switch is activated as a protocol change (see [Changing an encoding](#changing-an-encoding)).
Other nodes compare these hashes and verify these signatures; one differing byte breaks signature
verification, storage keys, and proof of work verification between node versions.

The `STORAGE_KEY` encodings of the stored data classes equal their `serializeForHash()`, also for
`AuthorizedData`. The data hash of storage key V2 uses them (see
[`../network/storage-keys.md`](../network/storage-keys.md)). The `STORAGE_KEY` purpose is used only for
this data hash, where the stored data is always the root of the preimage. The inventory order has no
purpose with identical bytes yet (see [Unresolved questions](#unresolved-questions)). A call site
without such a purpose needs a new purpose-specific schema or an activation.

The `PROOF_OF_WORK` encoding of a message equals `serializeForHash()` of the message, which is the
proof of work payload that nodes of all versions compute. `AuthorizedData` is no exception here: in a
proof of work payload it is always nested in a request, where `toProto(true)` contains its signature.
Only the messages of the data storage and `ConfidentialMessage` have a canonical encoding. The proof of
work call site can therefore switch to canonical encoding only after all other network message types
have one, or for these messages alone.

The `TRADE_CONTRACT` encoding of a contract must equal `serializeForHash()` of the contract. The
contract service hashes these bytes to the contract hash, which each trader signs. The peer verifies
that signature, or compares the peer's contract hash with the hash of its own contract, and each trade
persists the contract signature data. Traders on different versions must therefore compute the same
contract hash, and the contract hash of a persisted trade must not change. Every class that a contract
contains therefore leaves its `@ExcludeForHash` fields out of its `TRADE_CONTRACT` schema, with the
same version conditions, and a nested `AuthorizedData` keeps its signature, as `toProto(true)` does.
No contract class has a canonical encoding yet, so the contract hash call site cannot switch yet. The
existing implementations, which a contract does not contain, select for `TRADE_CONTRACT` the encoding
that reproduces their bytes inside the hash serialization of an enclosing message, as for
`PROOF_OF_WORK`. For most of them, these bytes equal their `serializeForHash()`. `AuthorizedData` is the
exception: its `TRADE_CONTRACT` encoding contains the signature and equals
`toProto(true).toByteArray()`. Its `serializeForHash()` omits the signature, because it defines the
preimage in which `AuthorizedData` is the root (the data hash of the storage key).

The `SIGNATURE` encodings contain the exact bytes of data of higher-level modules and the
`@ExcludeForHash` fields of the network classes. They are therefore not identical to the preimages of
signature V1, which keep `serializeForHash()`. The `SIGNATURE` purpose is the preimage of signature V2,
a new signature without legacy bytes (see
[`../network/storage-request-signatures.md`](../network/storage-request-signatures.md)).

### Preimages without legacy bytes

A new preimage that no node computed before has no parity requirement. Its bytes are defined only by
its schema, and all wire rules of this specification apply. Such a preimage should start with a
domain-separation string that names its purpose and version, so that its bytes cannot be confused
with the preimage of another purpose.

### One source of truth

The Java schema (`CanonicalSchema`) is the only definition of the bytes of a canonical message.
Documentation describes rules; it must not repeat field lists of schemas, because such copies drift.

## Concepts

- **Canonical object**: an object that implements `Canonical` and returns its canonical bytes from
  `encodeCanonical`.
- **Schema**: an ordered list of field definitions for one Java type. Each field has a field number,
  a field type, and a getter that reads the value from the object. Message fields, list-of-message
  fields, and map fields also have a nested schema or a map encoding.
- **Message**: the bytes produced by one schema for one object: the concatenation of its field
  records.
- **Field record**: one protobuf wire record: a tag (field number and wire type) followed by the
  value.
- **Encoder**: `CanonicalEncoder` executes a schema against an object, for an encoding purpose (see
  [Encoding purpose](#encoding-purpose)). `CanonicalWriter` writes the protobuf wire records.

## Wire format

The canonical encoding uses the protobuf binary wire format
(<https://protobuf.dev/programming-guides/encoding/>) with the restrictions of this section.

### Tags, lengths, and field numbers

A tag is the varint of `(field number << 3) | wire type`. Tags, length prefixes, and non-negative
integer values use the shortest varint form. Field numbers must be in the range `1..536870911` and
must not use the range `19000..19999`, which protobuf reserves.

### Field order

A message contains its field records in ascending field-number order. A singular field produces at
most one record. The records of one repeated or map field are contiguous. A message contains nothing
else: no unknown fields, no records of fields that the schema does not declare, and no trailing
bytes.

The schema builder rejects a field declaration whose number is not greater than the number of the
previous declaration. This makes an out-of-order or duplicate field impossible in a schema.

### Field types

| Builder method | Protobuf type | Wire type | Value | No record when |
| --- | --- | --- | --- | --- |
| `int32` | `int32` | varint | two's complement; a negative value is a 10-byte varint | value is `0` |
| `uint32` | `uint32` | varint | the Java `int` read as an unsigned 32-bit value | value is `0` |
| `int64` | `int64` | varint | two's complement; a negative value is a 10-byte varint | value is `0` |
| `sint32` | `sint32` | varint | ZigZag: `(value << 1) ^ (value >> 31)` | value is `0` |
| `sint64` | `sint64` | varint | ZigZag: `(value << 1) ^ (value >> 63)` | value is `0` |
| `doubleField` | `double` | fixed64 | `Double.doubleToRawLongBits`, little-endian | raw bits are `0` (only `+0.0`) |
| `bool` | `bool` | varint | `1` | value is `false` |
| `enumField` | enum | varint | `CanonicalEnum.getCode()`, encoded as `int32` | value is `null` or code is `0` |
| `string` | `string` | length-delimited | UTF-8 bytes of the Java string | value is `null` or empty |
| `bytes` | `bytes` | length-delimited | the bytes | value is `null` or empty |
| `compose`, `extend`, `oneof` | message | length-delimited | canonical bytes of the nested schema | getter returns `null` |
| `canonical` | message | length-delimited | canonical encoding of the value itself, for the purpose of the enclosing object (`encodeCanonical(encoder, purpose)`) | getter returns `null` |
| `encodedMessage` | message | length-delimited | bytes produced outside the canonical encoding, unchanged | getter returns `null` |
| `repeatedString` | `repeated string` | length-delimited, one record per element | UTF-8 bytes of the element | list is empty |
| `packedRepeatedInt32` | `repeated int32`, packed | length-delimited, one record | `int32` varints of all elements | list is empty |
| `repeatedCompose` | `repeated` message | length-delimited, one record per element | canonical bytes of the element | list is empty |
| `mapStringToString`, `mapStringToCompose` | `map<string, string>`, `map<string, message>` | length-delimited, one record per entry | entry message (see [Map fields](#map-fields)) | map is empty |

Additional rules:

- `-0.0` is encoded. A NaN is encoded with its exact raw bits, so different NaN bit patterns produce
  different bytes. This is the behavior of protobuf-java.
- Strings are not normalized. A string is encoded with `String.getBytes(UTF_8)`, which replaces an
  unpaired surrogate with `?` (`0x3F`). protobuf-java uses the same replacement.
- Elements of repeated fields are never omitted. An empty string element and a `0` element are
  encoded.
- A nested message whose schema produces no bytes is still encoded when the getter returns a
  non-null value: the record is the tag followed by the length `0`. The same applies to an empty byte
  array in an `encodedMessage` field.
- `compose`, `extend`, `oneof`, `canonical`, and `encodedMessage` produce the same wire format: a
  length-delimited nested message. The names describe where the nested bytes come from and the role of
  the field in the model (see [`canonical-model-mapping.md`](canonical-model-mapping.md)).

### Presence of values

Singular scalar, enum, string, and bytes fields follow proto3 implicit presence: an absent value
(`null`) and the default value (`0`, `false`, empty) produce the same bytes, which is no record. Two
objects that differ only in `null` versus default value of such a field have identical canonical
bytes. The protobuf wire format cannot express this difference for implicit-presence fields either.

Explicit presence (proto3 `optional`, wrapper types) is not supported. Bisq 2 uses `optional` for
`Optional` Java fields. For such a field, the canonical bytes equal the protobuf bytes only if a
present value is never the default value: a present `0`, `false`, or empty value is written by
protobuf but not by the canonical encoding. `AuthorizedData.signature` (`optional bytes`) is correct
because a present signature is validated and never empty. A field whose present value can be the
default needs explicit presence support in the framework before it can be encoded canonically.

A nested message field has explicit presence: a non-null value always produces a record.

### Map fields

A map entry is a message with the key as field `1` and the value as field `2`. Both are always
written, also when the key or the value is empty or zero, as protobuf-java does for map entries. A
message value is written with the canonical bytes of the value schema.

Protobuf does not define an order for map entries. Every map field must therefore declare its entry
order explicitly with a `CanonicalMapEntryIterator`. The encoder first converts each source entry
into a canonical entry with the key mapper and the value mapper of the field, then writes the
entries in the order that the iterator returns. The following orders are supported:

1. **Sorted order**: `TreeMapIterator.naturalOrder()` or `TreeMapIterator.comparing(...)`. For
   string keys, natural order is the order of `String.compareTo`, which compares UTF-16 code units.
   It is not the order of the UTF-8 bytes. The iterator rejects `null` keys and duplicate keys. Use
   sorted order for new map fields.
2. **Legacy `Collectors.toMap` order**: `LegacyCollectorsToMapIterator`. It reproduces the iteration
   order of a `java.util.HashMap` with string keys that `Collectors.toMap(...)` created with default
   capacity and load factor. It exists for Bisq 1 DAO state hashes and must not be used in Bisq 2
   without a legacy preimage that was produced exactly this way.
3. **Source iteration order**: an iterator that returns the entries in the iteration order of the
   source map (for example `List::iterator`). It is allowed only when the map type of the model
   defines a deterministic order that equals the order that the preimage requires, for example a
   `TreeMap`. It does not check for duplicate keys and must only be used without a key mapper.

Two entries with the same canonical key are not canonical. The iterators of orders 1 and 2 reject
them.

The default iteration order of an arbitrary Java map and the deterministic mode of protobuf-java
must not be used to define a canonical order. Bisq 2 avoids protobuf maps in data that must be
deterministic (see `docs/dev/protobuf-notes.md`); none of the network storage classes has a map field.

## Data of other modules

A network class can contain data of two kinds of other modules:

- **Lower-level modules**, on which the network module depends, for example `ConfidentialData` of the
  security module in `ConfidentialMessage`. The network class refers to the class directly. Such a
  class implements `Canonical` itself, and the network class encodes it with a `canonical` field, so
  that it receives the encoding purpose like any other nested object.
- **Higher-level modules**, which depend on the network module, for example offers, user profiles, and
  chat messages. The network module knows such data only through interfaces (`DistributedData`,
  `AppendOnlyData`) and holds it in a `google.protobuf.Any`. Canonical encoding is not yet supported for
  `DistributedData`. `AppendOnlyData` supports it (see [Append-only data](#append-only-data)).

The network class keeps the `google.protobuf.Any` of such data with the received bytes, and writes it
unchanged (see
[`../network/distributed-data-payload-bytes.md`](../network/distributed-data-payload-bytes.md)). It
encodes the data per purpose:

- `DEFAULT` and `SIGNATURE`: the kept `Any`, in a `compose` field with `DistributedDataAny.SCHEMA`: the
  type URL as field 1 and the value bytes as field 2, unchanged. These are the bytes that `serialize()`
  writes. They contain the fields annotated with `@ExcludeForHash` and the fields that the node does not
  know. A signature over them therefore covers every field of the data, and a node that does not know a
  field computes the same bytes as the signer. The type URL keeps the type of the data in the preimage.
- `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`: the protobuf serialization for hashes, in an
  `encodedMessage` field, with a comment that canonical encoding is not yet supported: the
  `google.protobuf.Any` that `toAny(true)` returns. It contains the type URL and the
  `serializeForHash()` bytes, exactly as the protobuf hash serialization of the holder contains it.
  Nodes of all versions must compute the same storage key, proof of work payload, and contract hash.

Both encodings are implemented once where the data is held: in the base class `AuthenticatedData` for
`DistributedData` (for `DefaultAuthenticatedData` and `AuthorizedData`). `AddAppendOnlyDataRequest`
uses the first one for `AppendOnlyData`, and the canonical encoding of the data for the hash purposes.

The protobuf serialization for hashes is only as deterministic as the protobuf serialization of the
external class. The fallback lives in the network class that holds the external data, not in the
external classes: an external class must not appear to have a canonical encoding while it returns
protobuf bytes, and a future canonical encoding of an external class changes the bytes of its holders,
which must be an explicit change of the holder.

### Append-only data

`AppendOnlyData` extends `Canonical`. Every implementation defines the canonical encoding of its bare
message, without the `google.protobuf.Any` in which `AddAppendOnlyDataRequest` carries it:

- `DEFAULT` and `SIGNATURE`: all non-transient fields. The bytes equal `serialize()`.
- `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`: without the fields annotated with
  `@ExcludeForHash`. The bytes equal `serializeForHash()`.

`AddAppendOnlyDataRequest` uses it as follows:

- `DEFAULT` and `SIGNATURE`: the kept `Any`, as for all data of higher-level modules.
- `PROOF_OF_WORK` and `TRADE_CONTRACT`: an `Any` with the kept type URL and the encoding of the data
  for the same purpose as value. The kept type URL is the one that `Any.pack` creates, so the bytes
  equal the `Any` of `toAny(true)`, and nodes of all versions compute the same proof of work payload.
- Storage key V2 hashes the `STORAGE_KEY` encoding of the data (see
  [`../network/storage-keys.md`](../network/storage-keys.md)). The request itself has no
  `STORAGE_KEY` encoding.

The canonical encoding of an append-only data type is part of the proof of work payload and of storage
key V2. Changing it is a protocol change.

## Encoding purpose

`Canonical.encodeCanonical(CanonicalEncoder, EncodingPurpose)` receives an `EncodingPurpose`. The
purpose states what the preimage is used for. It lets a class select a different encoding for a
different purpose.

| Purpose | Preimage |
| --- | --- |
| `DEFAULT` | The complete encoding of an object, with all its encoded fields. Used when no specific purpose applies. |
| `SIGNATURE` | The bytes that are signed and verified: the preimage of signature V2 of the storage requests. Signature V1 does not use it (see [Byte parity when a call site changes](#byte-parity-when-a-call-site-changes)). |
| `STORAGE_KEY` | The bytes of the stored data (`AuthenticatedData`, `MailboxData`, `AppendOnlyData`) that are hashed to the data hash of storage key V2. The requests and the sequential data have no `STORAGE_KEY` encoding. |
| `PROOF_OF_WORK` | The bytes of a network message to which the proof of work of the message's authorization token is bound, for example an `AddAuthenticatedDataRequest` with its `EnvelopePayloadMessage` envelope. The proof of work does not use it yet (see [Byte parity when a call site changes](#byte-parity-when-a-call-site-changes)). |
| `TRADE_CONTRACT` | The bytes of a trade contract that are hashed to the contract hash, which the traders sign. The contract hash does not use it yet (see [Byte parity when a call site changes](#byte-parity-when-a-call-site-changes)). |

`SIGNATURE`, `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` are the hash purposes: their
preimages are hashed or signed. Nodes compute storage key V1, the preimages of signature V1, the proof
of work payloads, and the contract hashes with `serializeForHash()`. The data hash of storage key V2
uses the `STORAGE_KEY` purpose.

Rules:

- The purpose applies to the whole preimage: to the object on which `encodeCanonical` is called and to
  every object nested in it.
  - `CanonicalEncoder.encode(value, schema, purpose)` requires the purpose and passes it to every
    nested schema (`compose`, `extend`, `oneof`, `repeatedCompose`, map values).
  - For a `canonical` field, the encoder calls `encodeCanonical(encoder, purpose)` of the value with the
    purpose of the enclosing object. The nested object selects its own encoding for the purpose.
  - An implementation passes the purpose that it receives to the encoder. An implementation that
    encodes a nested `Canonical` object itself passes the purpose on.
- A field that holds another `Canonical` object is a `canonical` field, so that the nested object
  selects its encoding for the purpose. A `compose` field uses the nested schema that the parent
  references, for every purpose. It is used for a nested message that has no own `encodeCanonical`, for
  example the bare message of an envelope class (see
  [`canonical-model-mapping.md`](canonical-model-mapping.md#envelope-messages)). Each purpose-specific
  schema of the parent must then reference the nested schema for the same purpose.
- An implementation selects its schema with a `switch` over all purposes, without a `default` branch.
  A new purpose then does not compile until every class has decided its encoding for it. A class
  without a specific encoding for a purpose uses its default schema.
- Fields annotated with `@ExcludeForHash` are left out of the `STORAGE_KEY`, `PROOF_OF_WORK`, and
  `TRADE_CONTRACT` schemas, as `serializeForHash()` leaves them out. The `DEFAULT` schema contains them.
  A comment at the schema names the left-out fields and their annotation. An annotation with
  `excludeOnlyInVersions` is reproduced with the same version condition, based on the version that
  `getVersion()` of the object returns. With these schemas, the `STORAGE_KEY` purpose reproduces the
  existing storage keys.
- The `SIGNATURE` purpose is the preimage of signature V2, which has no legacy bytes. Its schemas
  contain the fields annotated with `@ExcludeForHash`. A `SIGNATURE_V2_SCHEMA` may leave out a field
  only for a stated reason, named in a comment at the schema, and never the signature that the preimage
  is signed for.
- Exception: data of higher-level modules. Its fields cannot be left out of the kept bytes without
  decoding and encoding the data again, which loses the fields that the node does not know. The
  `SIGNATURE` purpose contains the kept bytes completely; the `STORAGE_KEY`, `PROOF_OF_WORK`, and
  `TRADE_CONTRACT` purposes use the hash serialization (see
  [Data of other modules](#data-of-other-modules)).
- The encoding of an object for a purpose applies wherever the object appears in a preimage for that
  purpose, also as a nested object. It may therefore leave out only data that no preimage of that
  purpose must protect. In particular, it must not leave out the object's own signature if an enclosing
  preimage of the same purpose must cover that signature.
- An implementation must select its encoding only from the purpose and from the encoded state of the
  object, not from external state such as configuration, time, or the role of the node.

Rationale:

- A purpose applies to the whole preimage, and the data that a purpose leaves out can be at any depth.
  The existing hash serialization works this way: `toProto(serializeForHash)` passes the flag to the
  `toProto` calls of nested objects, and each level clears its own `@ExcludeForHash` fields. For
  example, the preimage of signature V1 of `AddMailboxRequest` (`MailboxSequentialData.serializeForHash()`)
  does not contain the `version` of the nested `MailboxData`. A canonical encoding can reproduce such a
  preimage only if the nested objects receive the purpose.
- If the purpose applied only to the root object, a parent would have to select the encoding of each
  nested object itself. For a polymorphic field, this needs one `instanceof`-based alternative per
  implementation, which the framework cannot check for completeness (see
  [`canonical-model-mapping.md`](canonical-model-mapping.md#polymorphic-fields)).
- Because the purpose is passed on, a purpose-specific encoding of a nested object is part of every
  enclosing preimage of that purpose. If such an encoding left out the nested object's own signature,
  an enclosing signature of the same purpose would no longer cover it, and a relay could replace it
  with another valid signature without invalidating the enclosing signature. The rule above excludes
  such encodings. In the network storage classes, a signature covers a separate object or a hash (for
  example `AuthenticatedSequentialData` for `AddAuthenticatedDataRequest`, and the `DistributedData`
  for `AuthorizedData`), never the signing object without its signature field.

Security note on `@ExcludeForHash`: a field that a hash purpose leaves out is not protected by a hash
or signature of that purpose. The annotation keeps hashes compatible with nodes that do not know a
field, and the `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` purposes reproduce it only for
byte parity. In `MailboxData`, `RemoveAuthenticatedDataRequest`, and `RemoveMailboxRequest`, signature
V1 protects neither `version` nor the deprecated `metaData`; no hash contains `metaData` in any version.
Signature V2 covers `version`. The proof of work payload does not bind the `@ExcludeForHash`
fields either: a proof of work stays valid when they change. The receiver computes the expected cost
from the message that it received ([`../network/message-cost.md`](../network/message-cost.md)), so a
change does not lower the cost that the receiver expects. The contract hash does not bind the
`@ExcludeForHash` fields of the contract's classes either, for example `version`,
`tradeProtocolVersion`, and `appVersion` of `Offer` (see [Unresolved
questions](#unresolved-questions)). A preimage without legacy bytes must not repeat such exclusions
without a reason.

Current state: `MailboxData`, `AddAuthenticatedDataRequest`, `RemoveAuthenticatedDataRequest`,
`AddMailboxRequest`, and `RemoveMailboxRequest` use a `SIGNATURE_V2_SCHEMA` for `SIGNATURE`, and a
`PROOF_OF_WORK_SCHEMA` and a `TRADE_CONTRACT_SCHEMA` without their `@ExcludeForHash` fields;
`MailboxData` also has a `STORAGE_KEY_SCHEMA` without them. `RefreshAuthenticatedDataRequest` has no
preimage of its own; it uses `DEFAULT_SCHEMA` for `DEFAULT` and `SIGNATURE`, and a
`PROOF_OF_WORK_SCHEMA` and a `TRADE_CONTRACT_SCHEMA`. The requests have no `STORAGE_KEY` encoding; a
call with this purpose fails. `DefaultAuthenticatedData` and `AuthorizedData` use `DEFAULT_SCHEMA` for
`DEFAULT` and `SIGNATURE`, and a `STORAGE_KEY_SCHEMA`, a `PROOF_OF_WORK_SCHEMA`, and a
`TRADE_CONTRACT_SCHEMA` with the hash serialization of the data of the higher-level module.
`AddAppendOnlyDataRequest` uses `DEFAULT_SCHEMA` for `DEFAULT` and `SIGNATURE`, and a
`PROOF_OF_WORK_SCHEMA` and a `TRADE_CONTRACT_SCHEMA` with the canonical encoding of the
`AppendOnlyData` for the same purpose. All other implementations use `DEFAULT_SCHEMA` for all purposes,
except that `AuthenticatedSequentialData` and `MailboxSequentialData` have no `STORAGE_KEY` encoding; a
call with this purpose fails. Every implementation passes the purpose on.

## Schema metadata without effect on the bytes

- The schema version (`CanonicalSchema.newBuilder(int)`, `Builder.version(int)`, default
  `CanonicalSchema.CURRENT_VERSION`) is not encoded, and no code selects behavior by it. Changing it
  does not change any byte. A version that must affect a preimage must be an encoded field.
- The wrapper message name of `CanonicalSchema.oneof(...)` is informational.
- `CanonicalSchema.Rule` (`OMIT_DEFAULT`, `OMIT_EMPTY`, `OMIT_NULL`, `LIST_ORDER`, `MAP_ORDER`) is
  derived from the field type and describes the behavior in the table above. The encoder does not
  read it, and it cannot be configured.

## Encoded-byte caches

A source map may implement `CanonicalMapEntryByteCache` so that the encoder can reuse bytes that it
encoded before. Bisq 2 has no implementation. A cache must not change the bytes:

- With a cache, the encoder must produce exactly the bytes that it produces without the cache.
- The encoder uses a cache only for the `DEFAULT` purpose. Cached bytes do not record the purpose that
  produced them, and a map value can contain nested objects with a purpose-specific encoding.
- A cached complete map field is valid only for the schema field instance that produced it (identity,
  not equality) and only while the map content is unchanged.
- A cached map entry is valid only for the same canonical key and the same value instance. The
  lookup of a cached entry does not include the schema field. One map instance must therefore only be
  encoded by map fields with the same entry encoding.
- The encoded fields of a cached value must not change while the value instance is in the cache.
- Encoding a cache-enabled map changes the cache. The map must be confined to one thread or protected
  by external synchronization.

## Error behavior

The encoder throws an exception, and produces no bytes, when:

- a list, a map, a list element, a map key, or a map value is `null`;
- the sorted or the legacy map order finds a duplicate canonical key.

The schema builder throws an exception, usually during class initialization, when a field number is
outside the allowed range, the field numbers are not ascending, or a nested schema or map encoding
is missing or not allowed for the field type.

For a `null` singular value the encoder writes no record, while the protobuf-java builders throw an
exception for `null`. The encoder therefore accepts some object states that the protobuf serialization
rejects. Where an implementation must reject such an object, it checks the value explicitly in the
getter (for example with `checkNotNull`).

## Changing an encoding

Canonical bytes become part of the protocol as soon as a call site uses them.

- A change that alters the bytes of a preimage that nodes compare (field number, field type, field
  order, omission rule, map order, schema selection, the external-data fallback) is a protocol change.
  It requires an explicit activation and a plan for data created before the activation. It must not
  be made silently.
- A purpose-specific schema of a class changes the bytes of every preimage of that purpose that
  contains the class, also as a nested object. For each such preimage that a call site uses, it is a
  protocol change.
- A protobuf field that is part of a canonical class must be declared in the schema in the same change
  that adds the field. Otherwise the field is not protected, and the identity with `serialize()` breaks.
- A new implementation of a type that is encoded with a `canonical` field only needs its own
  `encodeCanonical`; the compiler enforces it when the type extends `Canonical`.
- Canonical encoding does not change how protobuf handles new fields of the network classes between
  versions. A node that does not know a new field of a network class drops it and computes a different
  preimage. Data of higher-level modules is different: nodes keep its received bytes, and the
  `SIGNATURE` purpose contains fields that the node does not know. Nodes released before that rule drop
  them (see [`../network/distributed-data-payload-bytes.md`](../network/distributed-data-payload-bytes.md)).

## Verification

- A schema of a class with a protobuf message must have tests that compare the canonical bytes with
  `serialize()`, including absent and default values of fields that can be absent, and every
  implementation of a `canonical` field.
- A class that encodes external data must have a test with external data that has a field annotated
  with `@ExcludeForHash`, to show that the `STORAGE_KEY` purpose encodes the external data with its hash
  serialization and the `DEFAULT` and `SIGNATURE` purposes with its exact bytes. It must have a test with
  external data that contains a field which the node does not know.
- Differences to `serializeForHash()` must be asserted by tests, so that a change of either side is
  noticed.
- Tests must show the bytes of every purpose: the default encoding equals `serialize()`, and the
  `STORAGE_KEY` encodings of the stored data equal `serializeForHash()`, for versions inside and outside
  every `excludeOnlyInVersions` list. The `SIGNATURE` encodings equal `serializeForHash()` for the classes
  without signature V2 and without external data. Documented differences (`AuthorizedData`, and the
  `SIGNATURE` encoding of external data) are asserted. The preimages of signature V2 have tests with
  a fixed layout.
- Tests must show that the purpose reaches nested objects: in the framework for the `canonical` field
  and for every field type with a nested schema, and in the model for a chain of nested classes, with a
  nested object that records the purposes it receives.
- A new writer feature must have byte-level tests with fixed expected bytes, including boundary
  values such as `0`, `127`, `128`, negative values, and maximum values.
- An implementation that encodes a nested `Canonical` object itself must have a test that the nested
  object receives every purpose of the parent.
- A preimage without legacy bytes must have tests with fixed expected bytes.

## Supported subset and limitations

The canonical encoder is not a general protobuf encoder. It supports only the protobuf features that
Bisq's preimages use, because every supported feature needs its own rules and tests to be
deterministic and compatible.

Not supported:

- the field types `float`, `uint64`, `fixed32`, `fixed64`, `sfixed32`, and `sfixed64`;
- repeated fields of `bytes`, `int64`, `sint32`, `sint64`, `uint32`, `bool`, enum, and `double`,
  packed encoding for any element type other than `int32`, and unpacked encoding of repeated numeric
  fields;
- map keys other than `string`, map values other than `string` and message, and `sint32`, `sint64`,
  `canonical`, and `encodedMessage` as map values;
- explicit presence (proto3 `optional`, wrapper types), groups, extensions, `Any` as a typed field
  (only as the kept `Any` of data of higher-level modules, with `DistributedDataAny.SCHEMA`, or as
  pre-encoded bytes), and unknown fields, except as opaque content of the value of a kept `Any`;
- decoding. The framework only produces bytes; it does not parse or validate canonical bytes.

Further limitations:

- The identity with `serialize()` holds for protobuf-java's default serialization of Bisq's messages.
- Data of higher-level modules is encoded with its kept bytes, and with protobuf serialization for the
  `STORAGE_KEY` purpose (see [Data of other modules](#data-of-other-modules)).
- The encoder checks values only through the Java type of the getter.
- The encoder does not check that exactly one alternative of a protobuf `oneof` is present; see
  [`canonical-model-mapping.md`](canonical-model-mapping.md#polymorphic-fields).
- A `compose`, `repeatedCompose`, or map value field uses the nested schema that the parent references
  for every purpose. There is no repeated or map variant of the `canonical` field, so a
  purpose-specific schema of the parent must reference the nested schema for the same purpose; see
  [`canonical-model-mapping.md`](canonical-model-mapping.md#composition).
- Determinism depends on the model: the order of lists is defined by the model object.

Adding a missing feature requires a writer method with byte-level tests, a builder method, tests for
the first schema that uses the feature, and an update of this specification.

## Unresolved questions

- **Purpose-specific schemas for the existing call sites.** Before a storage key, signature, proof of
  work payload, contract hash, or inventory order uses canonical encoding, it needs a purpose whose
  encoding reproduces the bytes of `serializeForHash()`, or an activation of new bytes. The
  `STORAGE_KEY` purpose reproduces `serializeForHash()` for the stored data classes, and the
  `PROOF_OF_WORK` purpose reproduces it for every message. Because the purpose is passed to nested
  objects, the schema of a class for a purpose applies at every depth. The `STORAGE_KEY` schema of
  `AuthorizedData` leaves out the signature, which is correct only while `STORAGE_KEY` is used for the
  data hash of storage keys alone, where `AuthorizedData` is the root. Where `AuthorizedData` is nested
  (the proof of work payload, the contract hash), `toProto(true)` contains the signature, and so do its
  `PROOF_OF_WORK` and `TRADE_CONTRACT` schemas. The inventory order has no purpose yet.
- **Canonical encoding of data of higher-level modules.** Signatures do not need it: the `SIGNATURE`
  purpose contains the kept bytes. `AppendOnlyData` supports it. For `DistributedData`, the
  `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` purposes still use the protobuf serialization for
  hashes. Replacing it changes storage keys, proof of work payloads, and contract hashes, unless the
  canonical bytes reproduce the protobuf hash serialization.
- **Contract classes.** The contract module and the classes that a contract contains (for example
  `Offer`, `Party`, `NetworkId`, and the payment method specifications) have no canonical encoding.
  Before the contract hash can use the `TRADE_CONTRACT` purpose, they need schemas whose
  `TRADE_CONTRACT` encoding equals `serializeForHash()`, and parity tests with the contracts of all
  trade protocols.
- **Fields that the contract hash does not bind.** The contract hash leaves out the fields annotated
  with `@ExcludeForHash` of the contract's classes, for example `version`, `tradeProtocolVersion`, and
  `appVersion` of `Offer`. The traders' signatures do not protect them. A contract hash that covers
  them has no legacy bytes and needs an activation; the `TRADE_CONTRACT` purpose reproduces the
  existing contract hash and cannot cover them. Whether any trade decision depends on these fields,
  and whether a new contract hash is needed, needs a decision by the developers.
- **Explicit presence.** `optional` fields whose present value can be the default need framework
  support.
- **Enforcement of `oneof` alternatives.** The framework cannot express that a group of fields is a
  protobuf `oneof`. The network storage classes avoid instanceof-based alternatives by using the
  `canonical` field.

## Related documents and code

- [`canonical-model-mapping.md`](canonical-model-mapping.md): mapping of composition, inheritance,
  polymorphic fields, envelope messages, and data of other modules.
- `../../../common/src/main/java/bisq/common/encoding/canonical/`: the framework.
- `../../../network/network/src/main/java/bisq/network/p2p/services/data/storage/`: the storage classes
  with canonical encodings, and `StorageCanonicalEncoderTest` in the test sources.
- `../../dev/protobuf-notes.md`: protobuf conventions of Bisq 2.
- `../../../.agents/skills/canonical-encoding/SKILL.md`: instructions for AI agents that write schemas.
- Bisq 1 documents the design rationale and the analysis of protobuf malleability in
  `docs/canonical-encoding/` of the Bisq 1 repository.
