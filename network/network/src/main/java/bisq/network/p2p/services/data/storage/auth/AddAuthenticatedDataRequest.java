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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.SignatureValidation;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
import bisq.network.protobuf.DataRequest;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.EcPublicKeyValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Request for adding authenticated data to the data storage.
 * A signature of the authenticatedSequentialData is verified with the given public key.
 * The data gets compared with existing map entries and need to be deterministic.
 */
@Slf4j
public final class AddAuthenticatedDataRequest implements AuthenticatedDataRequest, AddDataRequest {
    private final static int VERSION = 2;

    public static AddAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                   AuthenticatedData authenticatedData,
                                                   KeyPair keyPair)
            throws GeneralSecurityException {
        return from(store, authenticatedData, keyPair, 1);
    }

    // The sequence number is at least minSequenceNumber (see AuthorizedDataPublisherMigration)
    static AddAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                            AuthenticatedData authenticatedData,
                                            KeyPair keyPair,
                                            int minSequenceNumber)
            throws GeneralSecurityException {
        int version = VERSION;
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        ByteArray storageKey = getStorageKey(getAuthenticatedDataHash(authenticatedData), publicKeyEncoded);
        byte[] pubKeyHash = DigestUtil.hash(publicKeyEncoded);
        int sequenceNumber = Math.max(store.getNextSequenceNumber(storageKey), minSequenceNumber);
        long created = System.currentTimeMillis();
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(authenticatedData,
                sequenceNumber,
                pubKeyHash,
                created);
        byte[] signatureV1 = SignatureUtil.sign(sequentialData.serializeForHash(), keyPair.getPrivate());

        AddAuthenticatedDataRequest requestToSign = AddAuthenticatedDataRequest.withoutSignatureV2(sequentialData,
                signatureV1,
                publicKeyEncoded,
                publicKey,
                version);
        byte[] signatureV2 = SignatureUtil.sign(requestToSign.getPreImageV2(), keyPair.getPrivate());

        return new AddAuthenticatedDataRequest(sequentialData,
                signatureV1,
                publicKeyEncoded,
                publicKey,
                signatureV2,
                version);
    }

    private static AddAuthenticatedDataRequest withoutSignatureV2(AuthenticatedSequentialData sequentialData,
                                                                  byte[] signatureV1,
                                                                  byte[] publicKeyEncoded,
                                                                  PublicKey publicKey,
                                                                  int version) {
        return new AddAuthenticatedDataRequest(sequentialData,
                signatureV1,
                publicKeyEncoded,
                publicKey,
                new byte[]{},
                version);
    }

    public static AddAuthenticatedDataRequest from(AddAuthenticatedDataRequest requestFromMap,
                                                   RefreshAuthenticatedDataRequest refreshRequest) {
        long newTimestamp = refreshRequest.getCreated();
        int newSequenceNumber = refreshRequest.getSequenceNumber();
        byte[] newSignatureV1 = refreshRequest.getSignatureV1();
        byte[] newSignatureV2 = refreshRequest.getSignatureV2();

        AuthenticatedSequentialData sequentialDataFromMap = requestFromMap.getAuthenticatedSequentialData();
        AuthenticatedSequentialData newSequentialData = new AuthenticatedSequentialData(
                sequentialDataFromMap.getAuthenticatedData(),
                newSequenceNumber,
                sequentialDataFromMap.getPubKeyHash(),
                newTimestamp);
        return new AddAuthenticatedDataRequest(newSequentialData,
                newSignatureV1,
                refreshRequest.getSignaturePubKeyEncoded(),
                refreshRequest.getSignaturePublicKey(),
                newSignatureV2,
                // The owner signs an AddAuthenticatedDataRequest with this version and copies the version into the
                // refresh request (see RefreshAuthenticatedDataRequest.from). Signature V2 covers it.
                refreshRequest.getVersion());
    }

    @Getter
    private final AuthenticatedSequentialData authenticatedSequentialData;
    private final byte[] signature;
    private final byte[] ownerPublicKeyBytes;
    private final transient PublicKey ownerPublicKey;
    // Added in v2.1.14. Empty if received from nodes of earlier versions. Excluded from the hash, so that the proof of
    // work payload stays the same as that of nodes of earlier versions.
    @ExcludeForHash
    private final byte[] signatureV2;
    // Added in v2.1.14. 0 if received from nodes of earlier versions. Version 2 is the first version with signature V2.
    // Excluded from the hash for the same reason as signatureV2.
    @ExcludeForHash
    private final int version;

    public AddAuthenticatedDataRequest(AuthenticatedSequentialData authenticatedSequentialData,
                                       byte[] signature,
                                       byte[] ownerPublicKeyBytes,
                                       PublicKey ownerPublicKey,
                                       byte[] signatureV2,
                                       int version) {
        this.authenticatedSequentialData = authenticatedSequentialData;
        this.signature = signature;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.signatureV2 = signatureV2;
        this.version = version;

        verify();
    }

    @Override
    public void verify() {
        EcPublicKeyValidation.validate(ownerPublicKeyBytes);
        // The publisher hash must name the key which signs the request (see docs/specifications/network/storage-ownership.md).
        checkArgument(Arrays.equals(authenticatedSequentialData.getPubKeyHash(), DigestUtil.hash(ownerPublicKeyBytes)),
                "pubKeyHash must match the hash of ownerPublicKeyBytes");
        SignatureValidation.validateSignatures(signature, signatureV2);
        SignatureValidation.validateVersionOfSignatureV2(signatureV2, version);
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setAddAuthenticatedDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.AddAuthenticatedDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AddAuthenticatedDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AddAuthenticatedDataRequest.newBuilder()
                .setAuthenticatedSequentialData(authenticatedSequentialData.toProto(serializeForHash))
                .setSignature(ByteString.copyFrom(signature))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                .setSignatureV2(ByteString.copyFrom(signatureV2))
                .setVersion(version);
    }

    public static AddAuthenticatedDataRequest fromProto(bisq.network.protobuf.AddAuthenticatedDataRequest proto)
            throws UnresolvableProtobufException {
        try {
            byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            return new AddAuthenticatedDataRequest(
                    AuthenticatedSequentialData.fromProto(proto.getAuthenticatedSequentialData()),
                    proto.getSignature().toByteArray(),
                    ownerPublicKeyBytes,
                    ownerPublicKey,
                    proto.getSignatureV2().toByteArray(),
                    proto.getVersion()
            );
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    // Overrides Proto.getVersion(). Proto uses it to resolve @ExcludeForHash(excludeOnlyInVersions = ...), so it
    // determines the result of serializeForHash(), which is the payload of the proof of work. It must return the
    // version of the message.
    @Override
    public int getVersion() {
        return version;
    }


    /* --------------------------------------------------------------------- */
    // Canonical implementation
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<AddAuthenticatedDataRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(1,
                    CanonicalSchema.<AddAuthenticatedDataRequest>newBuilder()
                            .canonical(1, AddAuthenticatedDataRequest::getAuthenticatedSequentialData)
                            .bytes(2, request -> request.signature)
                            .bytes(3, request -> request.ownerPublicKeyBytes)
                            .bytes(4, request -> request.signatureV2)
                            .sint32(5, request -> request.version));

    public static final CanonicalSchema<AddAuthenticatedDataRequest> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<AddAuthenticatedDataRequest> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: signatureV2 (field 4) and
    // version (field 5).
    private static CanonicalSchema<AddAuthenticatedDataRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(1,
                CanonicalSchema.<AddAuthenticatedDataRequest>newBuilder()
                        .canonical(1, AddAuthenticatedDataRequest::getAuthenticatedSequentialData)
                        .bytes(2, request -> request.signature)
                        .bytes(3, request -> request.ownerPublicKeyBytes));
    }

    // The preimage of signature V2. It is a new message, not the protobuf message: a domain separation tag, then every
    // field except signatureV2 itself. The @ExcludeForHash annotations do not apply. The nested objects receive the
    // SIGNATURE purpose, so their own @ExcludeForHash fields are covered as well.
    public static final CanonicalSchema<AddAuthenticatedDataRequest> SIGNATURE_V2_SCHEMA =
            CanonicalSchema.<AddAuthenticatedDataRequest>newBuilder()
                    .string(1, ignore -> "bisq.AddAuthenticatedDataRequest.signatureV2")
                    .sint32(2, request -> request.version)
                    .canonical(3, AddAuthenticatedDataRequest::getAuthenticatedSequentialData)
                    .bytes(4, request -> request.ownerPublicKeyBytes)
                    .bytes(5, request -> request.signature)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AddAuthenticatedDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_V2_SCHEMA;
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for AddAuthenticatedDataRequest");
            case PROOF_OF_WORK -> PROOF_OF_WORK_SCHEMA;
            case TRADE_CONTRACT -> TRADE_CONTRACT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKeyV1() {
        return new ByteArray(DigestUtil.hash(authenticatedSequentialData.getAuthenticatedData().serializeForHash()));
    }

    // The data hash must be the one which remove and refresh requests carry (see getAuthenticatedDataHash).
    @Override
    public byte[] getStorageKeyPreImageV2() {
        return StorageKeyV2.createPreImage(StorageKeyV2.AUTHENTICATED_DATA_TAG,
                getAuthenticatedDataHash(authenticatedSequentialData.getAuthenticatedData()),
                ownerPublicKeyBytes);
    }

    // The hash which remove and refresh requests carry. The STORAGE_KEY encoding of AuthenticatedData equals its
    // serializeForHash(), so nodes of earlier versions compute the same hash.
    static byte[] getAuthenticatedDataHash(AuthenticatedData authenticatedData) {
        return DigestUtil.hash(authenticatedData.encodeCanonical(EncodingPurpose.STORAGE_KEY));
    }

    static ByteArray getStorageKey(byte[] authenticatedDataHash, byte[] ownerPublicKeyBytes) {
        return StorageKeyV2.create(StorageKeyV2.AUTHENTICATED_DATA_TAG, authenticatedDataHash, ownerPublicKeyBytes);
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getPreImageV1() {
        return authenticatedSequentialData.serializeForHash();
    }

    @Override
    public byte[] getPreImageV2() {
        return encodeCanonical(EncodingPurpose.SIGNATURE);
    }

    @Override
    public PublicKey getSignaturePublicKey() {
        return ownerPublicKey;
    }

    @Override
    public byte[] getSignatureV1() {
        return signature;
    }

    @Override
    public byte[] getSignatureV2() {
        return signatureV2;
    }


    /* --------------------------------------------------------------------- */
    // SignaturePubKeyHashVerifier implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getSignaturePubKeyEncoded() {
        return ownerPublicKeyBytes;
    }


    /* --------------------------------------------------------------------- */
    // Sequential implementation
    /* --------------------------------------------------------------------- */

    @Override
    public int getSequenceNumber() {
        return authenticatedSequentialData.getSequenceNumber();
    }


    /* --------------------------------------------------------------------- */
    // Expirable implementation
    /* --------------------------------------------------------------------- */

    @Override
    public long getCreated() {
        return authenticatedSequentialData.getCreated();
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - getCreated()) > getTtl();
    }


    /* --------------------------------------------------------------------- */
    // StorageConstraint
    /* --------------------------------------------------------------------- */

    @Override
    public MetaData getMetaData() {
        return authenticatedSequentialData.getAuthenticatedData().getMetaData();
    }


    /* --------------------------------------------------------------------- */
    // Delegates
    /* --------------------------------------------------------------------- */

    public DistributedData getDistributedData() {
        return authenticatedSequentialData.getDistributedData();
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AddAuthenticatedDataRequest that)) return false;

        return version == that.version && authenticatedSequentialData.equals(that.authenticatedSequentialData) && Arrays.equals(signature, that.signature) && Arrays.equals(ownerPublicKeyBytes, that.ownerPublicKeyBytes) && Arrays.equals(signatureV2, that.signatureV2);
    }

    @Override
    public int hashCode() {
        int result = authenticatedSequentialData.hashCode();
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Arrays.hashCode(ownerPublicKeyBytes);
        result = 31 * result + Arrays.hashCode(signatureV2);
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "AddAuthenticatedDataRequest{" +
                "authenticatedSequentialData=" + authenticatedSequentialData +
                ", signature=" + Hex.encode(signature) +
                ", ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ", signatureV2=" + Hex.encode(signatureV2) +
                ", version=" + version +
                '}';
    }
}
