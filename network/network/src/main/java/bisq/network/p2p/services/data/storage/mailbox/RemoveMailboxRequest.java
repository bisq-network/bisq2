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
public final class RemoveMailboxRequest implements MailboxRequest, RemoveDataRequest {
    private static final int VERSION = 2;

    public static RemoveMailboxRequest from(MailboxData mailboxData, KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        long created = System.currentTimeMillis();
        int version = VERSION;
        MetaData metaData = mailboxData.getMetaData();
        String className = metaData.getClassName();
        PublicKey receiverPublicKey = receiverKeyPair.getPublic();
        byte[] receiverPublicKeyEncoded = receiverPublicKey.getEncoded();
        byte[] mailboxDataHash = AddMailboxRequest.getMailboxDataHash(mailboxData);

        byte[] signatureV1 = SignatureUtil.sign(mailboxDataHash, receiverKeyPair.getPrivate());

        RemoveMailboxRequest requestToSign = RemoveMailboxRequest.withoutSignatureV2(version,
                metaData,
                mailboxDataHash,
                receiverPublicKeyEncoded,
                receiverPublicKey,
                signatureV1,
                className,
                created);
        byte[] preImageV2 = requestToSign.getPreImageV2();
        byte[] signatureV2 = SignatureUtil.sign(preImageV2, receiverKeyPair.getPrivate());

        return new RemoveMailboxRequest(version,
                metaData,
                mailboxDataHash,
                receiverPublicKeyEncoded,
                receiverPublicKey,
                signatureV1,
                created,
                className,
                signatureV2);
    }

    private static RemoveMailboxRequest withoutSignatureV2(int version,
                                                           MetaData metaData,
                                                           byte[] mailboxDataHash,
                                                           byte[] receiverPublicKeyEncoded,
                                                           PublicKey receiverPublicKey,
                                                           byte[] signatureV1,
                                                           String className,
                                                           long created) {
        return new RemoveMailboxRequest(version,
                metaData,
                mailboxDataHash,
                receiverPublicKeyEncoded,
                receiverPublicKey,
                signatureV1,
                created,
                className,
                new byte[]{});
    }

    // Deprecated. The class name selects the MetaData (see getMetaData()); the field is still sent only because nodes of
    // earlier versions use it. fromProto replaces the received value by the registered MetaData of the class name, so
    // its className always equals className. No hash, storage key, proof of work or signature contains it.
    @ExcludeForHash
    private final MetaData metaData;
    private final byte[] hash;
    private final byte[] receiverPublicKeyBytes;
    private final byte[] signature;
    private final long created;
    @ExcludeForHash
    private final int version;
    private final transient PublicKey receiverPublicKey;
    // Simple class name of the removed MailboxMessage. It selects the store.
    // Excluded from the hash, so it does not change the proof of work payload. Signature V2 covers it.
    @ExcludeForHash
    private final String className;
    // Added in v2.1.14. Empty if received from nodes of earlier versions. Excluded from the hash, so that the proof of
    // work payload stays the same as that of nodes of earlier versions.
    @ExcludeForHash
    private final byte[] signatureV2;

    // Receiver is owner for remove request
    private RemoveMailboxRequest(int version,
                                 MetaData metaData,
                                 byte[] hash,
                                 byte[] receiverPublicKeyBytes,
                                 PublicKey receiverPublicKey,
                                 byte[] signature,
                                 long created,
                                 String className,
                                 byte[] signatureV2) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.receiverPublicKeyBytes = receiverPublicKeyBytes;
        this.receiverPublicKey = receiverPublicKey;
        this.signature = signature;
        this.created = created;
        this.className = className;
        this.signatureV2 = signatureV2;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        EcPublicKeyValidation.validate(receiverPublicKeyBytes);
        NetworkDataValidation.validateDate(created);
        SignatureValidation.validateSignatures(signature, signatureV2);
        SignatureValidation.validateVersionOfSignatureV2(signatureV2, version);
        MetaData.validateVersionOfMetaDataHolder(version);
        MetaData.validateClassName(className);
        // Nodes of earlier versions select the store by the className of metaData
        checkArgument(metaData.getClassName().equals(className), "className of metaData must equal className");
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
                .setClassName(className)
                .setSignatureV2(ByteString.copyFrom(signatureV2));
    }

    public static RemoveMailboxRequest fromProto(bisq.network.protobuf.RemoveMailboxRequest proto)
            throws UnresolvableProtobufException {
        try {
            byte[] receiverPublicKeyBytes = proto.getReceiverPublicKeyBytes().toByteArray();
            PublicKey receiverPublicKey = KeyGeneration.generatePublic(receiverPublicKeyBytes);
            //noinspection deprecation
            MetaData receivedMetaData = MetaData.fromProto(proto.getMetaData());
            // Nodes of earlier versions do not send className.
            String className = proto.getClassName().isEmpty() ?
                    receivedMetaData.getClassName() :
                    proto.getClassName();
            // Nodes of earlier versions use metaData. Send them the registered values, not those which a sender or
            // relay chose. serializeForHash() never contains metaData, so the proof of work payload does not change.
            MetaData metaData = StorableTypesRegistry.getMetaDataForEarlierVersions(className, receivedMetaData);
            return new RemoveMailboxRequest(
                    proto.getVersion(),
                    metaData,
                    proto.getHash().toByteArray(),
                    receiverPublicKeyBytes,
                    receiverPublicKey,
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

    // Contains metaData, version, className and signatureV2, which serializeForHash() omits, as the default
    // encoding contains all non-transient fields.
    public static final CanonicalSchema<RemoveMailboxRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(5,
                    CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                            .canonical(1, request -> request.metaData)
                            .bytes(2, request -> request.hash)
                            .bytes(3, request -> request.receiverPublicKeyBytes)
                            .bytes(4, request -> request.signature)
                            .sint64(5, request -> request.created)
                            .sint32(6, request -> request.version)
                            .string(7, request -> request.className)
                            .bytes(8, request -> request.signatureV2));

    public static final CanonicalSchema<RemoveMailboxRequest> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<RemoveMailboxRequest> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: metaData (field 1), version (field 6), className (field 7) and signatureV2 (field 8).
    private static CanonicalSchema<RemoveMailboxRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(5,
                CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                        .bytes(2, request -> request.hash)
                        .bytes(3, request -> request.receiverPublicKeyBytes)
                        .bytes(4, request -> request.signature)
                        .sint64(5, request -> request.created));
    }

    // The preimage of signature V2. It is a new message, not the protobuf message: a domain separation tag, then every
    // field except signatureV2 itself and the deprecated metaData. No node uses the received metaData for a decision
    // (see getMetaData()). The @ExcludeForHash annotations do not apply.
    public static final CanonicalSchema<RemoveMailboxRequest> SIGNATURE_V2_SCHEMA =
            CanonicalSchema.<RemoveMailboxRequest>newBuilder()
                    .string(1, ignore -> "bisq.RemoveMailboxRequest.signatureV2")
                    .sint32(2, request -> request.version)
                    .string(3, request -> request.className)
                    .sint64(4, request -> request.created)
                    .bytes(5, request -> request.hash)
                    .bytes(6, request -> request.receiverPublicKeyBytes)
                    .bytes(7, request -> request.signature)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<RemoveMailboxRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_V2_SCHEMA;
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for RemoveMailboxRequest");
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
        return StorageKeyV2.createPreImage(StorageKeyV2.MAILBOX_DATA_TAG, hash, receiverPublicKeyBytes);
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
        return receiverPublicKey;
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

    // The time to live of the type, limited by MailboxData.MAX_TTL.
    @Override
    public long getTtl() {
        return Math.min(MailboxData.MAX_TTL, getMetaData().getTtl());
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
        if (!(o instanceof RemoveMailboxRequest that)) return false;

        return created == that.created &&
                version == that.version &&
                metaData.equals(that.metaData) &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(receiverPublicKeyBytes, that.receiverPublicKeyBytes) &&
                Arrays.equals(signature, that.signature) &&
                className.equals(that.className) &&
                Arrays.equals(signatureV2, that.signatureV2);
    }

    @Override
    public int hashCode() {
        int result = metaData.hashCode();
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(receiverPublicKeyBytes);
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + version;
        result = 31 * result + className.hashCode();
        result = 31 * result + Arrays.hashCode(signatureV2);
        return result;
    }

    @Override
    public String toString() {
        return "RemoveMailboxRequest{" +
                "metaData=" + metaData +
                ", hash=" + Hex.encode(hash) +
                ", receiverPublicKeyBytes=" + Hex.encode(receiverPublicKeyBytes) +
                ", signature=" + Hex.encode(signature) +
                ", created=" + created +
                ", version=" + version +
                ", className='" + className + '\'' +
                ", signatureV2=" + Hex.encode(signatureV2) +
                '}';
    }
}
