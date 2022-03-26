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

import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.KeyGeneration;
import bisq.security.SignatureUtil;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

@Slf4j
@EqualsAndHashCode
@Getter
public class RemoveMailboxRequest implements MailboxRequest, RemoveDataRequest {
    private final MetaData metaData;
    private final byte[] hash;
    private final byte[] receiverPublicKeyBytes;
    private final byte[] signature;
    private final long created;
    private transient PublicKey receiverPublicKey;

    public static RemoveMailboxRequest from(MailboxData mailboxData, KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(mailboxData.serialize());
        byte[] signature = SignatureUtil.sign(hash, receiverKeyPair.getPrivate());
        return new RemoveMailboxRequest(mailboxData.getMetaData(), hash, receiverKeyPair.getPublic(), signature);
    }

    // Receiver is owner for remove request
    public RemoveMailboxRequest(MetaData metaData,
                                byte[] hash,
                                PublicKey receiverPublicKey,
                                byte[] signature) {
        this(metaData,
                hash,
                receiverPublicKey.getEncoded(),
                receiverPublicKey,
                signature,
                System.currentTimeMillis());
    }

    private RemoveMailboxRequest(MetaData metaData,
                                 byte[] hash,
                                 byte[] receiverPublicKeyBytes,
                                 PublicKey receiverPublicKey,
                                 byte[] signature,
                                 long created) {
        this.metaData = metaData;
        this.hash = hash;
        this.receiverPublicKeyBytes = receiverPublicKeyBytes;
        this.receiverPublicKey = receiverPublicKey;
        this.signature = signature;
        this.created = created;
    }

    public bisq.network.protobuf.NetworkMessage toNetworkMessageProto() {
        return getNetworkMessageBuilder().setRemoveMailboxRequest(
                        bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                                .setMetaData(metaData.toProto())
                                .setHash(ByteString.copyFrom(hash))
                                .setReceiverPublicKeyBytes(ByteString.copyFrom(receiverPublicKeyBytes))
                                .setSignature(ByteString.copyFrom(signature))
                                .setCreated(created))
                .build();
    }

    public static RemoveMailboxRequest fromProto(bisq.network.protobuf.RemoveMailboxRequest proto) {
        byte[] receiverPublicKeyBytes = proto.getReceiverPublicKeyBytes().toByteArray();
        try {
            PublicKey receiverPublicKey = KeyGeneration.generatePublic(receiverPublicKeyBytes);
            return new RemoveMailboxRequest(
                    MetaData.fromProto(proto.getMetaData()),
                    proto.getHash().toByteArray(),
                    receiverPublicKeyBytes,
                    receiverPublicKey,
                    proto.getSignature().toByteArray(),
                    proto.getCreated()
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isPublicKeyHashInvalid(MailboxSequentialData mailboxSequentialData) {
        try {
            return !Arrays.equals(mailboxSequentialData.getReceiversPublicKeyHash(),
                    DigestUtil.hash(receiverPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isSignatureInvalid() {
        try {
            if (receiverPublicKey == null) {
                receiverPublicKey = KeyGeneration.generatePublic(receiverPublicKeyBytes);
            }
            return !SignatureUtil.verify(hash, signature, receiverPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        // Use max value for sequence number so that no other addData call is permitted.
        return Integer.MAX_VALUE <= seqNumberFromMap;
    }

    @Override
    public int getSequenceNumber() {
        return Integer.MAX_VALUE;
    }
}
