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

package bisq.network.p2p.services.data.storage.auth;

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.network.p2p.services.data.storage.EcPublicKeyVariants;
import bisq.network.p2p.services.data.storage.EcdsaSignatureVariants;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.List;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Signature V2 of the add and remove requests of authenticated data covers every field of the request except itself
 * and the deprecated metaData. Nodes of earlier versions know neither signatureV2 nor the version of the add request.
 * Both are excluded from the hash, so the proof of work payload is the same for nodes of all versions.
 * <p>
 * A node accepts requests without signature V2 only before SignedRequest.SIGNATURE_V1_CUTOFF_DATE. The tests of such
 * requests pass the time of the node, so they do not depend on the date at which they run.
 */
class AuthenticatedDataSignatureV2Test {
    private static final List<String> INVALID_CLASS_NAMES = List.of(
            "Name\nforged log line",
            "A".repeat(51),
            "lowerCaseName");

    private static final String STORE_KEY = TestDistributedData.class.getSimpleName();

    @TempDir
    Path tempDir;

    private AuthenticatedDataStorageService store;
    private KeyPair ownerKeyPair;
    private AuthenticatedData authenticatedData;

    @BeforeEach
    void setUp() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "owner",
                STORE_KEY);
        ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        authenticatedData = new DefaultAuthenticatedData(new TestDistributedData("content",
                ownerKeyPair.getPublic()));
    }

    @Test
    void addRequestHasValidSignaturesAfterProtobufRoundTrip()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        AddAuthenticatedDataRequest received = fromProto(request.toValueProto(false), request);

        assertEquals(2, received.getVersion());
        assertTrue(received.isSignatureV2Present());
        assertFalse(received.isSignatureV2Invalid());
        assertFalse(received.isSignatureV1Invalid());
        assertFalse(received.isSignatureInvalid());
    }

    @Test
    void addRequestPubKeyHashMustBeTheHashOfTheOwnerPublicKey()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.AddAuthenticatedDataRequest proto = request.toValueProto(false);
        AuthenticatedSequentialData sequentialData = request.getAuthenticatedSequentialData();
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        byte[] otherPublicKeyBytes = otherKeyPair.getPublic().getEncoded();

        // Otherwise the storage key V2 and the signature name one key, and the owner check uses another one.
        assertThrows(IllegalArgumentException.class, () -> fromProto(proto.toBuilder()
                .setOwnerPublicKeyBytes(ByteString.copyFrom(otherPublicKeyBytes))
                .build(), request));
        // Data of the other key, so that the sequential data is valid and only the request names another key
        AuthenticatedSequentialData changedPubKeyHash = new AuthenticatedSequentialData(
                new DefaultAuthenticatedData(new TestDistributedData("content", otherKeyPair.getPublic())),
                sequentialData.getSequenceNumber(),
                DigestUtil.hash(otherPublicKeyBytes),
                sequentialData.getCreated());
        assertThrows(IllegalArgumentException.class, () -> new AddAuthenticatedDataRequest(changedPubKeyHash,
                proto.getSignature().toByteArray(),
                proto.getOwnerPublicKeyBytes().toByteArray(),
                request.getSignaturePublicKey(),
                proto.getSignatureV2().toByteArray(),
                proto.getVersion()));
    }

    @Test
    void addRequestWithChangedVersionIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        AddAuthenticatedDataRequest changed = fromProto(request.toValueProto(false).toBuilder()
                .setVersion(3)
                .build(), request);

        assertTrue(changed.isSignatureInvalid());
    }

    // Version 0 is the format of releases before the version field existed. No node signs it with signature V2.
    @Test
    void requestsWithSignatureV2AndVersion0AreRejected() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        assertThrows(IllegalArgumentException.class,
                () -> fromProto(addRequest.toValueProto(false).toBuilder().setVersion(0).build(), addRequest));

        bisq.network.protobuf.RemoveAuthenticatedDataRequest removeProto = RemoveAuthenticatedDataRequest
                .from(store, authenticatedData, ownerKeyPair).toValueProto(false).toBuilder()
                .setVersion(0)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> RemoveAuthenticatedDataRequest.fromProto(removeProto));

        bisq.network.protobuf.RefreshAuthenticatedDataRequest refreshProto = RefreshAuthenticatedDataRequest
                .from(store, authenticatedData, ownerKeyPair).toValueProto(false).toBuilder()
                .setVersion(0)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> RefreshAuthenticatedDataRequest.fromProto(refreshProto));
    }

    // Version 2 is the first version with signature V2. Add requests of nodes up to 2.1.13 have version 0, the other
    // requests version 1, all without signature V2.
    @Test
    void requestsWithSignatureV2AndVersion1AreRejected() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        assertThrows(IllegalArgumentException.class,
                () -> fromProto(addRequest.toValueProto(false).toBuilder().setVersion(1).build(), addRequest));

        bisq.network.protobuf.RemoveAuthenticatedDataRequest removeProto = RemoveAuthenticatedDataRequest
                .from(store, authenticatedData, ownerKeyPair).toValueProto(false).toBuilder()
                .setVersion(1)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> RemoveAuthenticatedDataRequest.fromProto(removeProto));

        bisq.network.protobuf.RefreshAuthenticatedDataRequest refreshProto = RefreshAuthenticatedDataRequest
                .from(store, authenticatedData, ownerKeyPair).toValueProto(false).toBuilder()
                .setVersion(1)
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> RefreshAuthenticatedDataRequest.fromProto(refreshProto));
    }

    @Test
    void cutoffDateOfSignatureV1IsNovember25InUtc() {
        assertEquals(Instant.parse("2026-11-25T00:00:00Z").toEpochMilli(),
                SignedRequest.SIGNATURE_V1_CUTOFF_DATE.getTime());
        assertFalse(SignedRequest.isSignatureV2Required(BEFORE_CUTOFF));
        assertTrue(SignedRequest.isSignatureV2Required(AT_CUTOFF));
    }

    @Test
    void addRequestWithoutSignatureV2IsVerifiedWithSignatureV1BeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        AddAuthenticatedDataRequest asFromEarlierVersion = fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().clearVersion().build(), request);

        assertFalse(asFromEarlierVersion.isSignatureV2Present());
        assertFalse(asFromEarlierVersion.isRequiredSignatureV2Missing(BEFORE_CUTOFF));
        assertFalse(asFromEarlierVersion.isSignatureInvalid(BEFORE_CUTOFF));
        assertArrayEquals(request.serializeForHash(), asFromEarlierVersion.serializeForHash());
    }

    // Signature V1 is valid, but it does not cover all fields of the request. From the cutoff date on, a node does not
    // accept it alone.
    @Test
    void addRequestWithoutSignatureV2IsInvalidFromTheCutoffDateOn()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        AddAuthenticatedDataRequest asFromEarlierVersion = fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().clearVersion().build(), request);

        assertFalse(asFromEarlierVersion.isSignatureV1Invalid());
        assertTrue(asFromEarlierVersion.isRequiredSignatureV2Missing(AT_CUTOFF));
        assertTrue(asFromEarlierVersion.isSignatureInvalid(AT_CUTOFF));
    }

    @Test
    void requestsWithSignatureV2AreValidBeforeAndAfterTheCutoffDate() throws GeneralSecurityException {
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store, authenticatedData,
                ownerKeyPair);

        for (long now : new long[]{BEFORE_CUTOFF, AT_CUTOFF}) {
            assertFalse(addRequest.isRequiredSignatureV2Missing(now));
            assertFalse(addRequest.isSignatureInvalid(now));
            assertFalse(removeRequest.isRequiredSignatureV2Missing(now));
            assertFalse(removeRequest.isSignatureInvalid(now));
        }
    }

    // The cutoff date does not make a request with an invalid signature V2 fall back to signature V1
    @Test
    void requestWithInvalidSignatureV2IsInvalidBeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest changed = RemoveAuthenticatedDataRequest.fromProto(
                request.toValueProto(false).toBuilder().setSequenceNumber(request.getSequenceNumber() + 1).build());

        assertFalse(changed.isSignatureV1Invalid());
        assertTrue(changed.isSignatureInvalid(BEFORE_CUTOFF));
        assertTrue(changed.isSignatureInvalid(AT_CUTOFF));
    }

    @Test
    void removeRequestHasValidSignaturesAfterProtobufRoundTrip()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        RemoveAuthenticatedDataRequest received = RemoveAuthenticatedDataRequest.fromProto(request.toValueProto(false));

        assertTrue(received.isSignatureV2Present());
        assertFalse(received.isSignatureV2Invalid());
        assertFalse(received.isSignatureV1Invalid());
        assertFalse(received.isSignatureInvalid());
    }

    @Test
    void removeRequestWithChangedFieldIsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto = request.toValueProto(false);

        // Signature V1 covers only the hash. Signature V2 covers these fields as well.
        // The className of the deprecated metaData changes as well, otherwise parsing rejects the request
        //noinspection deprecation
        assertTrue(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder()
                        .setClassName("OtherType")
                        .setMetaData(new MetaData(MetaData.TTL_10_DAYS, "OtherType").toProto(false))
                        .build())
                .isSignatureInvalid());
        assertTrue(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder().setCreated(proto.getCreated() - 1).build())
                .isSignatureInvalid());
        assertTrue(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder().setVersion(3).build())
                .isSignatureInvalid());
        assertTrue(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder()
                        .setSequenceNumber(proto.getSequenceNumber() + 1)
                        .build())
                .isSignatureInvalid());
    }

    @Test
    void removeRequestWithoutSignatureV2IsVerifiedWithSignatureV1BeforeTheCutoffDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        RemoveAuthenticatedDataRequest withoutSignatureV2 = RemoveAuthenticatedDataRequest.fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().build());

        assertFalse(withoutSignatureV2.isSignatureInvalid(BEFORE_CUTOFF));
        assertArrayEquals(request.serializeForHash(), withoutSignatureV2.serializeForHash());
    }

    // Signature V1 of a remove request does not cover the creation date. The time of the node decides, so an earlier
    // creation date, which a relay can set, does not make the request valid.
    @Test
    void removeRequestWithoutSignatureV2IsInvalidFromTheCutoffDateOnForAnyCreationDate()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        RemoveAuthenticatedDataRequest withEarlierCreationDate = RemoveAuthenticatedDataRequest.fromProto(
                request.toValueProto(false).toBuilder()
                        .clearSignatureV2()
                        .setCreated(request.getCreated() - 1000)
                        .build());

        assertFalse(withEarlierCreationDate.isSignatureV1Invalid());
        assertTrue(withEarlierCreationDate.isRequiredSignatureV2Missing(AT_CUTOFF));
        assertTrue(withEarlierCreationDate.isSignatureInvalid(AT_CUTOFF));
    }

    // Signature V1 of a remove request does not cover the public key. A relay could replace the key with another
    // encoding of the same key, and the request would get another storage key. Only the canonical encoding is valid.
    @Test
    void removeRequestWithOtherEncodingOfOwnerKeyIsRejected() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto = request.toValueProto(false).toBuilder()
                .clearSignatureV2()
                .setOwnerPublicKeyBytes(EcPublicKeyVariants.toCompressed(ownerKeyPair.getPublic()))
                .build();

        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> RemoveAuthenticatedDataRequest.fromProto(proto)).getCause());
    }

    // fromProto parses the key before verify() checks the request. A key with explicit curve parameters is a valid
    // encoding of the same key, but it exceeds the size limit of a key. The work of the key parser grows with the size
    // of such a key, so the request is rejected by the size of the key, before the parser runs.
    @Test
    void removeRequestWithOversizedOwnerKeyIsRejectedBeforeTheKeyParserRuns() throws GeneralSecurityException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto = request.toValueProto(false).toBuilder()
                .setOwnerPublicKeyBytes(EcPublicKeyVariants.toExplicitParameters(ownerKeyPair.getPublic()))
                .build();

        assertInstanceOf(InvalidKeySpecException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> RemoveAuthenticatedDataRequest.fromProto(proto)).getCause());
    }

    // Nodes of this version sign in low-S form. A relay can create the high-S form of a signature without the private
    // key. Signature V2 accepts only the low-S form, signature V1 accepts both (nodes of earlier versions create both).
    @Test
    void addRequestWithHighSSignatureV2IsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.AddAuthenticatedDataRequest proto = request.toValueProto(false);

        AddAuthenticatedDataRequest highSSignatureV2 = fromProto(proto.toBuilder()
                .setSignatureV2(EcdsaSignatureVariants.toOtherS(proto.getSignatureV2(), ownerKeyPair.getPublic()))
                .build(), request);
        assertTrue(highSSignatureV2.isSignatureInvalid());

        AddAuthenticatedDataRequest highSSignatureV1 = fromProto(proto.toBuilder()
                .clearSignatureV2()
                .clearVersion()
                .setSignature(EcdsaSignatureVariants.toOtherS(proto.getSignature(), ownerKeyPair.getPublic()))
                .build(), request);
        assertFalse(highSSignatureV1.isSignatureInvalid(BEFORE_CUTOFF));
    }

    @Test
    void removeRequestWithHighSSignatureV2IsRejected() throws GeneralSecurityException, UnresolvableProtobufException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto = request.toValueProto(false);

        assertTrue(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder()
                        .setSignatureV2(EcdsaSignatureVariants.toOtherS(proto.getSignatureV2(), ownerKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid());

        assertFalse(RemoveAuthenticatedDataRequest.fromProto(proto.toBuilder()
                        .clearSignatureV2()
                        .setSignature(EcdsaSignatureVariants.toOtherS(proto.getSignature(), ownerKeyPair.getPublic()))
                        .build())
                .isSignatureInvalid(BEFORE_CUTOFF));
    }

    @Test
    void refreshRequestHasSameProofOfWorkPayloadWithoutSignatureV2()
            throws GeneralSecurityException, UnresolvableProtobufException {
        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);

        RefreshAuthenticatedDataRequest withoutSignatureV2 = RefreshAuthenticatedDataRequest.fromProto(
                request.toValueProto(false).toBuilder().clearSignatureV2().build());

        assertArrayEquals(request.serializeForHash(), withoutSignatureV2.serializeForHash());
    }

    // className is a field which the sender sets. It must have the format of the class name of MetaData, so a value
    // of any length or with line breaks fails at parsing (see docs/specifications/network/storage-metadata.md).
    @Test
    void removeRequestWithInvalidClassNameIsRejectedAtParsing() throws GeneralSecurityException {
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto =
                RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair).toValueProto(false);

        for (String className : INVALID_CLASS_NAMES) {
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(UnresolvableProtobufException.class,
                            () -> RemoveAuthenticatedDataRequest.fromProto(
                                    proto.toBuilder().setClassName(className).build())).getCause());
        }
    }

    @Test
    void refreshRequestWithInvalidClassNameIsRejectedAtParsing() throws GeneralSecurityException {
        bisq.network.protobuf.RefreshAuthenticatedDataRequest proto =
                RefreshAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair).toValueProto(false);

        for (String className : INVALID_CLASS_NAMES) {
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(UnresolvableProtobufException.class,
                            () -> RefreshAuthenticatedDataRequest.fromProto(
                                    proto.toBuilder().setClassName(className).build())).getCause());
        }
    }

    // The preimage of signature V2 is a new message. Its layout is written here independently with protobuf's
    // CodedOutputStream.
    @Test
    void removeRequestSignatureV2PreimageHasFixedLayout() throws GeneralSecurityException, IOException {
        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.RemoveAuthenticatedDataRequest proto = request.toValueProto(false);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(expected);
        output.writeString(1, "bisq.RemoveAuthenticatedDataRequest.signatureV2");
        output.writeSInt32(2, 2);
        output.writeString(3, STORE_KEY);
        output.writeSInt64(4, proto.getCreated());
        output.writeSInt32(5, proto.getSequenceNumber());
        output.writeBytes(6, proto.getHash());
        output.writeBytes(7, proto.getOwnerPublicKeyBytes());
        output.writeBytes(8, proto.getSignature());
        output.flush();

        assertArrayEquals(expected.toByteArray(), request.getPreImageV2());
    }

    @Test
    void addRequestSignatureV2PreimageHasFixedLayout() throws GeneralSecurityException, IOException {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, ownerKeyPair);
        bisq.network.protobuf.AddAuthenticatedDataRequest proto = request.toValueProto(false);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(expected);
        output.writeString(1, "bisq.AddAuthenticatedDataRequest.signatureV2");
        output.writeSInt32(2, 2);
        // The sequential data contains no field excluded from the hash, so its SIGNATURE encoding equals serialize()
        output.writeBytes(3, ByteString.copyFrom(request.getAuthenticatedSequentialData().serialize()));
        output.writeBytes(4, proto.getOwnerPublicKeyBytes());
        output.writeBytes(5, proto.getSignature());
        output.flush();

        assertArrayEquals(expected.toByteArray(), request.getPreImageV2());
    }

    // Like AddAuthenticatedDataRequest.fromProto, but with the sequential data of the given request. The test data has
    // no resolver, so it cannot be parsed from its Any.
    private static AddAuthenticatedDataRequest fromProto(bisq.network.protobuf.AddAuthenticatedDataRequest proto,
                                                         AddAuthenticatedDataRequest request)
            throws UnresolvableProtobufException {
        return new AddAuthenticatedDataRequest(request.getAuthenticatedSequentialData(),
                proto.getSignature().toByteArray(),
                proto.getOwnerPublicKeyBytes().toByteArray(),
                request.getSignaturePublicKey(),
                proto.getSignatureV2().toByteArray(),
                proto.getVersion());
    }
}
