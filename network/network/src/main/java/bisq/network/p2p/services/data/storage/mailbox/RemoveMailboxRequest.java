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

package bisq.network.p2p.services.data.storage.mailbox;

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
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

// Data size about 200-250 bytes
@Slf4j
public final class RemoveMailboxRequest implements MailboxRequest, RemoveDataRequest {
    private static final int VERSION = 1;

    public static RemoveMailboxRequest from(MailboxData mailboxData, KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        long created = System.currentTimeMillis();
        PublicKey publicKey = receiverKeyPair.getPublic();
        byte[] publicKeyEncoded = publicKey.getEncoded();
        byte[] hash = DigestUtil.hash(mailboxData.serializeForHash());
        byte[] signature = SignatureUtil.sign(hash, receiverKeyPair.getPrivate());
        return new RemoveMailboxRequest(VERSION,
                mailboxData.getMetaData(),
                hash,
                publicKeyEncoded,
                publicKey,
                signature,
                created,
                mailboxData.getClassName());
    }

    // Deprecated, replaced by className. Still sent, because nodes of earlier versions use it.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;
    private final byte[] hash;
    private final byte[] receiverPublicKeyBytes;
    private final byte[] signature;
    private final long created;
    @ExcludeForHash
    private final int version;
    private final transient PublicKey receiverPublicKey;
    // Simple class name of the removed MailboxMessage. It selects the store.
    // Excluded from the hash, so it does not change the proof of work payload.
    @ExcludeForHash
    private final String className;
    @Setter
    private transient Optional<MetaData> metaDataFromDistributedData = Optional.empty();

    // Receiver is owner for remove request
    private RemoveMailboxRequest(int version,
                                 MetaData metaData,
                                 byte[] hash,
                                 byte[] receiverPublicKeyBytes,
                                 PublicKey receiverPublicKey,
                                 byte[] signature,
                                 long created,
                                 String className) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.receiverPublicKeyBytes = receiverPublicKeyBytes;
        this.receiverPublicKey = receiverPublicKey;
        this.signature = signature;
        this.created = created;
        this.className = className;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateECPubKey(receiverPublicKeyBytes);
        NetworkDataValidation.validateDate(created);
        NetworkDataValidation.validateECSignature(signature);
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public bisq.network.protobuf.DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setRemoveMailboxRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.RemoveMailboxRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.RemoveMailboxRequest.Builder getValueBuilder(boolean serializeForHash) {
        //noinspection deprecation
        return bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setHash(ByteString.copyFrom(hash))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(receiverPublicKeyBytes))
                .setSignature(ByteString.copyFrom(signature))
                .setCreated(created)
                .setClassName(className);
    }

    public static RemoveMailboxRequest fromProto(bisq.network.protobuf.RemoveMailboxRequest proto) {
        byte[] receiverPublicKeyBytes = proto.getReceiverPublicKeyBytes().toByteArray();
        try {
            PublicKey receiverPublicKey = KeyGeneration.generatePublic(receiverPublicKeyBytes);
            //noinspection deprecation
            MetaData metaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ? metaData.getClassName() : proto.getClassName();
            return new RemoveMailboxRequest(
                    proto.getVersion(),
                    metaData,
                    proto.getHash().toByteArray(),
                    receiverPublicKeyBytes,
                    receiverPublicKey,
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
    public static final CanonicalSchema<RemoveMailboxRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(5,
                    CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                            .canonical(1, request -> request.metaData)
                            .bytes(2, request -> request.hash)
                            .bytes(3, request -> request.receiverPublicKeyBytes)
                            .bytes(4, request -> request.signature)
                            .sint64(5, request -> request.created)
                            .sint32(6, request -> request.version)
                            .string(7, request -> request.className));
    public static final CanonicalSchema<RemoveMailboxRequest> SIGNATURE_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<RemoveMailboxRequest> STORAGE_KEY_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 1) in versions
    // 1 to 3, version (field 6) and className (field 7).
    private static CanonicalSchema<RemoveMailboxRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(5,
                CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                        // metaData is annotated with @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
                        .canonical(1, request -> isMetaDataExcludedForHash(request.version) ? null : request.metaData)
                        .bytes(2, request -> request.hash)
                        .bytes(3, request -> request.receiverPublicKeyBytes)
                        .bytes(4, request -> request.signature)
                        .sint64(5, request -> request.created));
    }

    // Same versions as the @ExcludeForHash annotation of metaData
    private static boolean isMetaDataExcludedForHash(int version) {
        return version == 1 || version == 2 || version == 3;
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<RemoveMailboxRequest> schema = switch (encodingPurpose) {
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
        return MathUtils.bounded(0.1, 0.3, getMetaData().getCostFactor());
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
        return receiverPublicKey;
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
        return receiverPublicKeyBytes;
    }


    /* --------------------------------------------------------------------- */
    // Sequential implementation
    /* --------------------------------------------------------------------- */

    @Override
    public int getSequenceNumber() {
        // Use max value for sequence number so that no other addData call is permitted.
        return Integer.MAX_VALUE;
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
        return (System.currentTimeMillis() - getCreated()) > Math.min(MailboxData.MAX_TLL, getTtl());
    }


    /* --------------------------------------------------------------------- */
    // StorageConstraint
    /* --------------------------------------------------------------------- */

    @Override
    public MetaData getMetaData() {
        return metaDataFromDistributedData.orElse(metaData);
    }

    public MetaData getMetaDataFromProto() {
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
    public String toString() {
        return "RemoveMailboxRequest{" +
                "metaData=" + metaData +
                ", metaDataFromDistributedData=" + metaDataFromDistributedData +
                ", version=" + version +
                ", hash=" + Hex.encode(hash) +
                ", receiverPublicKeyBytes=" + Hex.encode(receiverPublicKeyBytes) +
                ", signature=" + Hex.encode(signature) +
                ", created=" + new Date(created) + " (" + created + ")" +
                ", receiverPublicKey=" + receiverPublicKey +
                ", className=" + className +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RemoveMailboxRequest that)) return false;

        return created == that.created &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(receiverPublicKeyBytes, that.receiverPublicKeyBytes) &&
                Arrays.equals(signature, that.signature) &&
                Objects.equals(receiverPublicKey, that.receiverPublicKey) &&
                Objects.equals(metaDataFromDistributedData, that.metaDataFromDistributedData);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(receiverPublicKeyBytes);
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + Objects.hashCode(receiverPublicKey);
        result = 31 * result + Objects.hashCode(metaDataFromDistributedData);
        return result;
    }
}
