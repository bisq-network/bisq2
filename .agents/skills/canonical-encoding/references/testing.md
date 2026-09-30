# Testing canonical schemas

A schema is correct only when tests prove its bytes. Code review cannot find a wrong field type
(`sint32` instead of `int32`) or a missing field reliably; a byte comparison can.

## Where and how

- Put the tests into the test sources of the class's module, for the network storage classes next to
  `network/network/src/test/java/bisq/network/p2p/services/data/storage/StorageCanonicalEncoderTest.java`
  or into it.
- JUnit 5 with `assertArrayEquals`, as in the existing tests.
- Run the narrowest test first:

  ```bash
  ./gradlew :network:network:test --tests 'bisq.network.p2p.services.data.storage.StorageCanonicalEncoderTest'
  ./gradlew :common:test --tests 'bisq.common.encoding.canonical.*'
  ```

  Then run the whole test suite of the changed module.

## The comparisons

- **With `serialize()`.** The canonical default bytes must equal `serialize()`:
  `assertArrayEquals(object.serialize(), object.encodeCanonical())`. This proves field numbers, types,
  envelopes, and completeness.
- **External data per purpose.** Use an external-data stub with a field annotated with
  `@ExcludeForHash` (see `ExternalData` in `StorageCanonicalEncoderTest`: it serializes as
  `network.MetaData`, so no resolver is needed, and its `priority` is excluded from the hash). With a
  non-default value in that field, the `STORAGE_KEY`, `PROOF_OF_WORK`, and `TRADE_CONTRACT` bytes
  equal `serializeForHash()`, and the `DEFAULT` and `SIGNATURE` bytes equal `serialize()`.
- **External data with a field unknown to the node.** Build the `Any` as a newer node sends it, with an
  unknown field in the value, and decode the holder with `fromProto` (see `DistributedDataAnyTest`: its
  stub serializes as `common.StringLongPair` and registers a resolver). The field must survive relay,
  persistence, and inventory round trips, and the `SIGNATURE` bytes must contain it.
- **Differences to `serializeForHash()`.** Assert them (`assertFalse(Arrays.equals(...))`) for classes
  with `@ExcludeForHash` fields or a custom `serializeForHash()`, so that a change on either side is
  noticed.
- **Hash purposes.** `encodeCanonical(STORAGE_KEY)` of the stored data (also `AuthorizedData` with a
  signature), `encodeCanonical(PROOF_OF_WORK)`, and `encodeCanonical(TRADE_CONTRACT)` must equal
  `serializeForHash()`. The exception is `AuthorizedData` with a signature: its `PROOF_OF_WORK` and
  `TRADE_CONTRACT` bytes contain the signature and equal `toProto(true).toByteArray()` (see
  `authorizedDataContainsSignatureUnlikeHashSerialization`). Requests and sequential data must reject
  `STORAGE_KEY`.
  `encodeCanonical(SIGNATURE)` must equal it for classes without signature V2 (see
  `hashPurposesMatchSerializeForHash` in `StorageCanonicalEncoderTest`). For every message type,
  `PROOF_OF_WORK` and `TRADE_CONTRACT` must equal `serializeForHash()` of the message, also with
  `AuthorizedData` and with external data that has an excluded field (see
  `nestedHashPurposesMatchSerializeForHashOfEveryMessage`). A holder of data with a canonical encoding
  must pass the purpose on; test this with a stub that records the purposes it receives (see
  `addAppendOnlyDataRequestPassesEncodingPurposeToAppendOnlyData`). For `excludeOnlyInVersions`, test
  versions inside and outside the list. Assert documented differences (`AuthorizedData`, and the
  `SIGNATURE` bytes of holders of external data with an excluded field).
- **Signature V2 preimages.** Test the layout with bytes written independently by protobuf's
  `CodedOutputStream`, and test that a changed field makes the signature invalid (see
  `AuthenticatedDataSignatureV2Test` and `MailboxSignatureV2Test`).
- **The purpose reaches nested objects.** Bytes show it where a nested class has a purpose-specific
  schema, for example `MailboxSequentialData` with `MailboxData` in the hash-purpose test. Otherwise use a
  nested object that records the purposes it receives, as `PurposeRecordingAuthenticatedData` in
  `StorageCanonicalEncoderTest` does for `AddAuthenticatedDataRequest`, `AuthenticatedSequentialData`,
  and `AuthenticatedData`.
- **Bare and envelope schemas.** For a class with a `DEFAULT_VALUE_SCHEMA`, compare
  `CanonicalEncoder.DEFAULT.encode(object, X.DEFAULT_VALUE_SCHEMA, EncodingPurpose.DEFAULT)` with
  `toValueProto(false).toByteArray()`.

## Test data that can find mistakes

- Give fields of the same type in one message different values (two different hashes, a sequence number
  different from the version). Otherwise swapped fields produce equal bytes and the test passes.
- Use negative values for `sint32`/`sint64` where the type allows them (for example
  `MetaData.LOW_PRIORITY`).
- Cover absent optional values (`Optional.empty()`, `null` nested objects) and every implementation of a
  `canonical` field.
- The objects must pass `verify()`: 20-byte hashes, EC signatures of 68 to 74 bytes, real EC public keys
  (`KeyGeneration.generateDefaultEcKeyPair()`), and dates between the Bisq 1 launch and two hours in the
  future (`NetworkDataValidation`).
- For classes with private constructors, build the protobuf message and use the public `fromProto`.

## Tests for other kinds of change

- **New framework feature**: byte-level tests with fixed expected bytes in `CanonicalWriterTest` and
  `CanonicalEncoderTest`, including boundary values (`0`, `127`, `128`, negative, maximum).
- **New preimage without protobuf message**: compare with fixed expected bytes (hex).
- **Delegation by hand**: test that the nested object receives every purpose of the parent, with a stub
  that records the purpose and whose bytes depend on it.

## When a comparison fails

Find the first differing byte and decode the tag (`tag >>> 3` is the field number, `tag & 7` the wire
type). Typical causes: wrong field number or type (`int32` versus `sint32`), a missing non-transient
field, an envelope schema where the bare message is expected (or the reverse), a getter that returns a
transient runtime value, or external data encoded without its `Any`.

Fix the schema. Do not change the expectation to the canonical bytes.
