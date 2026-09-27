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
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.protobuf.DataRequest;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

// Data size about 200-250 bytes
@Slf4j
public final class RemoveAuthenticatedDataRequest implements AuthenticatedDataRequest, RemoveDataRequest {
    private static final int VERSION = 1;

    public static RemoveAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                      AuthenticatedData authenticatedData,
                                                      KeyPair keyPair)
            throws GeneralSecurityException {
        PublicKey publicKey = keyPair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        long created = System.currentTimeMillis();
        MetaData metaData = authenticatedData.getMetaData();
        byte[] hash = DigestUtil.hash(authenticatedData.serializeForHash());
        ByteArray storageKey = new ByteArray(hash);
        int sequenceNumber = store.getNextSequenceNumber(storageKey);
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        return new RemoveAuthenticatedDataRequest(VERSION,
                metaData,
                hash,
                publicKeyEncoded,
                publicKey,
                sequenceNumber,
                signature,
                created,
                authenticatedData.getClassName());
    }

    // Deprecated, replaced by className. Still sent, because nodes of earlier versions use it. This node only
    // serializes the received value again. It never uses it for a decision (see getMetaData()).
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
    // Excluded from the hash, so it does not change the proof of work payload.
    @ExcludeForHash
    private final String className;

    private RemoveAuthenticatedDataRequest(int version,
                                           MetaData metaData,
                                           byte[] hash,
                                           byte[] ownerPublicKeyBytes,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature,
                                           long created,
                                           String className) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
        this.created = created;
        this.className = className;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateECPubKey(ownerPublicKeyBytes);
        checkArgument(sequenceNumber > 0,
                "sequenceNumber must be positive. sequenceNumber=" + sequenceNumber);
        NetworkDataValidation.validateDate(created);
        NetworkDataValidation.validateECSignature(signature);
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
                .setClassName(className);
    }

    public static RemoveAuthenticatedDataRequest fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest proto) {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            //noinspection deprecation
            MetaData metaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ? metaData.getClassName() : proto.getClassName();
            return new RemoveAuthenticatedDataRequest(
                    proto.getVersion(),
                    metaData,
                    proto.getHash().toByteArray(),
                    ownerPublicKeyBytes,
                    ownerPublicKey,
                    proto.getSequenceNumber(),
                    proto.getSignature().toByteArray(),
                    proto.getCreated(),
                    className
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

    // Contains metaData, version and className, which serializeForHash() omits (metaData only in versions 1 to 3), as
    // the default encoding contains all non-transient fields.
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
                            .string(8, request -> request.className));
    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> SIGNATURE_SCHEMA =
            createSchemaForHash();
    public static final CanonicalSchema<RemoveAuthenticatedDataRequest> STORAGE_KEY_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 1) in versions
    // 1 to 3, version (field 7) and className (field 8).
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

    // Same versions as the @ExcludeForHash annotation of metaData
    private static boolean isMetaDataExcludedForHash(int version) {
        return version == 1 || version == 2 || version == 3;
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<RemoveAuthenticatedDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_SCHEMA;
            case STORAGE_KEY -> STORAGE_KEY_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }


    /* --------------------------------------------------------------------- */
    // CostBearingMessage implementation
    /* --------------------------------------------------------------------- */

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.1, 0.3, StorableTypesRegistry.getCostFactor(className));
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKey() {
        return new ByteArray(hash);
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getPreImage() {
        return hash;
    }

    @Override
    public PublicKey getSignaturePublicKey() {
        return ownerPublicKey;
    }

    @Override
    public byte[] getSignature() {
        return signature;
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

    @Override
    public String getClassName() {
        return className;
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    @Override
    public String toString() {
        return "RemoveAuthenticatedDataRequest{" +
                "\r\n     version=" + version +
                ",\r\n     metaData=" + metaData +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ",\r\n     ownerPublicKey=" + ownerPublicKey +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     created=" + new Date(created) + " (" + created + ")" +
                ",\r\n     className=" + className +
                "\r\n}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoveAuthenticatedDataRequest that)) return false;

        return sequenceNumber == that.sequenceNumber &&
                created == that.created &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(ownerPublicKeyBytes, that.ownerPublicKeyBytes) &&
                Objects.equals(ownerPublicKey, that.ownerPublicKey) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(ownerPublicKeyBytes);
        result = 31 * result + Objects.hashCode(ownerPublicKey);
        result = 31 * result + sequenceNumber;
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Long.hashCode(created);
        return result;
    }
}
