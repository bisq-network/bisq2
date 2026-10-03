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
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.SignatureValidation;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
import bisq.security.keys.EcPublicKeyValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class RefreshAuthenticatedDataRequest implements AuthenticatedDataRequest {

    public static RefreshAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                       AuthenticatedData authenticatedData,
                                                       KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] authenticatedDataHash = AddAuthenticatedDataRequest.getAuthenticatedDataHash(authenticatedData);

        // We use the AddAuthenticatedDataRequest for signing and then copy the data we need for RefreshAuthenticatedDataRequest.
        // The version is copied as well: the receiver rebuilds the AddAuthenticatedDataRequest with it, and signature V2
        // covers it.
        AddAuthenticatedDataRequest addAuthenticatedDataRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
        return new RefreshAuthenticatedDataRequest(addAuthenticatedDataRequest.getVersion(),
                addAuthenticatedDataRequest.getMetaData(),
                authenticatedDataHash,
                addAuthenticatedDataRequest.getSignaturePubKeyEncoded(),
                addAuthenticatedDataRequest.getSignaturePublicKey(),
                addAuthenticatedDataRequest.getSequenceNumber(),
                addAuthenticatedDataRequest.getSignatureV1(),
                addAuthenticatedDataRequest.getCreated(),
                addAuthenticatedDataRequest.getClassName(),
                addAuthenticatedDataRequest.getSignatureV2());
    }


    // Deprecated. The class name selects the MetaData (see getMetaData()); the field is still sent only because nodes of
    // earlier versions use it. fromProto replaces the received value by the registered MetaData of the class name, so
    // its className always equals className. No hash, storage key, proof of work or signature contains it.
    @ExcludeForHash
    private final MetaData metaData;
    private final byte[] hash; // of authenticatedData
    private final byte[] ownerPublicKeyBytes; // 442 bytes
    private final transient PublicKey ownerPublicKey;
    private final int sequenceNumber;
    private final byte[] signature;         // 47 bytes
    @ExcludeForHash
    private final int version;
    private final long created;
    // Simple class name of the refreshed DistributedData. It selects the store.
    // Excluded from the hash, so it does not change the proof of work payload.
    @ExcludeForHash
    private final String className;
    // Added in v2.1.14. Empty if received from nodes of earlier versions. Excluded from the hash, so that the proof of
    // work payload stays the same as that of nodes of earlier versions.
    @ExcludeForHash
    private final byte[] signatureV2;

    private RefreshAuthenticatedDataRequest(int version,
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
        NetworkDataValidation.validateDate(created);
        SignatureValidation.validateSignatures(signature, signatureV2);
        MetaData.validateClassName(className);
        // Nodes of earlier versions select the store by the className of metaData
        checkArgument(metaData.getClassName().equals(className), "className of metaData must equal className");
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public bisq.network.protobuf.DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setRefreshAuthenticatedDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.RefreshAuthenticatedDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.RefreshAuthenticatedDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        //noinspection deprecation
        return bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
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

    public static RefreshAuthenticatedDataRequest fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest proto)
            throws UnresolvableProtobufException {
        try {
            byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            //noinspection deprecation
            MetaData receivedMetaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ?
                    receivedMetaData.getClassName() :
                    proto.getClassName();
            // Nodes of earlier versions use metaData. Send them the registered values, not those which a sender or
            // relay chose. serializeForHash() never contains metaData, so the proof of work payload does not change.
            // The signature and the storage key never contain it either.
            MetaData metaData = StorableTypesRegistry.getMetaDataForEarlierVersions(className, receivedMetaData);
            return new RefreshAuthenticatedDataRequest(
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
    // Canonical
    /* --------------------------------------------------------------------- */

    // Contains metaData, version, className and signatureV2, which serializeForHash() omits, as the default encoding
    // contains all non-transient fields.
    public static final CanonicalSchema<RefreshAuthenticatedDataRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(3,
                    CanonicalSchema.<RefreshAuthenticatedDataRequest>newBuilder()
                            .canonical(1, request -> request.metaData)
                            .bytes(2, request -> request.hash)
                            .bytes(3, request -> request.ownerPublicKeyBytes)
                            .sint32(4, request -> request.sequenceNumber)
                            .bytes(5, request -> request.signature)
                            .sint32(6, request -> request.version)
                            .sint64(7, request -> request.created)
                            .string(8, request -> request.className)
                            .bytes(9, request -> request.signatureV2));

    public static final CanonicalSchema<RefreshAuthenticatedDataRequest> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<RefreshAuthenticatedDataRequest> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 1), version
    // (field 6), className (field 8) and signatureV2 (field 9).
    private static CanonicalSchema<RefreshAuthenticatedDataRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(3,
                CanonicalSchema.<RefreshAuthenticatedDataRequest>newBuilder()
                        .bytes(2, request -> request.hash)
                        .bytes(3, request -> request.ownerPublicKeyBytes)
                        .sint32(4, request -> request.sequenceNumber)
                        .bytes(5, request -> request.signature)
                        .sint64(7, request -> request.created));
    }

    // No signature covers the refresh request itself: both signatures cover the AddAuthenticatedDataRequest which the
    // receiver rebuilds (see getPreImageV1()). The SIGNATURE purpose therefore has no own schema and uses the default
    // schema.
    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<RefreshAuthenticatedDataRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for RefreshAuthenticatedDataRequest");
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

    // The signature is created over the AddAuthenticatedDataRequest which the refresh produces (see from()).
    // It can only be verified on that request, which the receiver rebuilds from its stored entry with
    // AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest). The refresh request alone has no pre-image.
    @Override
    public byte[] getPreImageV1() {
        throw new UnsupportedOperationException("RefreshAuthenticatedDataRequest has no pre-image. " +
                "Verify the AddAuthenticatedDataRequest created with AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest).");
    }

    @Override
    public byte[] getPreImageV2() {
        throw new UnsupportedOperationException("RefreshAuthenticatedDataRequest has no pre-image. " +
                "Verify the AddAuthenticatedDataRequest created with AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest).");
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

    @Override
    public boolean isSignatureInvalid(long now) {
        throw new UnsupportedOperationException("RefreshAuthenticatedDataRequest cannot be verified alone. " +
                "Verify the AddAuthenticatedDataRequest created with AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest).");
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

    // The MetaData of the refreshed type, from the StorableTypesRegistry.
    // Throws if the type is not registered. A store exists only for registered types.
    @Override
    public MetaData getMetaData() {
        return StorableTypesRegistry.getMetaData(getClassName());
    }

    // The MetaData which this message carries in the deprecated field metaData for nodes of earlier versions. This node
    // does not use it for its own decisions.
    public MetaData getMetaDataForEarlierVersions() {
        return metaData;
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
        if (!(o instanceof RefreshAuthenticatedDataRequest that)) return false;

        return sequenceNumber == that.sequenceNumber &&
                version == that.version &&
                created == that.created &&
                metaData.equals(that.metaData) &&
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
        result = 31 * result + version;
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + className.hashCode();
        result = 31 * result + Arrays.hashCode(signatureV2);
        return result;
    }

    @Override
    public String toString() {
        return "RefreshAuthenticatedDataRequest{" +
                "metaData=" + metaData +
                ", hash=" + Hex.encode(hash) +
                ", ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ", sequenceNumber=" + sequenceNumber +
                ", signature=" + Hex.encode(signature) +
                ", version=" + version +
                ", created=" + created +
                ", className='" + className + '\'' +
                ", signatureV2=" + Hex.encode(signatureV2) +
                '}';
    }
}
