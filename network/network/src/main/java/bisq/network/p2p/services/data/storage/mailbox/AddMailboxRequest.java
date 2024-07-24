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

import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AddMailboxRequest implements MailboxRequest, AddDataRequest {

    public static AddMailboxRequest from(MailboxData mailboxData,
                                         KeyPair senderKeyPair,
                                         PublicKey receiverPublicKey)
            throws GeneralSecurityException {
        PublicKey senderPublicKey = senderKeyPair.getPublic();
        byte[] senderPublicKeyHash = DigestUtil.hash(senderPublicKey.getEncoded());
        byte[] receiverPublicKeyHash = DigestUtil.hash(receiverPublicKey.getEncoded());
        MailboxSequentialData mailboxSequentialData = new MailboxSequentialData(mailboxData,
                senderPublicKeyHash,
                receiverPublicKeyHash,
                receiverPublicKey,
                1);
        byte[] serialized = mailboxSequentialData.serializeForHash();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxRequest(mailboxSequentialData, signature, senderPublicKey);
    }

    private final MailboxSequentialData mailboxSequentialData;
    private final byte[] signature;
    private final byte[] senderPublicKeyBytes;
    private final PublicKey senderPublicKey;

    private AddMailboxRequest(MailboxSequentialData mailboxSequentialData,
                             byte[] signature,
                             PublicKey senderPublicKey) {
        this(mailboxSequentialData, signature, senderPublicKey.getEncoded(), senderPublicKey);
    }

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
        NetworkDataValidation.validateECSignature(signature);
        NetworkDataValidation.validateECPubKey(senderPublicKeyBytes);
    }

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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getCostFactor() {
        MailboxData mailboxData = mailboxSequentialData.getMailboxData();
        double metaDataImpact = mailboxData.getMetaData().getCostFactor();
        double dataImpact = mailboxData.getConfidentialMessage().getCostFactor();
        double impact = metaDataImpact + dataImpact;
        return MathUtils.bounded(0.25, 1, impact);
    }

    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(mailboxSequentialData.serializeForHash(), signature, getOwnerPublicKey());
        } catch (Exception e) {
            log.warn(e.toString(), e);
            return true;
        }
    }

    public boolean isPublicKeyInvalid() {
        try {
            return !Arrays.equals(mailboxSequentialData.getSenderPublicKeyHash(),
                    DigestUtil.hash(senderPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public PublicKey getOwnerPublicKey() {
        return Optional.ofNullable(senderPublicKey).orElseGet(() -> {
            try {
                return KeyGeneration.generatePublic(senderPublicKeyBytes);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public int getSequenceNumber() {
        return mailboxSequentialData.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return mailboxSequentialData.getCreated();
    }

    @Override
    public boolean isExpired() {
        return mailboxSequentialData.isExpired();
    }

    @Override
    public int getMaxMapSize() {
        return mailboxSequentialData.getMailboxData().getMetaData().getMaxMapSize();
    }

    @Override
    public String toString() {
        return "AddMailboxRequest{" +
                "mailboxSequentialData=" + mailboxSequentialData +
                ", signature=" + Hex.encode(signature) +
                ", senderPublicKeyBytes=" + Hex.encode(senderPublicKeyBytes) +
                ", senderPublicKey=" + Hex.encode(senderPublicKey.getEncoded()) +
                '}';
    }
}
