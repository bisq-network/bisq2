# Canonical encoding of model structures

## Scope

This specification defines how the structure of a Java model maps to canonical messages in Bisq 2:
composition, lists and maps of objects, inheritance, polymorphic fields, envelope messages, data of
other modules, and delegation. The wire rules, the required properties, and the change rules are
defined in [`canonical-encoding.md`](canonical-encoding.md).

## Principle

The canonical message of an object mirrors the protobuf message that `serialize()` writes for that
object (`toProto(false)`), field by field, including the envelopes that `toProto` adds. The Java class
structure matters only through the way the protobuf messages model it. When the Java structure and the
protobuf structure differ, the protobuf structure decides.

For each field of the protobuf message, the canonical field contains the value that the protobuf
builder receives, converted in the same way. A Java field that is `transient` is not part of the
canonical message. A field annotated with `@ExcludeForHash` is part of it: the canonical default
encoding contains all non-transient fields (see
[`canonical-encoding.md`](canonical-encoding.md#complete-encoding)). The `STORAGE_KEY`,
`PROOF_OF_WORK`, and `TRADE_CONTRACT` schemas leave such a field out, as `serializeForHash()` does;
the `SIGNATURE` schemas contain it (see
[`canonical-encoding.md`](canonical-encoding.md#encoding-purpose)).

## Composition

- A field that holds another `Canonical` model object is a nested message whose bytes the nested object
  produces itself, for the encoding purpose of the parent (`canonical`). The nested object selects its
  own encoding for the purpose. The parent schema must not repeat the fields of the nested object. When
  the object is absent (`null`), the field has no record. Example: `MailboxSequentialData.mailboxData`.
- A nested message without an own `encodeCanonical` uses the nested schema directly (`compose`), for
  example the bare message of an envelope class (see [Envelope messages](#envelope-messages)). This
  schema is the same for every purpose, so each purpose-specific schema of the parent must reference
  the nested schema for the same purpose.
- A list of model objects is a repeated nested message (`repeatedCompose`). The elements are encoded
  in list order, with the element schema that the parent references. The framework has no repeated
  `canonical` field, so the same rule as for `compose` applies to purpose-specific schemas.
- A map with model objects as values is a map field with message values (`mapStringToCompose`), with
  the value schema that the parent references. The entry order follows the rules for map fields in
  [`canonical-encoding.md`](canonical-encoding.md#map-fields).
- The nested message must be the protobuf message type of the field. If the encoding of the nested
  class contains an envelope message (see [Envelope messages](#envelope-messages)), the parent needs a
  schema of the bare message instead.
- If the declared type of the field has several implementations, see
  [Polymorphic fields](#polymorphic-fields).

## Inheritance

Protobuf messages model Java inheritance in two ways. The canonical message follows the way that the
protobuf message uses.

### Flat inheritance

The protobuf message contains the base fields and the subclass fields in one message. The canonical
message of a subclass object contains both in one message, in ascending field-number order. The base
class defines its fields once, in a static builder method; every subclass continues the builder with
its own fields.

### Subtype message inside the base message

The protobuf base message contains the base fields and a `oneof` whose alternatives are one message
per subclass. Example: `AuthenticatedData` with the field `distributedData = 1` and the alternatives
`DefaultAuthenticatedData = 10` and `AuthorizedData = 11`.

The canonical message of a subclass object contains, in ascending field-number order:

1. the base fields;
2. one nested message at the field number of the object's alternative, with the fields of the
   subclass. If the subclass has no own fields, the nested message is empty and the record is the tag
   followed by the length `0` (`DefaultAuthenticatedData`);
3. base fields whose numbers are higher than the alternative.

The base class provides the base fields in a static builder method
(`AuthenticatedData.getAuthenticatedDataSchemaBuilder()`), and each subclass adds its alternative with
`extend` and an identity getter.

## Polymorphic fields

A polymorphic field is a field whose declared type has several implementations, for example
`AuthenticatedSequentialData.authenticatedData` of the abstract type `AuthenticatedData`.

In Bisq 2, such a field uses the `canonical` field: the encoder calls the value's own
`encodeCanonical` with the purpose of the parent, so the runtime type selects the encoding. When the
declared type extends `Canonical` and is abstract or an interface, the compiler requires every
implementation to define its encoding. No implementation can be left out, and no object can match two
alternatives.

Bisq 1 also uses schemas with one `instanceof`-based alternative per subtype. Such a schema must fulfill
these requirements, which the framework does not enforce:

- For every object, exactly one alternative of each protobuf `oneof` must produce a record.
- Every subtype must be registered in every schema that encodes the base type.
- The selection of an alternative must be exclusive, also when a Java subclass relation does not exist
  in protobuf.

A missing registration drops the fields of the subtype from the preimage without an error. Prefer the
`canonical` field in Bisq 2.

## Envelope messages

For some classes, `toProto` returns an envelope message, and `serialize()` and `serializeForHash()`
contain the envelope:

- The data requests are wrapped twice: in the field `dataRequest = 12` of `EnvelopePayloadMessage`, and
  in it in the field of the request type in `DataRequest` (`AddAuthenticatedDataRequest = 1`,
  `RemoveAuthenticatedDataRequest = 2`, `RefreshAuthenticatedDataRequest = 3`, `AddMailboxRequest = 4`,
  `RemoveMailboxRequest = 5`, `AddAppendOnlyDataRequest = 6`). `DataRequest.createCanonicalSchema`
  adds both envelopes.
- `ConfidentialMessage` is wrapped in the field `confidentialMessage = 8` of `EnvelopePayloadMessage`.

The canonical default encoding of such a class contains the same envelopes. A parent that contains
such an object as a bare message uses a schema of the bare message with `compose`: `MailboxData`
contains the bare `ConfidentialMessage` and uses `ConfidentialMessage.DEFAULT_VALUE_SCHEMA` in its
default schema. Because `encodeCanonical` of the envelope class writes the envelope, the purpose does
not select the bare schema; each purpose-specific schema of `MailboxData` must reference the bare schema
of `ConfidentialMessage` for the same purpose.

## Data of other modules

A class of a lower-level module that a network class contains directly implements `Canonical`, and the
network class encodes it with a `canonical` field: `ConfidentialMessage.confidentialData`
(`ConfidentialData` of the security module).

A network class that contains data of a higher-level module in a `google.protobuf.Any` keeps the
received `Any` and encodes it per purpose. The `DEFAULT` and `SIGNATURE` purposes contain the kept
`Any` in a `compose` field with `DistributedDataAny.SCHEMA`: the type URL and the exact value bytes.
For the other purposes, the two kinds of data differ:

- `DistributedData` has no canonical encoding yet. The `STORAGE_KEY`, `PROOF_OF_WORK`, and
  `TRADE_CONTRACT` purposes contain the protobuf serialization for hashes in an `encodedMessage` field:
  the `Any` that `toAny(true)` returns, with the type URL and the `serializeForHash()` bytes.
- `AppendOnlyData` extends `Canonical`. The `PROOF_OF_WORK` and `TRADE_CONTRACT` purposes contain an
  `Any` with the kept type URL and the encoding of the data for the same purpose. The holder has no
  `STORAGE_KEY` encoding: storage key V2 hashes the `STORAGE_KEY` encoding of the data itself.

The encodings are implemented once where the data is held: in the base class `AuthenticatedData` for
`DistributedData`, and in `AddAppendOnlyDataRequest` for `AppendOnlyData`. The rules and the rationale
are in [`canonical-encoding.md`](canonical-encoding.md#data-of-other-modules) and in
[`../network/distributed-data-payload-bytes.md`](../network/distributed-data-payload-bytes.md).

## Delegation

Delegation means that the nested bytes come from the value's own `encodeCanonical`. In Bisq 2, a
schema expresses it with the `canonical` field. A class may also write its records by hand with
`CanonicalWriter`; it is then responsible for the field order and the presence rules of its own fields,
because no schema checks them.

In both cases the parent passes its encoding purpose to the nested object (see
[`canonical-encoding.md`](canonical-encoding.md#encoding-purpose)).

## Values that need conversion

- Bisq 2 protobuf messages use `sint32` and `sint64` for most integers. The canonical field must use the
  same type (`sint32`, `sint64`); `int32` and `int64` produce different bytes, also for positive values.
- A protobuf enum field is encoded with the code of the Java enum, which implements `CanonicalEnum`.
  The code is the number of the protobuf enum constant. The protobuf default constant has code `0`,
  which produces no record.
- An `Optional` Java field (proto3 `optional`) returns the value or `null`
  (`signature.orElse(null)`). This is correct only when a present value is never the protobuf default
  (see [`canonical-encoding.md`](canonical-encoding.md#presence-of-values)).

## Absent values

The getter of a canonical field returns the value that the protobuf builder receives:

- An optional singular value that the builder does not receive when it is absent returns `null` or
  the default value. The field then has no record.
- A value that the builder requires must make the getter fail when it is absent, for example with
  `checkNotNull`, so that the canonical path does not accept an object that the protobuf path rejects.
- An absent list or map that the builder skips returns an empty list or an empty map. A `null` list or
  map makes the encoder fail.

## Related documents and code

- [`canonical-encoding.md`](canonical-encoding.md): wire rules, required properties, change rules.
- `../../../network/network/src/main/java/bisq/network/p2p/services/data/storage/auth/AuthenticatedData.java`:
  subtype message inside the base message, and the fallback for data of other modules.
- `../../../network/network/src/main/java/bisq/network/p2p/services/data/storage/auth/AuthenticatedSequentialData.java`:
  polymorphic field with the `canonical` field.
- `../../../network/network/src/main/java/bisq/network/p2p/services/data/DataRequest.java`: envelopes of
  the data requests.
- `../../../network/network/src/main/java/bisq/network/p2p/services/confidential/ConfidentialMessage.java`:
  schema with and without envelope.
