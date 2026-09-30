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
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.SignatureValidation;
import bisq.network.p2p.services.data.storage.StorageKeyV2;
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

@Slf4j
public final class AddMailboxRequest implements MailboxRequest, AddDataRequest {
    private final static int VERSION = 2;

    public static AddMailboxRequest from(MailboxData mailboxData,
                                         KeyPair senderKeyPair,
                                         PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        int version = VERSION;
        long created = System.currentTimeMillis();
        PublicKey senderPublicKey = senderKeyPair.getPublic();
        byte[] senderPublicKeyEncoded = senderPublicKey.getEncoded();
        byte[] senderPublicKeyHash = DigestUtil.hash(senderPublicKeyEncoded);
        byte[] receiverPublicKeyEncoded = receiverPublicKey.getEncoded();
        byte[] receiverPublicKeyHash = DigestUtil.hash(receiverPublicKeyEncoded);
        int sequenceNumber = 1;
        MailboxSequentialData mailboxSequentialData = new MailboxSequentialData(mailboxData,
                senderPublicKeyHash,
                receiverPublicKeyHash,
                receiverPublicKey,
                receiverPublicKeyEncoded,
                created,
                sequenceNumber);

        byte[] preImageV1 = mailboxSequentialData.serializeForHash();
        byte[] signatureV1 = SignatureUtil.sign(preImageV1, senderKeyPair.getPrivate());

        AddMailboxRequest requestToSign = AddMailboxRequest.withoutSignatureV2(mailboxSequentialData,
                signatureV1,
                senderPublicKeyEncoded,
                senderPublicKey,
                version);
        byte[] preImageV2 = requestToSign.getPreImageV2();
        byte[] signatureV2 = SignatureUtil.sign(preImageV2, senderKeyPair.getPrivate());

        return new AddMailboxRequest(mailboxSequentialData,
                signatureV1,
                senderPublicKeyEncoded,
                senderPublicKey,
                signatureV2,
                version);
    }

    private static AddMailboxRequest withoutSignatureV2(MailboxSequentialData mailboxSequentialData,
                                                        byte[] signatureV1,
                                                        byte[] senderPublicKeyEncoded,
                                                        PublicKey senderPublicKey,
                                                        int version) {
        return new AddMailboxRequest(mailboxSequentialData,
                signatureV1,
                senderPublicKeyEncoded,
                senderPublicKey,
                new byte[]{},
                version);
    }

    @Getter
    private final MailboxSequentialData mailboxSequentialData;
    private final byte[] signature;
    private final byte[] senderPublicKeyBytes;
    private final transient PublicKey senderPublicKey;
    // Added in v2.1.14. Empty if received from nodes of earlier versions. Excluded from the hash, so that the proof of
    // work payload stays the same as that of nodes of earlier versions.
    @ExcludeForHash
    private final byte[] signatureV2;
    // Added in v2.1.14. 0 if received from nodes of earlier versions. Version 2 is the first version with signature V2.
    // Excluded from the hash for the same reason as signatureV2.
    @ExcludeForHash
    private final int version;

    private AddMailboxRequest(MailboxSequentialData mailboxSequentialData,
                              byte[] signature,
                              byte[] senderPublicKeyBytes,
                              PublicKey senderPublicKey,
                              byte[] signatureV2,
                              int version) {
        this.mailboxSequentialData = mailboxSequentialData;
        this.signature = signature;
        this.senderPublicKeyBytes = senderPublicKeyBytes;
        this.senderPublicKey = senderPublicKey;
        this.signatureV2 = signatureV2;
        this.version = version;

        verify();
    }

    @Override
    public void verify() {
        EcPublicKeyValidation.validate(senderPublicKeyBytes);
        SignatureValidation.validateSignatures(signature, signatureV2);
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

    @Override
    public bisq.network.protobuf.DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setAddMailboxRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.AddMailboxRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AddMailboxRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AddMailboxRequest.newBuilder()
                .setMailboxSequentialData(mailboxSequentialData.toProto(serializeForHash))
                .setSignature(ByteString.copyFrom(signature))
                .setSenderPublicKeyBytes(ByteString.copyFrom(senderPublicKeyBytes))
                .setSignatureV2(ByteString.copyFrom(signatureV2))
                .setVersion(version);
    }

    public static AddMailboxRequest fromProto(bisq.network.protobuf.AddMailboxRequest proto)
            throws UnresolvableProtobufException {
        byte[] senderPublicKeyBytes = proto.getSenderPublicKeyBytes().toByteArray();
        try {
            PublicKey senderPublicKey = KeyGeneration.generatePublic(senderPublicKeyBytes);
            return new AddMailboxRequest(
                    MailboxSequentialData.fromProto(proto.getMailboxSequentialData()),
                    proto.getSignature().toByteArray(),
                    senderPublicKeyBytes,
                    senderPublicKey,
                    proto.getSignatureV2().toByteArray(),
                    proto.getVersion()
            );
        } catch (GeneralSecurityException e) {
            log.error("Failed to generate public key", e);
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
    // Canonical implementation
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<AddMailboxRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(4,
                    CanonicalSchema.<AddMailboxRequest>newBuilder()
                            .canonical(1, AddMailboxRequest::getMailboxSequentialData)
                            .bytes(2, request -> request.signature)
                            .bytes(3, request -> request.senderPublicKeyBytes)
                            .bytes(4, request -> request.signatureV2)
                            .sint32(5, request -> request.version));

    public static final CanonicalSchema<AddMailboxRequest> PROOF_OF_WORK_SCHEMA = createSchemaForHash();
    public static final CanonicalSchema<AddMailboxRequest> TRADE_CONTRACT_SCHEMA = createSchemaForHash();

    // Leaves out the fields annotated with @ExcludeForHash, as serializeForHash() does: signatureV2 (field 4) and
    // version (field 5).
    private static CanonicalSchema<AddMailboxRequest> createSchemaForHash() {
        return bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(4,
                CanonicalSchema.<AddMailboxRequest>newBuilder()
                        .canonical(1, AddMailboxRequest::getMailboxSequentialData)
                        .bytes(2, request -> request.signature)
                        .bytes(3, request -> request.senderPublicKeyBytes));
    }

    // The preimage of signature V2. It is a new message, not the protobuf message: a domain separation tag, then every
    // field except signatureV2 itself. The @ExcludeForHash annotations do not apply. The nested objects receive the
    // SIGNATURE purpose, so MailboxData contributes its version and className (see MailboxData.SIGNATURE_V2_SCHEMA).
    public static final CanonicalSchema<AddMailboxRequest> SIGNATURE_V2_SCHEMA =
            CanonicalSchema.<AddMailboxRequest>newBuilder()
                    .string(1, ignore -> "bisq.AddMailboxRequest.signatureV2")
                    .sint32(2, request -> request.version)
                    .canonical(3, AddMailboxRequest::getMailboxSequentialData)
                    .bytes(4, request -> request.senderPublicKeyBytes)
                    .bytes(5, request -> request.signature)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AddMailboxRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> SIGNATURE_V2_SCHEMA;
            case STORAGE_KEY -> throw new IllegalArgumentException("Storage key not supported for AddMailboxRequest");
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
        return new ByteArray(DigestUtil.hash(mailboxSequentialData.getMailboxData().serializeForHash()));
    }

    // The data hash must be the one which remove requests carry (see getMailboxDataHash). The receiver owns the entry:
    // only the receiver can remove it.
    @Override
    public byte[] getStorageKeyPreImageV2() {
        return StorageKeyV2.createPreImage(StorageKeyV2.MAILBOX_DATA_TAG,
                getMailboxDataHash(mailboxSequentialData.getMailboxData()),
                mailboxSequentialData.getReceiversPubKeyBytes());
    }

    // The hash which remove requests carry. The STORAGE_KEY encoding of MailboxData equals its serializeForHash(), so
    // nodes of earlier versions compute the same hash.
    static byte[] getMailboxDataHash(MailboxData mailboxData) {
        return DigestUtil.hash(mailboxData.encodeCanonical(EncodingPurpose.STORAGE_KEY));
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getPreImageV1() {
        return mailboxSequentialData.serializeForHash();
    }

    @Override
    public byte[] getPreImageV2() {
        return encodeCanonical(EncodingPurpose.SIGNATURE);
    }

    @Override
    public PublicKey getSignaturePublicKey() {
        return senderPublicKey;
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
        return senderPublicKeyBytes;
    }


    /* --------------------------------------------------------------------- */
    // Sequential implementation
    /* --------------------------------------------------------------------- */

    @Override
    public int getSequenceNumber() {
        return mailboxSequentialData.getSequenceNumber();
    }


    /* --------------------------------------------------------------------- */
    // Expirable implementation
    /* --------------------------------------------------------------------- */

    @Override
    public long getCreated() {
        return mailboxSequentialData.getCreated();
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
        return mailboxSequentialData.getMailboxData().getMetaData();
    }

    // The time to live of the type, limited by MailboxData.MAX_TTL.
    @Override
    public long getTtl() {
        return Math.min(MailboxData.MAX_TTL, getMetaData().getTtl());
    }

    @Override
    public String getClassName() {
        return mailboxSequentialData.getMailboxData().getClassName();
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AddMailboxRequest that)) return false;

        return version == that.version &&
                mailboxSequentialData.equals(that.mailboxSequentialData) &&
                Arrays.equals(signature, that.signature) &&
                Arrays.equals(senderPublicKeyBytes, that.senderPublicKeyBytes) &&
                Arrays.equals(signatureV2, that.signatureV2);
    }

    @Override
    public int hashCode() {
        int result = mailboxSequentialData.hashCode();
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Arrays.hashCode(senderPublicKeyBytes);
        result = 31 * result + Arrays.hashCode(signatureV2);
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "AddMailboxRequest{" +
                "mailboxSequentialData=" + mailboxSequentialData +
                ", signature=" + Hex.encode(signature) +
                ", senderPublicKeyBytes=" + Hex.encode(senderPublicKeyBytes) +
                ", signatureV2=" + Hex.encode(signatureV2) +
                ", version=" + version +
                '}';
    }
}
