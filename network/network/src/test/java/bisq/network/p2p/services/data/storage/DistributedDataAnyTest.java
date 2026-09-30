/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.services.data.storage;

import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.data.ByteArray;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.locale.Region;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The network classes keep the google.protobuf.Any of received DistributedData and write it unchanged. The tests
 * simulate data of a newer version: its payload contains field 3, which the model of this node does not know. Under the
 * rules for new fields, the newer version annotates field 3 with @ExcludeForHash, so that the hash serialization, and
 * with it the storage key and the signature of the add request, is the same on nodes which do not know the field.
 */
public class DistributedDataAnyTest {
    private static final String KEY = "content";
    private static final long VALUE = 5;
    private static final KeyPair KEY_PAIR = KeyGeneration.generateDefaultEcKeyPair();
    private static final byte[] PUBLIC_KEY_BYTES = KEY_PAIR.getPublic().getEncoded();
    private static final byte[] AUTHORIZATION_SIGNATURE = filledBytes(70, 0x22);
    private static final long CREATED = System.currentTimeMillis() - 60_000;

    @BeforeAll
    static void registerResolver() {
        DistributedDataResolver.addResolver("common.StringLongPair", PayloadData.class, PayloadData::fromAny);
        DistributedDataResolver.addResolver("common.Country", NestedPayloadData.class, NestedPayloadData::fromAny);
    }

    @Test
    public void defaultAuthenticatedDataKeepsFieldUnknownToThisNode() throws InvalidProtocolBufferException {
        bisq.network.protobuf.AuthenticatedData proto = getDefaultAuthenticatedDataProto(getNewerAny("new field"));

        AuthenticatedData received = AuthenticatedData.fromProto(proto);

        assertEquals(new PayloadData(KEY, VALUE), received.getDistributedData());
        assertArrayEquals(proto.toByteArray(), received.serialize());
        assertArrayEquals(proto.toByteArray(), relay(received).serialize());
        // The hash serialization is rebuilt from the model without the fields excluded from the hash, as nodes of all
        // versions compute it
        assertArrayEquals(getDefaultAuthenticatedDataProto(getHashSerializationAny()).toByteArray(),
                received.serializeForHash());
    }

    @Test
    public void authorizedDataKeepsFieldUnknownToThisNode() throws InvalidProtocolBufferException {
        bisq.network.protobuf.AuthenticatedData proto = getAuthorizedDataProto(getNewerAny("new field"));

        AuthenticatedData received = AuthenticatedData.fromProto(proto);

        assertEquals(new PayloadData(KEY, VALUE), received.getDistributedData());
        assertArrayEquals(proto.toByteArray(), received.serialize());
        assertArrayEquals(proto.toByteArray(), relay(received).serialize());
        // serializeForHash() of AuthorizedData leaves out the signature as well
        bisq.network.protobuf.AuthenticatedData hashSerialization = getAuthorizedDataProto(getHashSerializationAny())
                .toBuilder()
                .setAuthorizedData(proto.getAuthorizedData().toBuilder().clearSignature())
                .build();
        assertArrayEquals(hashSerialization.toByteArray(), received.serializeForHash());
    }

    @Test
    public void addAuthenticatedDataRequestKeepsFieldUnknownToThisNode() throws Exception {
        bisq.network.protobuf.DataRequest proto = getAddAuthenticatedDataRequestProto(getNewerAny("new field"));

        AddAuthenticatedDataRequest received = (AddAuthenticatedDataRequest) DataRequest.fromProto(proto);

        // Signature V1 covers the hash serialization, which this node computes from its model
        assertFalse(received.isSignatureInvalid(BEFORE_CUTOFF));
        assertEquals(getStorageKeyOfNodesWhichKnowOnlyTheModel(), received.getStorageKeyV1());
        // Storage key V2 contains the hash of the model as well: a field unknown to this node does not change it
        AddAuthenticatedDataRequest createdFromModel = AddAuthenticatedDataRequest.fromProto(
                getAddAuthenticatedDataRequestProto(getHashSerializationAny()).getAddAuthenticatedDataRequest());
        assertEquals(createdFromModel.getStorageKey(), received.getStorageKey());

        // Relay
        assertArrayEquals(proto.toByteArray(), received.toDataRequestProto(false).toByteArray());
        assertArrayEquals(proto.toByteArray(), relay(received).toDataRequestProto(false).toByteArray());

        // Persistence
        DataStore<DataRequest> dataStore = new DataStore<>(Map.of(received.getStorageKey(), received));
        DataStore<?> persisted = (DataStore<?>) DataStore.fromProto(
                bisq.network.protobuf.DataStore.parseFrom(dataStore.serialize()));
        DataRequest restored = persisted.getMap().get(received.getStorageKey());
        assertArrayEquals(proto.toByteArray(), restored.toDataRequestProto(false).toByteArray());

        // Inventory response
        int maxSize = Inventory.maxSize;
        Inventory.setMaxSize(1_000_000);
        try {
            Inventory inventory = new Inventory(List.of(received), false);
            Inventory receivedInventory = Inventory.fromProto(
                    bisq.network.protobuf.Inventory.parseFrom(inventory.serialize()));
            assertArrayEquals(proto.toByteArray(),
                    receivedInventory.getEntries().get(0).toDataRequestProto(false).toByteArray());
        } finally {
            Inventory.setMaxSize(maxSize);
        }
    }

    @Test
    public void signaturePurposeContainsTheExactPayloadBytes() throws Exception {
        AddAuthenticatedDataRequest received = (AddAuthenticatedDataRequest) DataRequest.fromProto(
                getAddAuthenticatedDataRequestProto(getNewerAny("new field")));
        // A relay changes field 3 and keeps the signature of the request
        AddAuthenticatedDataRequest changedByRelay = (AddAuthenticatedDataRequest) DataRequest.fromProto(
                getAddAuthenticatedDataRequestProto(getNewerAny("changed field")));
        AuthenticatedSequentialData sequentialData = received.getAuthenticatedSequentialData();
        AuthenticatedSequentialData changedSequentialData = changedByRelay.getAuthenticatedSequentialData();

        // The hash serialization does not contain field 3, so signature V1 of the request does not detect the change
        assertArrayEquals(sequentialData.serializeForHash(), changedSequentialData.serializeForHash());
        assertFalse(changedByRelay.isSignatureInvalid(BEFORE_CUTOFF));

        // The SIGNATURE encoding contains the payload as serialize() writes it, also after a relay hop
        byte[] signaturePreimage = sequentialData.encodeCanonical(EncodingPurpose.SIGNATURE);
        assertArrayEquals(sequentialData.serialize(), signaturePreimage);
        assertArrayEquals(signaturePreimage,
                relay(received).getAuthenticatedSequentialData().encodeCanonical(EncodingPurpose.SIGNATURE));
        assertFalse(Arrays.equals(signaturePreimage, changedSequentialData.encodeCanonical(EncodingPurpose.SIGNATURE)));

        // A signature over it verifies on this node, which does not know field 3, and detects the change
        byte[] signature = SignatureUtil.sign(signaturePreimage, KEY_PAIR.getPrivate());
        assertTrue(SignatureUtil.verify(relay(received).getAuthenticatedSequentialData()
                        .encodeCanonical(EncodingPurpose.SIGNATURE),
                signature,
                KEY_PAIR.getPublic()));
        assertFalse(SignatureUtil.verify(changedSequentialData.encodeCanonical(EncodingPurpose.SIGNATURE),
                signature,
                KEY_PAIR.getPublic()));

        // The STORAGE_KEY encoding of the stored data is its hash serialization
        AuthenticatedData authenticatedData = sequentialData.getAuthenticatedData();
        assertArrayEquals(authenticatedData.serializeForHash(),
                authenticatedData.encodeCanonical(EncodingPurpose.STORAGE_KEY));
    }

    @Test
    public void addAppendOnlyDataRequestKeepsFieldUnknownToThisNode() throws InvalidProtocolBufferException {
        bisq.network.protobuf.DataRequest proto = bisq.network.protobuf.DataRequest.newBuilder()
                .setAddAppendOnlyDataRequest(bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder()
                        .setAppendOnlyData(getNewerAny("new field")))
                .build();

        AddAppendOnlyDataRequest received = (AddAppendOnlyDataRequest) DataRequest.fromProto(proto);

        assertEquals(new PayloadData(KEY, VALUE), received.getAppendOnlyData());
        assertArrayEquals(proto.toByteArray(), received.toDataRequestProto(false).toByteArray());
        assertArrayEquals(proto.toByteArray(), relay(received).toDataRequestProto(false).toByteArray());
        AddAppendOnlyDataRequest createdFromModel = new AddAppendOnlyDataRequest(new PayloadData(KEY, VALUE));
        assertEquals(createdFromModel.getStorageKeyV1(), received.getStorageKeyV1());
        // Storage key V2 uses the canonical STORAGE_KEY encoding of the model: a field unknown to this node does not
        // change it
        assertEquals(createdFromModel.getStorageKey(), received.getStorageKey());
        assertArrayEquals(received.serialize(), received.encodeCanonical(EncodingPurpose.SIGNATURE));
        assertArrayEquals(received.serializeForHash(), received.encodeCanonical(EncodingPurpose.PROOF_OF_WORK));
    }

    @Test
    public void dataCreatedByThisNodeContainsTheCompletePayload() {
        PayloadData payloadData = new PayloadData(KEY, VALUE);
        Any completeAny = Any.pack(bisq.common.protobuf.StringLongPair.newBuilder()
                .setKey(KEY)
                .setValue(VALUE)
                .build());

        DefaultAuthenticatedData created = new DefaultAuthenticatedData(payloadData);
        assertEquals(completeAny, created.toProto(false).getDistributedData());
        assertEquals(completeAny,
                new AuthorizedData(payloadData, KEY_PAIR.getPublic()).toProto(false).getDistributedData());
        assertEquals(completeAny, new AddAppendOnlyDataRequest(payloadData).toValueProto(false).getAppendOnlyData());

        // A copy received from a peer is equal to the created data, a copy with more fields is not
        assertEquals(created, AuthenticatedData.fromProto(created.toProto(false)));
        assertNotEquals(created,
                AuthenticatedData.fromProto(getDefaultAuthenticatedDataProto(getNewerAny("new field"))));
    }

    @Test
    public void typeUrlOtherThanTheOneOfAnyPackIsRejected() {
        Any newerAny = getNewerAny("new field");
        List<String> typeUrls = List.of(
                // Resolves to the same type, so only the check of the type URL rejects it
                "example.com/common.StringLongPair",
                "type.googleapis.com/common.StringLongPair/suffix",
                "common.StringLongPair");
        for (String typeUrl : typeUrls) {
            Any any = newerAny.toBuilder().setTypeUrl(typeUrl).build();
            assertThrows(IllegalArgumentException.class,
                    () -> AuthenticatedData.fromProto(getDefaultAuthenticatedDataProto(any)),
                    typeUrl);
        }
    }

    @Test
    public void unknownFieldsOfTheAnyMessageAreNotKept() {
        Any newerAny = getNewerAny("new field");
        Any anyWithUnknownField = newerAny.toBuilder()
                .setUnknownFields(UnknownFieldSet.newBuilder()
                        .addField(3, UnknownFieldSet.Field.newBuilder().addVarint(1).build())
                        .build())
                .build();

        AuthenticatedData received = AuthenticatedData.fromProto(getDefaultAuthenticatedDataProto(anyWithUnknownField));

        assertEquals(newerAny, received.toProto(false).getDistributedData());
    }

    @Test
    public void holdersKeepPayloadAtExtraByteLimit() throws InvalidProtocolBufferException {
        // Field 3 has a one-byte tag and a two-byte length prefix.
        Any any = getNewerAny("x".repeat(DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES - 3));
        assertEquals(DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES,
                any.unpack(bisq.common.protobuf.StringLongPair.class).getUnknownFields().getSerializedSize());

        assertAllHoldersKeep(any);
        AuthenticatedData received = AuthenticatedData.fromProto(getDefaultAuthenticatedDataProto(any));
        assertArrayEquals(getDefaultAuthenticatedDataProto(getHashSerializationAny()).toByteArray(),
                received.serializeForHash());
    }

    @Test
    public void holdersRejectPayloadAboveExtraByteLimit() {
        assertAllHoldersReject(getNewerAny("x".repeat(DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES - 2)));
        assertAllHoldersReject(getNewerAny("x".repeat(3_000_000)));
    }

    @Test
    public void extraByteLimitIncludesNestedUnknownFields() {
        bisq.common.protobuf.Country country = getCountryProto();
        UnknownFieldSet unknownFields = getUnknownBytes(6000);
        assertTrue(unknownFields.getSerializedSize() < DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES);
        bisq.common.protobuf.Country nestedOnly = country.toBuilder()
                .setRegion(country.getRegion().toBuilder().setUnknownFields(unknownFields))
                .build();
        assertAllHoldersKeep(Any.pack(nestedOnly));

        // Both individual unknown-field sets fit, but their combined retained overhead does not.
        assertAllHoldersReject(Any.pack(nestedOnly.toBuilder().setUnknownFields(unknownFields).build()));
        assertAllHoldersReject(Any.pack(country.toBuilder()
                .setRegion(country.getRegion().toBuilder()
                        .setUnknownFields(getUnknownBytes(DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES)))
                .build()));
    }

    @Test
    public void knownFieldsExcludedFromHashesDoNotConsumeExtraByteAllowance() {
        Any any = Any.pack(getCountryProto().toBuilder()
                .setName("x".repeat(DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES + 1)).build());
        assertTrue(any.getValue().size() > DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES);

        assertAllHoldersKeep(any);
    }

    @Test
    public void holdersRejectRedundantKnownFieldsWithoutUnknownFields() throws Exception {
        Any original = new PayloadData(KEY, VALUE).toAny(false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CodedOutputStream protobuf = CodedOutputStream.newInstance(output);
        // A singular string is overwritten by its last occurrence, so this large first value is lost on decoding.
        protobuf.writeString(1, "x".repeat(3_000_000));
        protobuf.writeRawBytes(original.getValue());
        protobuf.flush();
        Any padded = original.toBuilder().setValue(ByteString.copyFrom(output.toByteArray())).build();
        bisq.common.protobuf.StringLongPair decoded = padded.unpack(bisq.common.protobuf.StringLongPair.class);
        assertEquals(KEY, decoded.getKey());
        assertEquals(0, decoded.getUnknownFields().getSerializedSize());

        assertAllHoldersReject(padded);
    }

    @Test
    public void oversizedSignedRequestIsRejectedAndSkippedOnRestore() throws Exception {
        bisq.network.protobuf.DataRequest original = getAddAuthenticatedDataRequestProto(getNewerAny("new field"));
        AddAuthenticatedDataRequest received = (AddAuthenticatedDataRequest) DataRequest.fromProto(original);
        assertFalse(received.isSignatureInvalid(BEFORE_CUTOFF));
        bisq.network.protobuf.AddAuthenticatedDataRequest addRequest = original.getAddAuthenticatedDataRequest();
        bisq.network.protobuf.AuthenticatedSequentialData sequentialData = addRequest.getAuthenticatedSequentialData();
        bisq.network.protobuf.DataRequest padded = original.toBuilder()
                .setAddAuthenticatedDataRequest(addRequest.toBuilder()
                        .setAuthenticatedSequentialData(sequentialData.toBuilder()
                                .setAuthenticatedData(sequentialData.getAuthenticatedData().toBuilder()
                                        .setDistributedData(getNewerAny("x".repeat(
                                                DistributedDataAny.MAX_EXTRA_PAYLOAD_BYTES - 2))))))
                .build();
        assertEquals(addRequest.getSignature(), padded.getAddAuthenticatedDataRequest().getSignature());
        assertThrows(IllegalArgumentException.class, () -> DataRequest.fromProto(padded));

        ByteArray oversizedKey = new ByteArray(filledBytes(20, 0x77));
        bisq.network.protobuf.DataStore store = bisq.network.protobuf.DataStore.newBuilder()
                .addMapEntries(bisq.network.protobuf.DataStore.MapEntry.newBuilder()
                        .setKey(received.getStorageKey().toProto(false)).setValue(original))
                .addMapEntries(bisq.network.protobuf.DataStore.MapEntry.newBuilder()
                        .setKey(oversizedKey.toProto(false)).setValue(padded))
                .build();
        DataStore<?> restored = (DataStore<?>) DataStore.fromProto(
                bisq.network.protobuf.DataStore.parseFrom(store.toByteArray()));
        assertEquals(1, restored.getMap().size());
        assertFalse(restored.getMap().containsKey(oversizedKey));
        assertArrayEquals(received.serialize(), restored.getMap().get(received.getStorageKey()).serialize());
    }

    private static void assertAllHoldersKeep(Any any) {
        for (bisq.network.protobuf.AuthenticatedData proto : List.of(getDefaultAuthenticatedDataProto(any),
                getAuthorizedDataProto(any))) {
            AuthenticatedData received = AuthenticatedData.fromProto(proto);
            assertEquals(any, received.toProto(false).getDistributedData());
            assertArrayEquals(received.serialize(), received.encodeCanonical(EncodingPurpose.SIGNATURE));
        }
        AddAppendOnlyDataRequest received = AddAppendOnlyDataRequest.fromProto(
                bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder().setAppendOnlyData(any).build());
        assertEquals(any, received.toValueProto(false).getAppendOnlyData());
    }

    private static void assertAllHoldersReject(Any any) {
        assertThrows(IllegalArgumentException.class,
                () -> AuthenticatedData.fromProto(getDefaultAuthenticatedDataProto(any)));
        assertThrows(IllegalArgumentException.class,
                () -> AuthenticatedData.fromProto(getAuthorizedDataProto(any)));
        assertThrows(IllegalArgumentException.class, () -> AddAppendOnlyDataRequest.fromProto(
                bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder().setAppendOnlyData(any).build()));
    }

    private static UnknownFieldSet getUnknownBytes(int length) {
        return UnknownFieldSet.newBuilder()
                .addField(10, UnknownFieldSet.Field.newBuilder()
                        .addLengthDelimited(ByteString.copyFrom(new byte[length])).build())
                .build();
    }

    private static bisq.common.protobuf.Country getCountryProto() {
        return bisq.common.protobuf.Country.newBuilder().setCode("DE").setName("Germany")
                .setRegion(bisq.common.protobuf.Region.newBuilder().setCode("EU").setName("Europe"))
                .build();
    }

    // The payload as a newer node sends it: with field 3, which this node does not know
    private static Any getNewerAny(String field3) {
        UnknownFieldSet newField = UnknownFieldSet.newBuilder()
                .addField(3, UnknownFieldSet.Field.newBuilder()
                        .addLengthDelimited(ByteString.copyFromUtf8(field3))
                        .build())
                .build();
        return Any.pack(bisq.common.protobuf.StringLongPair.newBuilder()
                .setKey(KEY)
                .setValue(VALUE)
                .setUnknownFields(newField)
                .build());
    }

    // The payload in the hash serialization of all node versions: without value and field 3, which are annotated with
    // @ExcludeForHash
    private static Any getHashSerializationAny() {
        return Any.pack(bisq.common.protobuf.StringLongPair.newBuilder()
                .setKey(KEY)
                .build());
    }

    private static ByteArray getStorageKeyOfNodesWhichKnowOnlyTheModel() {
        AuthenticatedData createdFromModel = new DefaultAuthenticatedData(new PayloadData(KEY, VALUE));
        return new ByteArray(DigestUtil.hash(createdFromModel.serializeForHash()));
    }

    private static bisq.network.protobuf.AuthenticatedData getDefaultAuthenticatedDataProto(Any any) {
        return bisq.network.protobuf.AuthenticatedData.newBuilder()
                .setDistributedData(any)
                .setDefaultAuthenticatedData(bisq.network.protobuf.DefaultAuthenticatedData.newBuilder())
                .build();
    }

    private static bisq.network.protobuf.AuthenticatedData getAuthorizedDataProto(Any any) {
        return bisq.network.protobuf.AuthenticatedData.newBuilder()
                .setDistributedData(any)
                .setAuthorizedData(bisq.network.protobuf.AuthorizedData.newBuilder()
                        .setSignature(ByteString.copyFrom(AUTHORIZATION_SIGNATURE))
                        .setAuthorizedPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES)))
                .build();
    }

    // The add request as the newer node creates it
    private static bisq.network.protobuf.DataRequest getAddAuthenticatedDataRequestProto(Any any)
            throws GeneralSecurityException {
        bisq.network.protobuf.AuthenticatedSequentialData sequentialData =
                getAuthenticatedSequentialDataProto(getDefaultAuthenticatedDataProto(any));
        // The newer node signs its hash serialization, which does not contain field 3
        byte[] preImage = getAuthenticatedSequentialDataProto(
                getDefaultAuthenticatedDataProto(getHashSerializationAny())).toByteArray();
        byte[] signature = SignatureUtil.sign(preImage, KEY_PAIR.getPrivate());
        return bisq.network.protobuf.DataRequest.newBuilder()
                .setAddAuthenticatedDataRequest(bisq.network.protobuf.AddAuthenticatedDataRequest.newBuilder()
                        .setAuthenticatedSequentialData(sequentialData)
                        .setSignature(ByteString.copyFrom(signature))
                        .setOwnerPublicKeyBytes(ByteString.copyFrom(PUBLIC_KEY_BYTES)))
                .build();
    }

    private static bisq.network.protobuf.AuthenticatedSequentialData getAuthenticatedSequentialDataProto(
            bisq.network.protobuf.AuthenticatedData authenticatedData) {
        return bisq.network.protobuf.AuthenticatedSequentialData.newBuilder()
                .setAuthenticatedData(authenticatedData)
                .setSequenceNumber(1)
                .setPubKeyHash(ByteString.copyFrom(DigestUtil.hash(PUBLIC_KEY_BYTES)))
                .setCreated(CREATED)
                .build();
    }

    private static AuthenticatedData relay(AuthenticatedData authenticatedData) throws InvalidProtocolBufferException {
        return AuthenticatedData.fromProto(
                bisq.network.protobuf.AuthenticatedData.parseFrom(authenticatedData.serialize()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends DataRequest> T relay(T dataRequest) throws InvalidProtocolBufferException {
        return (T) DataRequest.fromProto(bisq.network.protobuf.DataRequest.parseFrom(
                dataRequest.toDataRequestProto(false).toByteArray()));
    }

    private static byte[] filledBytes(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    // Uses a nested model whose decoder drops unknown fields, as production payload models do.
    private static final class NestedPayloadData implements AuthorizedDistributedData, AppendOnlyData {
        private final String code;
        @ExcludeForHash
        private final String name;
        private final Region region;

        private NestedPayloadData(bisq.common.protobuf.Country proto) {
            code = proto.getCode();
            name = proto.getName();
            region = Region.fromProto(proto.getRegion());
        }

        private static NestedPayloadData fromAny(Any any) {
            try {
                return new NestedPayloadData(any.unpack(bisq.common.protobuf.Country.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.common.protobuf.Country.Builder getBuilder(boolean serializeForHash) {
            return bisq.common.protobuf.Country.newBuilder().setCode(code).setName(name)
                    .setRegion(region.toProto(serializeForHash));
        }

        @Override
        public MetaData getMetaData() {
            return new MetaData(MetaData.TTL_10_DAYS, "NestedPayloadData");
        }

        @Override
        public boolean isDataInvalid() {
            return false;
        }

        // common.proto: message Region { string code = 1; string name = 2; }
        private static final CanonicalSchema<Region> REGION_SCHEMA = CanonicalSchema.<Region>newBuilder()
                .string(1, Region::getCode)
                .string(2, Region::getName)
                .build();

        private static final CanonicalSchema<NestedPayloadData> DEFAULT_SCHEMA = CanonicalSchema.<NestedPayloadData>newBuilder()
                .string(1, data -> data.code)
                .string(2, data -> data.name)
                .compose(3, data -> data.region, REGION_SCHEMA)
                .build();

        // Leaves out name (field 2), which is annotated with @ExcludeForHash, as serializeForHash() does
        private static final CanonicalSchema<NestedPayloadData> SCHEMA_FOR_HASH = CanonicalSchema.<NestedPayloadData>newBuilder()
                .string(1, data -> data.code)
                .compose(3, data -> data.region, REGION_SCHEMA)
                .build();

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            CanonicalSchema<NestedPayloadData> schema = switch (encodingPurpose) {
                case DEFAULT -> DEFAULT_SCHEMA;
                case SIGNATURE -> DEFAULT_SCHEMA;
                case STORAGE_KEY -> SCHEMA_FOR_HASH;
                case PROOF_OF_WORK -> SCHEMA_FOR_HASH;
                case TRADE_CONTRACT -> SCHEMA_FOR_HASH;
            };
            return canonicalEncoder.encode(this, schema, encodingPurpose);
        }
    }

    // DistributedData of a higher-level module as this node knows it. It serializes as common.StringLongPair. value is
    // annotated with @ExcludeForHash, like a field which was added under the rules for new fields.
    @EqualsAndHashCode
    private static final class PayloadData implements AuthorizedDistributedData, AppendOnlyData {
        private final String key;
        @ExcludeForHash
        private final long value;
        private final transient MetaData metaData = new MetaData(MetaData.TTL_10_DAYS, "PayloadData");

        private PayloadData(String key, long value) {
            this.key = key;
            this.value = value;
        }

        private static PayloadData fromAny(Any any) {
            try {
                bisq.common.protobuf.StringLongPair proto = any.unpack(bisq.common.protobuf.StringLongPair.class);
                return new PayloadData(proto.getKey(), proto.getValue());
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.common.protobuf.StringLongPair.Builder getBuilder(boolean serializeForHash) {
            return bisq.common.protobuf.StringLongPair.newBuilder()
                    .setKey(key)
                    .setValue(value);
        }

        @Override
        public bisq.common.protobuf.StringLongPair toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return metaData;
        }

        @Override
        public boolean isDataInvalid() {
            return false;
        }

        private static final CanonicalSchema<PayloadData> DEFAULT_SCHEMA = CanonicalSchema.<PayloadData>newBuilder()
                .string(1, payloadData -> payloadData.key)
                .sint64(2, payloadData -> payloadData.value)
                .build();

        // Leaves out value (field 2), which is annotated with @ExcludeForHash, as serializeForHash() does
        private static final CanonicalSchema<PayloadData> SCHEMA_FOR_HASH = CanonicalSchema.<PayloadData>newBuilder()
                .string(1, payloadData -> payloadData.key)
                .build();

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            CanonicalSchema<PayloadData> schema = switch (encodingPurpose) {
                case DEFAULT -> DEFAULT_SCHEMA;
                case SIGNATURE -> DEFAULT_SCHEMA;
                case STORAGE_KEY -> SCHEMA_FOR_HASH;
                case PROOF_OF_WORK -> SCHEMA_FOR_HASH;
                case TRADE_CONTRACT -> SCHEMA_FOR_HASH;
            };
            return canonicalEncoder.encode(this, schema, encodingPurpose);
        }
    }
}
