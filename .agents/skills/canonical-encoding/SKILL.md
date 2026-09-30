---
name: canonical-encoding
description: Create, change, or review canonical encodings in Bisq 2 - CanonicalSchema definitions and Canonical.encodeCanonical implementations (package bisq.common.encoding.canonical). Use this skill whenever a Bisq model class must produce deterministic bytes for a hash, a signature, a storage key, a proof of work payload, or a trade contract hash; when a class implements Canonical or defines a DEFAULT_SCHEMA; when adding a field, a subclass, or a DataRequest type to the network storage classes; when replacing serializeForHash() in a hash or signature path; or when canonical bytes must be compared with protobuf output. Covers composition, inheritance, polymorphic fields (canonical field), envelopes (EnvelopePayloadMessage, DataRequest), data of other modules (canonical for lower-level modules, kept Any bytes and the encodedMessage storage-key fallback for higher-level modules), sint32/sint64, encoding purposes, and parity tests.
---

# Canonical encoding in Bisq 2

Canonical encoding produces deterministic bytes for hash and signature preimages. The authoritative
rules are in the specifications; read them before non-trivial work:

- `docs/specifications/encoding/canonical-encoding.md`: required properties, wire rules, data of other
  modules, encoding purpose, supported subset, change rules.
- `docs/specifications/encoding/canonical-model-mapping.md`: composition, inheritance, polymorphic
  fields, envelope messages, delegation.

## The rules you must not break

1. **Determinism.** The same object state produces the same bytes on every node. The bytes must not
   depend on the protobuf library, on Java map iteration, or on anything except the encoded values.
2. **Complete encoding.** The default encoding contains all non-transient fields, with the field
   numbers and types of the protobuf message and the envelopes that `serialize()` writes. For data of
   the class's own module, the canonical bytes equal `serialize()` (`toProto(false).toByteArray()`).
   Fields annotated with `@ExcludeForHash` are included in `DEFAULT_SCHEMA` and in the
   `SIGNATURE_V2_SCHEMA` of the storage requests; `STORAGE_KEY_SCHEMA`, `PROOF_OF_WORK_SCHEMA`, and
   `TRADE_CONTRACT_SCHEMA` leave them out, as `serializeForHash()` does.
3. **No silent change of hashed bytes.** The production call sites of canonical encoding are signature
   V2 of the storage requests (`docs/specifications/network/storage-request-signatures.md`) and the data
   hash of storage key V2 (`docs/specifications/network/storage-keys.md`). Storage key V1, signature V1,
   proof of work payloads, trade contract hashes, and the inventory order use `serializeForHash()`.
   Switching a call site is a protocol change: the bytes must be identical to what older nodes compute,
   or the developers must plan an activation. Do not switch a call site on your own.

The framework is not a general protobuf encoder. It supports only what Bisq's preimages use. If a field
needs a missing feature (for example `uint64`, `float`, repeated `int64`, maps with non-string keys, or
explicit presence for an `optional` field whose present value can be the default), stop and report it;
do not work around it.

## The parts of the framework

- `Canonical`: implemented by model classes. Implement the one abstract method
  `encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose)`: select the
  schema for the purpose with a `switch`, and pass the purpose to the encoder. The default overloads
  `encodeCanonical()`, `encodeCanonical(CanonicalEncoder)`, and `encodeCanonical(EncodingPurpose)` fill
  in the defaults for callers; do not override them.
- `CanonicalSchema<T>`: an ordered list of fields, built with `CanonicalSchema.<T>newBuilder()`. Every
  builder method takes the protobuf field number and a getter. Bisq 2 adds `sint32`, `sint64`,
  `canonical` (nested `Canonical` object that encodes itself for the purpose of the parent), and
  `encodedMessage` (nested message from bytes produced outside the framework).
- `canonicalEncoder.encode(object, schema, encodingPurpose)`: executes a schema. The purpose is
  required; the encoder passes it to every nested schema and to every `canonical` field.
- `EncodingPurpose`: `DEFAULT` (the complete encoding), `SIGNATURE` (the bytes that are signed and
  verified), `STORAGE_KEY` (the bytes that are hashed to a storage key), `PROOF_OF_WORK` (the bytes of
  a network message to which the proof of work of its authorization token is bound), and
  `TRADE_CONTRACT` (the bytes of a trade contract that are hashed to the contract hash, which the
  traders sign). It is the purpose of the whole preimage: it applies to the object on which
  `encodeCanonical` is called and to every object nested in it. Classes with `@ExcludeForHash` fields
  use hash schemas without them for `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`. The signed
  storage requests and `MailboxData` use a `SIGNATURE_V2_SCHEMA` for `SIGNATURE`. All other classes
  use `DEFAULT_SCHEMA` for every purpose without an own schema, except the holders of data of
  higher-level modules (see the pattern table).
- `CanonicalWriter`: low-level wire writer; use schemas instead wherever possible.
- `CanonicalEnum`: implemented by Java enums that are protobuf enum fields.

A typical class (compare `MetaData` in `network/network/.../data/storage/MetaData.java`):

```java
    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // network.proto: message MetaData { sint64 ttl = 1; sint32 priority = 2; sint32 maxMapSize = 3; string className = 4; }
    public static final CanonicalSchema<MetaData> DEFAULT_SCHEMA = CanonicalSchema.<MetaData>newBuilder()
            .sint64(1, MetaData::getTtl)
            .sint32(2, MetaData::getPriority)
            .sint32(3, MetaData::getMaxMapSize)
            .string(4, MetaData::getClassName)
            .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<MetaData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }
```

The class also adds `Canonical` to its `implements` clause. The protobuf comment is for this
explanation only; the classes do not repeat the protobuf definition. The protobuf field numbers decide
the order of a schema: the builder rejects fields that are not in ascending field-number order, and the
bytes must follow protobuf. Declare the Java fields of a canonical class in the same order, so that
class, schema, and protobuf message read the same (`transient` fields next to the field they belong to or
after the protobuf fields).

A schema is named after its purpose: `DEFAULT_SCHEMA`, `SIGNATURE_V2_SCHEMA` (the preimage of signature
V2), `STORAGE_KEY_SCHEMA`, `PROOF_OF_WORK_SCHEMA`, and `TRADE_CONTRACT_SCHEMA`.
The `switch` has one case per purpose and no `default` branch, so a new purpose does not compile until
every class has decided its encoding for it. A purpose without its own schema uses `DEFAULT_SCHEMA`.
Always pass the received `encodingPurpose` to the encoder, also when the selected schema is
`DEFAULT_SCHEMA`: nested objects select their own encoding for it.

When the class has fields annotated with `@ExcludeForHash`, the `STORAGE_KEY`, `PROOF_OF_WORK`, and
`TRADE_CONTRACT` purposes leave them out, as `serializeForHash()` does. Build these schemas with one
method, name the left-out fields and their annotation in a comment, and reproduce
`excludeOnlyInVersions` with the same version condition (compare `MailboxData`):

```java
    public static final CanonicalSchema<MailboxData> STORAGE_KEY_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<MailboxData> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<MailboxData> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 2) in versions
    // 1 to 3, version (field 3) and className (field 4).
    private static CanonicalSchema<MailboxData> createSchemaForHash() {
        return CanonicalSchema.<MailboxData>newBuilder()
                // ConfidentialMessage uses its default value schema for every purpose
                .compose(1, MailboxData::getConfidentialMessage, ConfidentialMessage.DEFAULT_VALUE_SCHEMA)
                // metaData is annotated with @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
                .canonical(2, mailboxData -> isMetaDataExcludedForHash(mailboxData.version) ?
                        null :
                        mailboxData.metaData)
                .build();
    }
```

The switch then selects `STORAGE_KEY_SCHEMA`, `PROOF_OF_WORK_SCHEMA`, and `TRADE_CONTRACT_SCHEMA`. A
field that these schemas leave out is not protected by the storage key, the proof of work, or the
contract hash; point every such field out to the developer. The exclusions exist only for byte parity
with older nodes.

The `SIGNATURE` purpose is the preimage of signature V2, which has no legacy bytes. A
`SIGNATURE_V2_SCHEMA` contains the `@ExcludeForHash` fields. It leaves out a field only for a stated
reason in a comment (for example the deprecated `metaData`), and never the signature it is the preimage
of. The preimage of a request starts with a domain separation tag (see `RemoveMailboxRequest`).

## Workflow: add canonical encoding to a class

1. **Read the protobuf message** in the module's `.proto` file (`<module>/src/main/proto/<module>.proto`,
   for example `network/network/src/main/proto/network.proto`): field numbers, types (`sint32`,
   `sint64`, `optional`, `google.protobuf.Any`, `oneof`), and nested messages.
2. **Read `getBuilder`, `getValueBuilder`, and `toProto`** of the class and of its base classes. Note
   the envelopes: an `EnvelopePayloadMessage` implementation writes the envelope in `getBuilder` and the
   bare message in `getValueBuilder`; a `DataRequest` is wrapped twice. Note which values are set only
   when present (`Optional`, `ifPresent`), how values are converted, and which values come from other
   modules.
3. **List the non-transient fields.** Every non-transient field is encoded in `DEFAULT_SCHEMA`,
   including `@ExcludeForHash` fields; note those for the hash schemas. `transient` fields (cached keys,
   values restored at runtime) are not. Use the fields, not getters that return runtime values: for
   example `RemoveAuthenticatedDataRequest.getMetaData()` returns a transient override, the field
   `metaData` is the encoded value.
4. **Choose the pattern** with the table below. Read
   [references/inheritance-and-composition.md](references/inheritance-and-composition.md) before you
   write a schema for a class with a superclass, subclasses, a field typed with another model class or an
   interface, data of another module, or an envelope.
5. **Write the schema** in ascending field-number order with exactly the protobuf types; see
   [references/field-types.md](references/field-types.md). For map fields see
   [references/maps.md](references/maps.md).
6. **Implement `encodeCanonical`** as shown above (a `switch` over all purposes and
   `canonicalEncoder.encode(this, schema, encodingPurpose)`), and add `Canonical` to the class (or rely
   on an interface that extends it, such as `DataRequest`).
7. **Write the tests** and run them; see [references/testing.md](references/testing.md). The canonical
   bytes must equal `serialize()`; differences to `serializeForHash()` must be asserted.
8. **Update the specification** when you add a framework feature or change a documented rule.

## Choose the pattern

| What you see in the `.proto` and in the builders | Pattern | Example in Bisq 2 |
| --- | --- | --- |
| `Foo foo = n;` (field holds a network model object that implements `Canonical`) | `canonical(n, getter)`: the nested object encodes itself for the purpose | `MailboxSequentialData.mailboxData`, `MailboxData.metaData` |
| `repeated Foo foos = n;` | `repeatedCompose(n, getter, Foo.DEFAULT_SCHEMA)`; a purpose-specific schema of the parent references the element schema for the same purpose | - |
| Base message with `oneof message { Sub sub = n; ... }` | Base builder method plus `extend(n, sub -> sub, subtype builder)` | `AuthenticatedData` with `DefaultAuthenticatedData` (10), `AuthorizedData` (11) |
| Field whose declared type has several implementations | `canonical(n, getter)`: the runtime type encodes itself | `AuthenticatedSequentialData.authenticatedData` |
| `google.protobuf.Any` with data of a higher-level module | The class that holds it keeps the received `Any` (`DistributedDataAny.retain` in `fromProto`) and writes it in `toProto(false)`. `DEFAULT` and `SIGNATURE`: `compose(n, x -> x.dataAny, DistributedDataAny.SCHEMA)`. `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`: `encodedMessage(n, x -> x.data.toAny(true).toByteArray())` with the "not yet supported" comment | `AuthenticatedData.distributedData` |
| `google.protobuf.Any` with `AppendOnlyData` | Kept `Any` as above for `DEFAULT` and `SIGNATURE`. `AppendOnlyData` extends `Canonical`: `PROOF_OF_WORK` and `TRADE_CONTRACT` are an `Any` with the kept type URL and the data's encoding for the same purpose; storage key V2 hashes the data's `STORAGE_KEY` encoding | `AddAppendOnlyDataRequest.appendOnlyData` |
| Message type of a lower-level module (for example `security.ConfidentialData`) | implement `Canonical` in that class and use `canonical(n, getter)` | `ConfidentialMessage.confidentialData` |
| `toProto` returns `EnvelopePayloadMessage` with a `DataRequest` | `DataRequest.createCanonicalSchema(requestFieldNumber, requestBuilder)` | the six storage requests |
| `toProto` returns another envelope | `CanonicalSchema.oneof("EnvelopePayloadMessage", n, builder)`, plus a bare `DEFAULT_VALUE_SCHEMA` if a parent embeds the bare message | `ConfidentialMessage` |
| Field holds the bare message of an envelope class | `compose(n, getter, Foo.DEFAULT_VALUE_SCHEMA)`; a purpose-specific schema of the parent references the bare schema for the same purpose | `MailboxData.confidentialMessage` |

`compose`, `extend`, `oneof`, `canonical`, and `encodedMessage` all write a length-delimited nested
message. The name says where the bytes come from and which role the field has.

## Rules for changing an existing encoding

- **Do not switch a hash, signature, proof of work, contract hash, or inventory call site** from
  `serializeForHash()` to canonical encoding without byte identity or an agreed activation. The
  default encodings of `AuthorizedData`, `MailboxData`, and the remove, refresh, and mailbox requests
  differ from `serializeForHash()`. `STORAGE_KEY` equals it for the stored data, including
  `AuthorizedData`, whose `STORAGE_KEY` schema omits the signature; the `DataRequest` types and the
  sequential data have no `STORAGE_KEY` encoding. `PROOF_OF_WORK` of a message (a `DataRequest` or
  `ConfidentialMessage`) equals it without exception. `TRADE_CONTRACT` of a contract must equal it; the
  contract classes have no canonical encoding yet. `SIGNATURE` equals it, except for `AuthorizedData`,
  for the holders of data of higher-level modules, whose `SIGNATURE` encoding contains the exact bytes
  of that data, and for the classes with a `SIGNATURE_V2_SCHEMA` and the classes that contain them.
- **Changing a `SIGNATURE_V2_SCHEMA`** changes the preimage of signature V2: it is a protocol change.
- **Adding a protobuf field** to a canonical class: add it to the schemas in the same change.
- **Adding or changing an `@ExcludeForHash` annotation**: update `createSchemaForHash()` (the
  `STORAGE_KEY_SCHEMA`, `PROOF_OF_WORK_SCHEMA`, and `TRADE_CONTRACT_SCHEMA`) of the class in the same
  change.
- **Adding an implementation** of a type encoded with a `canonical` field: implement `encodeCanonical`
  (the compiler requires it when the type extends `Canonical`) and add it to the parity test.
- **Adding a DataRequest type**: it must implement `encodeCanonical` (`DataRequest` extends `Canonical`);
  use `DataRequest.createCanonicalSchema` with its field number in the protobuf `DataRequest`.
- **Changing the external-data fallback** changes the bytes of all holders; treat it as a protocol change.
- **Adding a purpose-specific schema** to a class changes every preimage of that purpose that contains
  the class, also as a nested object. Treat it as a protocol change for each such preimage that a call
  site uses. The schema may leave out only data that no preimage of that purpose must protect, never a
  signature that an enclosing preimage must cover.

## Pitfalls

- **`DataRequest` name clash.** Many request classes import the protobuf class
  `bisq.network.protobuf.DataRequest`. Call the helper with the full name
  `bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(...)`.
- **`int32` and `int64` versus `sint32` and `sint64`.** Most Bisq 2 integers are `sint32` or `sint64`.
  The wrong method changes the bytes also for positive values.
- **`optional` fields.** The framework has no explicit presence. `Optional` values map to
  `value.orElse(null)`, which is only correct when a present value is never the default (`0`, `false`,
  empty).
- **Envelope or bare message.** `ConfidentialMessage.DEFAULT_SCHEMA` includes the
  `EnvelopePayloadMessage` envelope; `MailboxData` embeds the bare message and must use
  `ConfidentialMessage.DEFAULT_VALUE_SCHEMA`.
- **Data of higher-level modules** (behind `DistributedData` or `AppendOnlyData` in an `Any`). The
  network class that holds the data keeps the received `Any` and never rebuilds it from the decoded
  object, which would drop the fields that the node does not know. `DEFAULT` and `SIGNATURE` encode the
  kept `Any` with `DistributedDataAny.SCHEMA`; `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` use
  the protobuf hash serialization (the `Any` of `toAny(true)`, with the type URL). Do not add a fallback
  `encodeCanonical` to the external classes or to their interfaces. `AppendOnlyData` is the exception:
  it extends `Canonical`, and every implementation defines a real encoding of its bare message
  (`DEFAULT` equals `serialize()`; `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` equal
  `serializeForHash()`); see `docs/specifications/encoding/canonical-encoding.md`. A class of a
  lower-level module, such as `ConfidentialData`, implements `Canonical` instead. See
  `docs/specifications/network/distributed-data-payload-bytes.md`.
- **Always pass the encoding purpose on.** Call `canonicalEncoder.encode(this, schema, encodingPurpose)`
  with the received purpose, never with a fixed purpose. Encode a nested `Canonical` object with
  `canonical(n, getter)`, not with `compose(n, getter, Foo.DEFAULT_SCHEMA)`: a `compose` field uses the
  referenced schema for every purpose, so the nested object cannot select its own encoding. By hand,
  use `value.encodeCanonical(canonicalEncoder, encodingPurpose)`.
- **Getters with runtime values.** Encode fields, not getters that return transient overrides.
- **`excludeOnlyInVersions`.** The condition uses the version that `getVersion()` of the object
  returns (the `version` field of the remove requests and of `MailboxData`). Reproduce the same
  versions in the getter of the `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` schemas and test
  versions inside and outside the list.
- **Class initialization cycles.** A superclass must not reference static schemas of its subclasses in
  its static fields. Use static builder methods (`getAuthenticatedDataSchemaBuilder()`).
- **Builders are mutable.** A builder method must return a new builder on every call.
- **Unsupported types.** `uint64` is used in several Bisq 2 protobuf files but is not supported yet.

## Definition of done

- Every non-transient field is in the schema, with the protobuf field number and type.
- Classes of lower-level modules implement `Canonical`. Data of higher-level modules uses the kept `Any`
  for `DEFAULT` and `SIGNATURE`, and the documented hash-serialization fallback with the "not yet
  supported" comment for `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`.
- `encodeCanonical` selects the schema with a `switch` over all purposes and passes the received
  purpose to the encoder. Nested `Canonical` objects use `canonical` fields.
- `@ExcludeForHash` fields are left out of the `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`
  schemas, with a comment.
- Tests show that the default encoding equals `serialize()`, that external data uses its hash
  serialization for `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` and its exact bytes for
  `SIGNATURE`, that external data keeps fields which the node does not know, that differences to
  `serializeForHash()` are known, and that the hash purposes equal `serializeForHash()` where
  documented, also for versions inside and outside `excludeOnlyInVersions`.
- No hash, signature, proof of work, contract hash, or inventory call site changed its bytes without
  an agreed activation.
- The specification is updated when a framework feature or a documented rule changed.
