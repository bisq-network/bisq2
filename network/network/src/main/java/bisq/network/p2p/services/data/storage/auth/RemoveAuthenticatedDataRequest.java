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
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.SignatureValidation;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
import bisq.network.protobuf.DataRequest;
import bisq.security.SignatureUtil;
import bisq.security.keys.EcPublicKeyValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

// Data size about 200-250 bytes
@Slf4j
public final class RemoveAuthenticatedDataRequest implements AuthenticatedDataRequest, RemoveDataRequest {
    private static final int VERSION = 2;

    public static RemoveAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                      AuthenticatedData authenticatedData,
                                                      KeyPair keyPair)
            throws GeneralSecurityException {
        int version = VERSION;
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        long created = System.currentTimeMillis();
        MetaData metaData = authenticatedData.getMetaData();
        String className = metaData.getClassName();
        byte[] authenticatedDataHash = AddAuthenticatedDataRequest.getAuthenticatedDataHash(authenticatedData);
        ByteArray storageKey = AddAuthenticatedDataRequest.getStorageKey(authenticatedDataHash, publicKeyEncoded);
        int sequenceNumber = store.getNextSequenceNumber(storageKey);

        byte[] signatureV1 = SignatureUtil.sign(authenticatedDataHash, keyPair.getPrivate());

        RemoveAuthenticatedDataRequest requestToSign = RemoveAuthenticatedDataRequest.withoutSignatureV2(version,
                metaData,
                authenticatedDataHash,
                publicKeyEncoded,
                publicKey,
                sequenceNumber,
                signatureV1,
                created,
                className);
        byte[] signatureV2 = SignatureUtil.sign(requestToSign.getPreImageV2(), keyPair.getPrivate());

        return new RemoveAuthenticatedDataRequest(version,
                metaData,
                authenticatedDataHash,
                publicKeyEncoded,
                publicKey,
                sequenceNumber,
                signatureV1,
                created,
                className,
                signatureV2);
    }

    private static RemoveAuthenticatedDataRequest withoutSignatureV2(int version,
                                                                     MetaData metaData,
                                                                     byte[] authenticatedDataHash,
                                                                     byte[] publicKeyEncoded,
                                                                     PublicKey publicKey,
                                                                     int sequenceNumber,
                                                                     byte[] signatureV1,
                                                                     long created,
                                                                     String className) {
        return new RemoveAuthenticatedDataRequest(version,
                metaData,
                authenticatedDataHash,
                publicKeyEncoded,
                publicKey,
                sequenceNumber,
                signatureV1,
                created,
                className,
                new byte[]{});
    }

    // Deprecated, replaced by className. Still sent, because nodes of earlier versions use it. This node sends its
    // registered MetaData of the class name (see fromProto) and never uses the field for a decision (see
    // getMetaData()).
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;
    private final byte[] hash;
    private final byte[] ownerPublicKeyBytes;
    private final transient PublicKey ownerPublicKey;
    private final int sequenceNumber;
    private final byte[] signature;
    private final long created;
    @ExcludeForHash
    private final int version;
    // Simple class name of the removed DistributedData. It selects the store.
    // Excluded from the hash, so it does not change the proof of work payload. Signature V2 covers it.
    @ExcludeForHash
    private final String className;
    // Added in v2.1.14. Empty if received from nodes of earlier versions. Excluded from the hash, so that the proof of
    // work payload stays the same as that of nodes of earlier versions.
    @ExcludeForHash
    private final byte[] signatureV2;

    private RemoveAuthenticatedDataRequest(int version,
                                           MetaData metaData,
                                           byte[] hash,
                                           byte[] ownerPublicKeyBytes,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature,
                                           long created,
                                           String className,
                                           byte[] signatureV2) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
        this.created = created;
        this.className = className;
        this.signatureV2 = signatureV2;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        EcPublicKeyValidation.validate(ownerPublicKeyBytes);
        checkArgument(sequenceNumber > 0,
                "sequenceNumber must be positive. sequenceNumber=" + sequenceNumber);
        NetworkDataValidation.validateDate(created);
        SignatureValidation.validateSignatures(signature, signatureV2);
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setRemoveAuthenticatedDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.RemoveAuthenticatedDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.RemoveAuthenticatedDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        //noinspection deprecation
        return bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setHash(ByteString.copyFrom(hash))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                .setSequenceNumber(sequenceNumber)
                .setSignature(ByteString.copyFrom(signature))
                .setCreated(created)
                .setClassName(className)
                .setSignatureV2(ByteString.copyFrom(signatureV2));
    }

    public static RemoveAuthenticatedDataRequest fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest proto)
            throws UnresolvableProtobufException {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            //noinspection deprecation
            MetaData receivedMetaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ?
                    receivedMetaData.getClassName() :
                    proto.getClassName();
            // Nodes of earlier versions use metaData. Send them the registered values, not those which a sender or
            // relay chose. In these versions serializeForHash() does not contain metaData, so the proof of work payload
            // does not change. The signature and the storage key never contain it: both use only the hash of the
            // stored data.
            MetaData metaData = isMetaDataExcludedForHash(proto.getVersion()) ?
                    StorableTypesRegistry.getMetaDataForEarlierVersions(className, receivedMetaData) :
                    receivedMetaData;
            return new RemoveAuthenticatedDataRequest(
                    proto.getVersion(),
                    metaData,
                    proto.getHash().toByteArray(),
                    ownerPublicKeyBytes,
                    ownerPublicKey,
                    proto.getSequenceNumber(),
                    proto.getSignature().toByteArray(),
                    proto.getCreated(),
                    className,
                    proto.getSignatureV2().toByteArray()
            );
        } catch (GeneralSecurityException e) {
            log.error("Failed to generate public key");
            throw new RuntimeException(e);
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
    // Canonical
    /* --------------------------------------------------------------------- */

    // Contains metaData, version, className and signatureV2, which serializeForHash() omits (metaData only in versions
    // 1 to 3), as the default encoding contains all non-transient fields.
    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(2,
                    CanonicalSchema.<RemoveAuthenticatedDataRequest>newBuilder()
                            .canonical(1, request -> request.metaData)
                            .bytes(2, request -> request.hash)
                            .bytes(3, request -> request.ownerPublicKeyBytes)
                            .sint32(4, request -> request.sequenceNumber)
                            .bytes(5, request -> request.signature)
                            .sint64(6, request -> request.created)
                            .sint32(7, request -> request.version)
                            .string(8, request -> request.className)
                            .bytes(9, request -> request.signatureV2));

    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 1) in versions
    // 1 to 3, version (field 7), className (field 8) and signatureV2 (field 9).
    private static CanonicalSchema<RemoveAuthenticatedDataRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(2,
                CanonicalSchema.<RemoveAuthenticatedDataRequest>newBuilder()
                        // metaData is annotated with @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
                        .canonical(1, request -> isMetaDataExcludedForHash(request.version) ? null : request.metaData)
                        .bytes(2, request -> request.hash)
                        .bytes(3, request -> request.ownerPublicKeyBytes)
                        .sint32(4, request -> request.sequenceNumber)
                        .bytes(5, request -> request.signature)
                        .sint64(6, request -> request.created));
    }

    // The preimage of signature V2. It is a new message, not the protobuf message: a domain separation tag, then every
    // field except signatureV2 itself and the deprecated metaData. No node uses the received metaData for a decision
    // (see getMetaData()). The @ExcludeForHash annotations do not apply.
    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> SIGNATURE_V2_SCHEMA =
            CanonicalSchema.<RemoveAuthenticatedDataRequest>newBuilder()
                    .string(1, ignore -> "bisq.RemoveAuthenticatedDataRequest.signatureV2")
                    .sint32(2, request -> request.version)
                    .string(3, request -> request.className)
                    .sint64(4, request -> request.created)
                    .sint32(5, request -> request.sequenceNumber)
                    .bytes(6, request -> request.hash)
                    .bytes(7, request -> request.ownerPublicKeyBytes)
                    .bytes(8, request -> request.signature)
                    .build();

    // Same versions as the @ExcludeForHash annotation of metaData
    private static boolean isMetaDataExcludedForHash(int version) {
        return version == 1 || version == 2 || version == 3;
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<RemoveAuthenticatedDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_V2_SCHEMA;
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for RemoveAuthenticatedDataRequest");
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
        return new ByteArray(hash);
    }

    @Override
    public byte[] getStorageKeyPreImageV2() {
        return StorageKeyV2.createPreImage(StorageKeyV2.AUTHENTICATED_DATA_TAG, hash, ownerPublicKeyBytes);
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getPreImageV1() {
        return hash;
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
        return sequenceNumber;
    }


    /* --------------------------------------------------------------------- */
    // Expirable implementation
    /* --------------------------------------------------------------------- */

    @Override
    public long getCreated() {
        return created;
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - getCreated()) > getTtl();
    }


    /* --------------------------------------------------------------------- */
    // StorageConstraint
    /* --------------------------------------------------------------------- */

    // The MetaData of the removed type, from the StorableTypesRegistry.
    // Throws if the type is not registered. StorageService does not accept such requests.
    @Override
    public MetaData getMetaData() {
        return StorableTypesRegistry.getMetaData(className);
    }

    // The MetaData which this message carries in the deprecated field metaData for nodes of earlier versions. This node
    // does not use it for its own decisions.
    public MetaData getMetaDataForEarlierVersions() {
        return metaData;
    }

    // In version 0 serializeForHash() contains the field metaData, so the proof of work payload covers it. The
    // signature covers only the hash of the stored data, so a relay can change the field, but then pays the proof of
    // work for the changed request.
    public boolean isMetaDataCoveredByHash() {
        return !isMetaDataExcludedForHash(version);
    }

    @Override
    public String getClassName() {
        return className;
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoveAuthenticatedDataRequest that)) return false;

        return sequenceNumber == that.sequenceNumber &&
                created == that.created &&
                version == that.version && metaData.equals(that.metaData) &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(ownerPublicKeyBytes, that.ownerPublicKeyBytes) &&
                Arrays.equals(signature, that.signature) &&
                className.equals(that.className) &&
                Arrays.equals(signatureV2, that.signatureV2);
    }

    @Override
    public int hashCode() {
        int result = metaData.hashCode();
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(ownerPublicKeyBytes);
        result = 31 * result + sequenceNumber;
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + version;
        result = 31 * result + className.hashCode();
        result = 31 * result + Arrays.hashCode(signatureV2);
        return result;
    }

    @Override
    public String toString() {
        return "RemoveAuthenticatedDataRequest{" +
                "metaData=" + metaData +
                ", hash=" + Hex.encode(hash) +
                ", ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ", sequenceNumber=" + sequenceNumber +
                ", signature=" + Hex.encode(signature) +
                ", created=" + created +
                ", version=" + version +
                ", className='" + className + '\'' +
                ", signatureV2=" + Hex.encode(signatureV2) +
                '}';
    }
}
