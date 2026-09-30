# Field types: builder methods, bytes, and conversions

Read this when you map protobuf fields to `CanonicalSchema.Builder` calls. The normative rules are in
`docs/specifications/encoding/canonical-encoding.md` (section "Wire format").

## Builder methods

Every method takes the protobuf field number first. Declare fields in ascending field-number order.
Allowed numbers: `1..536870911`, except `19000..19999`.

| Protobuf field | Builder call | Getter type | No record when |
| --- | --- | --- | --- |
| `sint32 x = n;` | `sint32(n, getter)` | `ToIntFunction<T>` | `0` |
| `sint64 x = n;` | `sint64(n, getter)` | `ToLongFunction<T>` | `0` |
| `int32 x = n;` | `int32(n, getter)` | `ToIntFunction<T>` | `0` |
| `uint32 x = n;` | `uint32(n, getter)` | `ToIntFunction<T>` (read as unsigned) | `0` |
| `int64 x = n;` | `int64(n, getter)` | `ToLongFunction<T>` | `0` |
| `double x = n;` | `doubleField(n, getter)` | `ToDoubleFunction<T>` | raw bits `0` (only `+0.0`) |
| `bool x = n;` | `bool(n, getter)` | `Predicate<T>` | `false` |
| `SomeEnum x = n;` | `enumField(n, getter)` | `Function<T, ? extends CanonicalEnum>` | `null` or code `0` |
| `string x = n;` | `string(n, getter)` | `Function<T, String>` | `null` or `""` |
| `bytes x = n;` | `bytes(n, getter)` | `Function<T, byte[]>` | `null` or empty |
| `Foo x = n;` (network class that implements `Canonical`) | `canonical(n, getter)` | `Function<T, ? extends Canonical>` | getter returns `null` |
| `Foo x = n;` (message without own `encodeCanonical`, for example a bare message) | `compose(n, getter, Foo.DEFAULT_VALUE_SCHEMA)` | `Function<T, N>` | getter returns `null` |
| subclass alternative of the base message's `oneof` | `extend(n, sub -> sub, schemaOrBuilder)` | `Function<T, N>` | getter returns `null` |
| envelope alternative | `oneof(n, getter, schemaOrBuilder)` | `Function<T, N>` | getter returns `null` |
| field whose declared type has several implementations | `canonical(n, getter)`; the runtime type selects the encoding | `Function<T, ? extends Canonical>` | getter returns `null` |
| `google.protobuf.Any` with data of a higher-level module | `DEFAULT` and `SIGNATURE`: `compose(n, getter, DistributedDataAny.SCHEMA)` with the kept `Any`; `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`: `encodedMessage(n, getter)` | `Function<T, Any>`; `Function<T, byte[]>` | getter returns `null` |
| `repeated string x = n;` | `repeatedString(n, getter)` | `Function<T, List<String>>` | empty list |
| `repeated int32 x = n;` (packed) | `packedRepeatedInt32(n, getter)` | `Function<T, List<Integer>>` | empty list |
| `repeated Foo x = n;` | `repeatedCompose(n, getter, Foo.DEFAULT_SCHEMA)` | `Function<T, ? extends List<N>>` | empty list |
| `map<string, string> x = n;` | `mapStringToString(n, getter, iterator)` | `Function<T, ? extends Map<String, String>>` | empty map |
| `map<string, Foo> x = n;` | `mapStringToCompose(n, getter, Foo.DEFAULT_SCHEMA, iterator)` | `Function<T, ? extends Map<String, V>>` | empty map |

Behavior that is easy to get wrong:

- `sint32`/`sint64` use ZigZag encoding; `int32`/`int64` do not. The bytes differ for every non-zero
  value. Most Bisq 2 integers are `sint32` or `sint64`.
- Negative `int32` and `int64` values are 10-byte varints. A negative Java `int` in a `uint32` field is a
  5-byte varint of the unsigned value.
- Elements of repeated fields are always written, including `""` and `0`.
- A non-null nested object whose schema produces no bytes is still written (tag and length `0`), as
  protobuf writes `setX(X.newBuilder())`. An empty array in an `encodedMessage` field is written the same
  way.
- `null` lists, `null` maps, `null` elements, and `null` map keys or values make the encoder throw.
- `canonical` passes the encoding purpose of the parent to the nested object, which selects its own
  encoding. `compose`, `repeatedCompose`, and map values use the referenced schema for every purpose;
  a purpose-specific schema of the parent must reference the nested schema for the same purpose.

## Conversions: mirror the protobuf builder

The canonical value of a field is the value that the protobuf builder receives. Copy the conversion,
not the Java field type.

| The protobuf builder receives | Canonical getter | Example |
| --- | --- | --- |
| `setX(value)` for `sint32`/`sint64` | `sint32(n, o -> o.value)` / `sint64(...)` | `MetaData.ttl`, `RemoveMailboxRequest.created` |
| `setX(ByteString.copyFrom(bytes))` | `bytes(n, o -> o.bytes)` | `AddAuthenticatedDataRequest.signature` |
| `optionalBytes.ifPresent(v -> builder.setX(ByteString.copyFrom(v)))` for `optional bytes` | `bytes(n, o -> o.optionalBytes.orElse(null))`; only correct when a present value is never empty | `AuthorizedData.signature` |
| `setX(nested.toProto(serializeForHash))` for a network class | `canonical(n, o -> o.nested)` | `MailboxData.metaData` |
| `setX(nested.toProto(...).getBareMessage())` (bare message of an envelope class) | `compose(n, o -> o.nested, Nested.DEFAULT_VALUE_SCHEMA)` | `MailboxData.confidentialMessage` |
| `setX(serializeForHash ? distributedData.toAny(true) : distributedDataAny)` with data of a higher-level module | `DEFAULT` and `SIGNATURE`: `compose(n, o -> o.distributedDataAny, DistributedDataAny.SCHEMA)`; `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT`: `encodedMessage(n, o -> o.distributedData.toAny(true).toByteArray())` | `AuthenticatedData.distributedData` |
| `setX(lowerLevel.toProto(serializeForHash))` with a class of a lower-level module | `canonical(n, o -> o.lowerLevel)`; the class implements `Canonical` | `ConfidentialMessage.confidentialData` |
| `setX(javaEnum.toProtoEnum())` | `enumField(n, o -> o.javaEnum)`; the enum implements `CanonicalEnum` | - |
| field annotated with `@ExcludeForHash` | in `DEFAULT_SCHEMA` and `SIGNATURE_V2_SCHEMA`; left out of `STORAGE_KEY_SCHEMA`, `PROOF_OF_WORK_SCHEMA`, and `TRADE_CONTRACT_SCHEMA`. With `excludeOnlyInVersions`, these schemas use a getter that returns `null` in the listed versions | `MailboxData.version`, `MailboxData.className` |

The `STORAGE_KEY` variant of the `Any` row always needs the comment that canonical encoding is not yet
supported for that type (see `AuthenticatedData.getSchemaBuilderForHash()`).

### Enums

For a protobuf enum field, the Java enum implements `CanonicalEnum`. `getCode()` returns the number of
the protobuf enum constant; the protobuf default constant (number `0`) produces no record. Either return
explicit codes (then test every code against `bisq.<module>.protobuf.X.Y.getNumber()`), or return the
number of the protobuf constant directly.

## Not supported

`uint64` (used in several Bisq 2 protobuf files), `float`, `fixed32`, `fixed64`, `sfixed32`, `sfixed64`,
repeated `bytes`/`int64`/`sint32`/`sint64`/`uint32`/`bool`/enum/`double`, packed encoding of other types
than `int32`, map keys other than `string`, map values other than `string` and message, explicit
presence (proto3 `optional` whose present value can be the default), groups, extensions.

If a field needs one of these, stop and report it to the developer. Adding a type needs a writer method
with byte-level tests in `CanonicalWriterTest`, a builder method, tests, and an update of the
specification. Do not emulate a missing type with `bytes` or `encodedMessage`.
