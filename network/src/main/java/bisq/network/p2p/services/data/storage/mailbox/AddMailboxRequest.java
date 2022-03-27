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

import bisq.network.p2p.services.data.AddDataRequest;
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
import java.util.Optional;

@Slf4j
@EqualsAndHashCode
@Getter
public class AddMailboxRequest implements MailboxRequest, AddDataRequest {

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
        byte[] serialized = mailboxSequentialData.serialize();
        byte[] signature = SignatureUtil.sign(serialized, senderKeyPair.getPrivate());
        return new AddMailboxRequest(mailboxSequentialData, signature, senderPublicKey);
    }

    private final MailboxSequentialData mailboxSequentialData;
    private final byte[] signature;
    private final byte[] senderPublicKeyBytes;
    private final PublicKey senderPublicKey;

    public AddMailboxRequest(MailboxSequentialData mailboxSequentialData,
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
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder().setAddMailboxRequest(
                        bisq.network.protobuf.AddMailboxRequest.newBuilder()
                                .setMailboxSequentialData(mailboxSequentialData.toProto())
                                .setSignature(ByteString.copyFrom(signature))
                                .setSenderPublicKeyBytes(ByteString.copyFrom(senderPublicKeyBytes)))
                .build();
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

    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(mailboxSequentialData.serialize(), signature, getOwnerPublicKey());
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

    public String getFileName() {
        return mailboxSequentialData.getMailboxData().getMetaData().getFileName();
    }

    @Override
    public int getSequenceNumber() {
        return mailboxSequentialData.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return mailboxSequentialData.getCreated();
    }

    public MetaData getMetaData() {
        return mailboxSequentialData.getMailboxData().getMetaData();
    }


    @Override
    public String toString() {
        return "AddMailboxDataRequest{} " + super.toString();
    }


}
