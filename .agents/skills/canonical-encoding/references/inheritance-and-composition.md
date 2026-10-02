# Inheritance and composition

Read this before you write or change a schema for a class that has a superclass or subclasses, a
field typed with another model class or an interface, data of another module, or an envelope. The
normative rules are in `docs/specifications/encoding/canonical-model-mapping.md`.

## Contents

1. The rule that decides everything
2. Composition: a field holds another network object
3. Subtype message: the base message has a `oneof` with one message per subclass
4. Polymorphic fields: the `canonical` field
5. Data of other modules: `canonical`, or the kept `Any` and the `encodedMessage` fallback
6. Envelope messages: `EnvelopePayloadMessage` and `DataRequest`
7. Delegation by hand
8. Static initialization and builder reuse

## 1. The rule that decides everything

The canonical message mirrors what `serialize()` writes (`toProto(false)`), including the envelopes
that `toProto` adds. The Java class structure matters only through the way the protobuf messages model
it. Always start from the `.proto` definition and from `getBuilder`/`getValueBuilder`, not from the Java
class diagram. All non-transient fields are encoded in `DEFAULT_SCHEMA`, including `@ExcludeForHash`
fields; the `STORAGE_KEY` schemas leave those out (see `SKILL.md`).

## 2. Composition: a field holds another network object

```proto
message MailboxSequentialData {
  MailboxData mailboxData = 1;
  sint32 sequenceNumber = 2;
  bytes senderPublicKeyHash = 3;
  sint64 created = 4;
  bytes receiversPubKeyBytes = 5;
  bytes receiversPubKeyHash = 6;
}
```

```java
public static final CanonicalSchema<MailboxSequentialData> DEFAULT_SCHEMA =
        CanonicalSchema.<MailboxSequentialData>newBuilder()
                .canonical(1, MailboxSequentialData::getMailboxData)
                .sint32(2, MailboxSequentialData::getSequenceNumber)
                .bytes(3, MailboxSequentialData::getSenderPublicKeyHash)
                .sint64(4, MailboxSequentialData::getCreated)
                .bytes(5, MailboxSequentialData::getReceiversPubKeyBytes)
                .bytes(6, MailboxSequentialData::getReceiversPublicKeyHash)
                .build();
```

- `MailboxData` implements `Canonical`, so the field is a `canonical` field. The encoder calls
  `mailboxData.encodeCanonical(encoder, purpose)` with the purpose of the parent, and `MailboxData`
  selects its own schema for that purpose. The parent never repeats the nested fields.
- Do not use `compose(n, getter, Foo.DEFAULT_SCHEMA)` for a `Canonical` object: a `compose` field uses
  the referenced schema for every purpose, so the nested object could not select its encoding.
- `compose` is for a nested message without an own `encodeCanonical`, for example the bare message of
  an envelope class (section 6). `compose` and `repeatedCompose` need a built schema, and each
  purpose-specific schema of the parent must reference the nested schema for the same purpose.
- The nested bytes must be exactly the protobuf message type of the field. If the nested class's
  `encodeCanonical` writes an envelope, use `compose` with its bare schema (section 6).
- The transient field `receiversPubKey` is not encoded.

## 3. Subtype message: the base message has a `oneof` with one message per subclass

```proto
message DefaultAuthenticatedData {
}
message AuthenticatedData {
  google.protobuf.Any distributedData = 1;
  oneof message {
    DefaultAuthenticatedData defaultAuthenticatedData = 10;
    AuthorizedData authorizedData = 11;
  }
}
message AuthorizedData {
  optional bytes signature = 1;
  bytes authorizedPublicKeyBytes = 2;
}
```

The base class provides the base fields in a generic static builder method. Each subclass adds its
alternative with `extend` and an identity getter:

```java
// AuthenticatedData (abstract, implements StorageData, Canonical). Two base builders, because the
// payload is encoded differently per purpose (see section 5).
protected static <T extends AuthenticatedData> CanonicalSchema.Builder<T> getAuthenticatedDataSchemaBuilder() {
    return CanonicalSchema.<T>newBuilder()
            .compose(1, authenticatedData -> authenticatedData.distributedDataAny, DistributedDataAny.SCHEMA);
}

protected static <T extends AuthenticatedData> CanonicalSchema.Builder<T> getSchemaBuilderForHash() {
    return CanonicalSchema.<T>newBuilder()
            // Canonical encoding is not yet supported for DistributedData, ... (see section 5)
            .encodedMessage(1, authenticatedData -> authenticatedData.distributedData.toAny(true).toByteArray());
}

// DefaultAuthenticatedData: the subclass message has no fields, but it is present (tag 10, length 0).
// STORAGE_KEY_SCHEMA, PROOF_OF_WORK_SCHEMA, and TRADE_CONTRACT_SCHEMA are the same with
// getSchemaBuilderForHash().
public static final CanonicalSchema<DefaultAuthenticatedData> DEFAULT_SCHEMA =
        DefaultAuthenticatedData.<DefaultAuthenticatedData>getAuthenticatedDataSchemaBuilder()
                .extend(10,
                        defaultAuthenticatedData -> defaultAuthenticatedData,
                        CanonicalSchema.newBuilder())
                .build();

// AuthorizedData: the signature is included, also in SIGNATURE and PROOF_OF_WORK. Only STORAGE_KEY omits it, as
// serializeForHash() does where AuthorizedData is the root of the preimage (the data hash of the storage key).
// The subtype builder is a method, so that each schema gets a new builder.
public static final CanonicalSchema<AuthorizedData> DEFAULT_SCHEMA =
        AuthorizedData.<AuthorizedData>getAuthenticatedDataSchemaBuilder()
                .extend(11, authorizedData -> authorizedData, getAuthorizedDataSchemaBuilder())
                .build();

private static CanonicalSchema.Builder<AuthorizedData> getAuthorizedDataSchemaBuilder() {
    return CanonicalSchema.<AuthorizedData>newBuilder()
            .bytes(1, authorizedData -> authorizedData.signature.orElse(null))
            .bytes(2, authorizedData -> authorizedData.authorizedPublicKeyBytes);
}
```

Points to check:

- `<T extends AuthenticatedData>` makes the base getters work for every subclass; the builder method
  returns a new builder on every call.
- The field numbers inside the `extend` builder are the numbers of the subtype message; they start
  again at `1`.
- Base fields and alternatives are ordered only by number. A base field numbered after an alternative
  follows the `extend` call; an alternative numbered after a base field follows that field.
- The transient field `authorizedPublicKey` is not encoded.

## 4. Polymorphic fields: the `canonical` field

When the declared type of a field has several implementations, let the runtime type encode itself:

```java
// AuthenticatedSequentialData: authenticatedData is a DefaultAuthenticatedData or an AuthorizedData
public static final CanonicalSchema<AuthenticatedSequentialData> DEFAULT_SCHEMA =
        CanonicalSchema.<AuthenticatedSequentialData>newBuilder()
                .canonical(1, AuthenticatedSequentialData::getAuthenticatedData)
                .sint32(2, AuthenticatedSequentialData::getSequenceNumber)
                .bytes(3, AuthenticatedSequentialData::getPubKeyHash)
                .sint64(4, AuthenticatedSequentialData::getCreated)
                .build();
```

- The encoder calls `value.encodeCanonical(encoder, purpose)` with the purpose of the parent and writes
  the result as the nested message.
- The declared type must extend `Canonical`. When it is abstract (`AuthenticatedData implements
  Canonical`), the compiler requires every concrete subclass to implement `encodeCanonical`, so no
  subtype can be forgotten, and no object can match two alternatives.
- Each implementation's own schema must produce the bytes of the protobuf message of the field (here:
  the complete `AuthenticatedData` message with base fields and the alternative).

Bisq 1 uses schemas with one `instanceof`-based alternative per subtype instead
(`Proposal.getProposalSchemaBuilder()`). They fail silently when a subtype is not registered or matches
twice. Prefer the `canonical` field in Bisq 2.

## 5. Data of other modules: `canonical`, or the kept `Any` and the `encodedMessage` fallback

A class of a **lower-level module** (a module that the network module depends on, such as `security`)
that a network class contains directly implements `Canonical` itself, with a `DEFAULT_SCHEMA` and the
`switch` over the purposes. The network class encodes it with a `canonical` field, so that it receives
the encoding purpose:

```java
// ConfidentialMessage: the protobuf field has the message type security.ConfidentialData
.canonical(1, confidentialMessage -> confidentialMessage.confidentialData)
```

Classes of **higher-level modules** (offers, user profiles, chat messages, ...) do not support canonical
encoding yet. The network module knows them only through interfaces (`DistributedData`,
`AppendOnlyData`) and holds them in a `google.protobuf.Any`. The network class that holds such data
keeps the received `Any` and writes it unchanged, so that fields which the node does not know are not
lost (`docs/specifications/network/distributed-data-payload-bytes.md`):

```java
// fromProto: keep the received Any, decode the data from the received instance
Any distributedDataAny = DistributedDataAny.retain(proto.getDistributedData());
DistributedData distributedData = DistributedData.fromAny(proto.getDistributedData());

// Data created by this node: serialize it once, when the holder is created
this(distributedData, distributedData.toAny(false));

// getBuilder: the kept Any, except for the hash serialization
.setDistributedData(serializeForHash ? distributedData.toAny(true) : distributedDataAny)
```

The schemas encode the data per purpose, once, where the data is held:

```java
// DEFAULT and SIGNATURE (AuthenticatedData base class, AddAppendOnlyDataRequest)
.compose(1, authenticatedData -> authenticatedData.distributedDataAny, DistributedDataAny.SCHEMA)

// STORAGE_KEY, PROOF_OF_WORK, and TRADE_CONTRACT: the protobuf hash serialization, with the comment
.encodedMessage(1, authenticatedData -> authenticatedData.distributedData.toAny(true).toByteArray())
```

The comment at the `STORAGE_KEY` fallback states: canonical encoding is not yet supported for this
type, because it is defined outside the network module; the protobuf serialization for hashes is used
instead.

- `DistributedDataAny.SCHEMA` writes the type URL and the value bytes unchanged. The `SIGNATURE`
  purpose therefore contains every field of the data, including `@ExcludeForHash` fields and fields that
  the node does not know.
- For the `STORAGE_KEY` fallback, keep the `Any`: `toAny(true)` contains the type URL and the
  `serializeForHash()` bytes, exactly as the protobuf hash serialization of the holder contains it.
  Writing only `serializeForHash()` would drop the type from the preimage.
- Never rebuild the written `Any` from the decoded object (`toAny(false)`) outside the creation of local
  data.
- `AppendOnlyData` extends `Canonical`, and its implementations define a real canonical encoding.
  `AddAppendOnlyDataRequest` uses it for `PROOF_OF_WORK`, for `TRADE_CONTRACT`, and for storage key V2
  instead of the fallback.
- Do not add a fallback `encodeCanonical` to the external classes or to their interfaces
  (`DistributedData`, `MailboxMessage`). They would appear to have a canonical
  encoding while they return protobuf bytes, and a later real encoding would silently change the bytes
  of all holders.
- The fallback is only as deterministic as the protobuf serialization of the external class.

## 6. Envelope messages: `EnvelopePayloadMessage` and `DataRequest`

`serialize()` of an `EnvelopePayloadMessage` implementation contains the envelope. The canonical
default encoding contains it too.

Data requests are wrapped twice (`EnvelopePayloadMessage.dataRequest = 12`, then the request type in
`DataRequest`). Use the helper, with the full interface name because many request classes import the
protobuf class `DataRequest`:

```java
public static final CanonicalSchema<RemoveMailboxRequest> DEFAULT_SCHEMA =
        bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(5,
                CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                        .canonical(1, request -> request.metaData)
                        .bytes(2, request -> request.hash)
                        .bytes(3, request -> request.receiverPublicKeyBytes)
                        .bytes(4, request -> request.signature)
                        .sint64(5, request -> request.created)
                        .sint32(6, request -> request.version));
```

The request field numbers in `DataRequest`: `AddAuthenticatedDataRequest = 1`,
`RemoveAuthenticatedDataRequest = 2`, `RefreshAuthenticatedDataRequest = 3`, `AddMailboxRequest = 4`,
`RemoveMailboxRequest = 5`, `AddAppendOnlyDataRequest = 6`.

When a parent embeds the bare message of an envelope class, provide both schemas:

```java
// ConfidentialMessage
public static final CanonicalSchema<ConfidentialMessage> DEFAULT_SCHEMA =
        CanonicalSchema.oneof("EnvelopePayloadMessage", 8, getValueSchemaBuilder());
public static final CanonicalSchema<ConfidentialMessage> DEFAULT_VALUE_SCHEMA = getValueSchemaBuilder().build();

// MailboxData embeds the bare ConfidentialMessage
.compose(1, MailboxData::getConfidentialMessage, ConfidentialMessage.DEFAULT_VALUE_SCHEMA)
```

`ConfidentialMessage.encodeCanonical` writes the envelope, so the parent cannot use a `canonical` field
for the bare message, and the purpose does not select the bare schema. Each purpose-specific schema of
`MailboxData` must reference the bare schema of `ConfidentialMessage` for the same purpose.

## 7. Delegation by hand

Prefer the `canonical` field. If a class must write records with `CanonicalWriter` itself, it is
responsible for ascending field numbers and the presence rules, and it encodes nested `Canonical`
objects with `value.encodeCanonical(canonicalEncoder, encodingPurpose)`, with the purpose that it
received. A nested object must receive the purpose of the preimage, so that it can select its encoding
for that purpose.

## 8. Static initialization and builder reuse

- **No subclass constants in superclass static fields.** When the JVM initializes a subclass, it first
  initializes the superclass. If the superclass's static initializer reads a static schema of that
  subclass, the value is still `null`, and the builder throws (`ExceptionInInitializerError`). Use
  static builder methods, as `AuthenticatedData` does.
- **Constants referencing other classes are fine** when there is no cycle (`MailboxData.DEFAULT_SCHEMA`
  uses `ConfidentialMessage.DEFAULT_VALUE_SCHEMA`).
- **Builders are mutable.** A method that returns a builder must create a new builder on every call
  (`ConfidentialMessage.getValueSchemaBuilder()`). Never store a `CanonicalSchema.Builder` in a static
  field.
