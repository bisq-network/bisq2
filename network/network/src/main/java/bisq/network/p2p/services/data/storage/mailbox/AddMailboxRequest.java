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

import bisq.common.data.ByteArray;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public final class AddMailboxRequest implements MailboxRequest, AddDataRequest {

    public static AddMailboxRequest from(MailboxData mailboxData,
                                         KeyPair senderKeyPair,
                                         PublicKey receiverPublicKey)
            throws GeneralSecurityException {
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
        byte[] preImage = mailboxSequentialData.serializeForHash();
        byte[] signature = SignatureUtil.sign(preImage, senderKeyPair.getPrivate());
        return new AddMailboxRequest(mailboxSequentialData, signature, senderPublicKeyEncoded, senderPublicKey);
    }

    @Getter
    private final MailboxSequentialData mailboxSequentialData;
    private final byte[] signature;
    private final byte[] senderPublicKeyBytes;
    private final transient PublicKey senderPublicKey;

    private AddMailboxRequest(MailboxSequentialData mailboxSequentialData,
                              byte[] signature,
                              byte[] senderPublicKeyBytes,
                              PublicKey senderPublicKey) {
        this.mailboxSequentialData = mailboxSequentialData;
        this.signature = signature;
        this.senderPublicKeyBytes = senderPublicKeyBytes;
        this.senderPublicKey = senderPublicKey;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateECPubKey(senderPublicKeyBytes);
        NetworkDataValidation.validateECSignature(signature);
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
                .setSenderPublicKeyBytes(ByteString.copyFrom(senderPublicKeyBytes));
    }

    public static AddMailboxRequest fromProto(bisq.network.protobuf.AddMailboxRequest proto) {
        byte[] senderPublicKeyBytes = proto.getSenderPublicKeyBytes().toByteArray();
        try {
            PublicKey senderPublicKey = KeyGeneration.generatePublic(senderPublicKeyBytes);
            return new AddMailboxRequest(
                    MailboxSequentialData.fromProto(proto.getMailboxSequentialData()),
                    proto.getSignature().toByteArray(),
                    senderPublicKeyBytes,
                    senderPublicKey
            );
        } catch (GeneralSecurityException e) {
            log.error("Failed to generate public key");
            throw new RuntimeException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<AddMailboxRequest> DEFAULT_SCHEMA =
            bisq.network.p2p.services.data.DataRequest.createCanonicalSchema(4,
                    CanonicalSchema.<AddMailboxRequest>newBuilder()
                            .canonical(1, AddMailboxRequest::getMailboxSequentialData)
                            .bytes(2, request -> request.signature)
                            .bytes(3, request -> request.senderPublicKeyBytes));

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AddMailboxRequest> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    private byte[] getStoragePreImage() {
        return mailboxSequentialData.getMailboxData().serializeForHash();
    }


    /* --------------------------------------------------------------------- */
    // CostBearingMessage implementation
    /* --------------------------------------------------------------------- */

    @Override
    public double getCostFactor() {
        MailboxData mailboxData = mailboxSequentialData.getMailboxData();
        double metaDataImpact = MessageCostPolicy.getStorageCostFactor(mailboxData.getClassName());
        double dataImpact = mailboxData.getConfidentialMessage().getCostFactor();
        double impact = metaDataImpact + dataImpact;
        return MathUtils.bounded(0.25, 1, impact);
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKey() {
        byte[] hash = DigestUtil.hash(getStoragePreImage());
        return new ByteArray(hash);
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public byte[] getPreImage() {
        return mailboxSequentialData.serializeForHash();
    }

    @Override
    public PublicKey getSignaturePublicKey() {
        return senderPublicKey;
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
    public String toString() {
        return "AddMailboxRequest{" +
                "mailboxSequentialData=" + mailboxSequentialData +
                ", signature=" + Hex.encode(signature) +
                ", senderPublicKeyBytes=" + Hex.encode(senderPublicKeyBytes) +
                ", senderPublicKey=" + Hex.encode(senderPublicKey.getEncoded()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AddMailboxRequest that)) return false;

        return Objects.equals(mailboxSequentialData, that.mailboxSequentialData) &&
                Arrays.equals(signature, that.signature) &&
                Arrays.equals(senderPublicKeyBytes, that.senderPublicKeyBytes) &&
                Objects.equals(senderPublicKey, that.senderPublicKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mailboxSequentialData);
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Arrays.hashCode(senderPublicKeyBytes);
        result = 31 * result + Objects.hashCode(senderPublicKey);
        return result;
    }
}
