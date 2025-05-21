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
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Getter
public final class MailboxSequentialData implements NetworkProto {
    private final MailboxData mailboxData;
    private final byte[] senderPublicKeyHash;
    private final byte[] receiversPublicKeyHash;
    private final byte[] receiversPubKeyBytes;
    private final long created;
    private final int sequenceNumber;
    // transient fields are excluded by default for EqualsAndHashCode
    private transient final PublicKey receiversPubKey;

    public MailboxSequentialData(MailboxData mailboxData,
                                 byte[] senderPublicKeyHash,
                                 byte[] receiversPublicKeyHash,
                                 PublicKey receiversPubKey,
                                 int sequenceNumber) {
        this(mailboxData,
                senderPublicKeyHash,
                receiversPublicKeyHash,
                receiversPubKey,
                System.currentTimeMillis(),
                sequenceNumber);
    }

    public MailboxSequentialData(MailboxData mailboxData,
                                 byte[] senderPublicKeyHash,
                                 byte[] receiversPublicKeyHash,
                                 PublicKey receiversPubKey,
                                 long created,
                                 int sequenceNumber) {
        this(mailboxData,
                senderPublicKeyHash,
                receiversPublicKeyHash,
                receiversPubKey,
                receiversPubKey.getEncoded(),
                created,
                sequenceNumber);
    }

    public MailboxSequentialData(MailboxData mailboxData,
                                 byte[] senderPublicKeyHash,
                                 byte[] receiversPublicKeyHash,
                                 PublicKey receiversPubKey,
                                 byte[] receiversPubKeyBytes,
                                 long created,
                                 int sequenceNumber) {
        this.mailboxData = mailboxData;
        this.senderPublicKeyHash = senderPublicKeyHash;
        this.receiversPublicKeyHash = receiversPublicKeyHash;
        this.receiversPubKey = receiversPubKey;
        this.receiversPubKeyBytes = receiversPubKeyBytes;
        this.created = created;
        this.sequenceNumber = sequenceNumber;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(senderPublicKeyHash);
        NetworkDataValidation.validateHash(receiversPublicKeyHash);
        NetworkDataValidation.validateECPubKey(receiversPubKeyBytes);
        NetworkDataValidation.validateDate(created);
    }

    @Override
    public bisq.network.protobuf.MailboxSequentialData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.MailboxSequentialData.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.MailboxSequentialData.newBuilder()
                .setMailboxData(mailboxData.toProto(serializeForHash))
                .setSenderPublicKeyHash(ByteString.copyFrom(senderPublicKeyHash))
                .setReceiversPubKeyHash(ByteString.copyFrom(receiversPublicKeyHash))
                .setReceiversPubKeyBytes(ByteString.copyFrom(receiversPubKeyBytes))
                .setCreated(created)
                .setSequenceNumber(sequenceNumber);
    }

    public static MailboxSequentialData fromProto(bisq.network.protobuf.MailboxSequentialData proto) {
        byte[] receiversPubKeyBytes = proto.getReceiversPubKeyBytes().toByteArray();
        try {
            PublicKey receiversPubKey = KeyGeneration.generatePublic(receiversPubKeyBytes);
            return new MailboxSequentialData(
                    MailboxData.fromProto(proto.getMailboxData()),
                    proto.getSenderPublicKeyHash().toByteArray(),
                    proto.getReceiversPubKeyHash().toByteArray(),
                    receiversPubKey,
                    receiversPubKeyBytes,
                    proto.getCreated(),
                    proto.getSequenceNumber()
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) >
                Math.min(MailboxData.MAX_TLL, mailboxData.getMetaData().getTtl());
    }

    @Override
    public String toString() {
        return "MailboxSequentialData{" +
                "sequenceNumber=" + sequenceNumber +
                ", created=" + new Date(created) + " (" + created + ")" +
                ", senderPublicKeyHash=" + Hex.encode(senderPublicKeyHash) +
                ", receiversPublicKeyHash=" + Hex.encode(receiversPublicKeyHash) +
                ", receiversPubKeyBytes=" + Hex.encode(receiversPubKeyBytes) +
                ", receiversPubKey=" + Hex.encode(receiversPubKey.getEncoded()) +
                ", mailboxData=" + mailboxData +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MailboxSequentialData that)) return false;

        return created == that.created &&
                sequenceNumber == that.sequenceNumber &&
                Objects.equals(mailboxData, that.mailboxData) &&
                Arrays.equals(senderPublicKeyHash, that.senderPublicKeyHash) &&
                Arrays.equals(receiversPublicKeyHash, that.receiversPublicKeyHash) &&
                Arrays.equals(receiversPubKeyBytes, that.receiversPubKeyBytes) &&
                Objects.equals(receiversPubKey, that.receiversPubKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mailboxData);
        result = 31 * result + Arrays.hashCode(senderPublicKeyHash);
        result = 31 * result + Arrays.hashCode(receiversPublicKeyHash);
        result = 31 * result + Arrays.hashCode(receiversPubKeyBytes);
        result = 31 * result + Long.hashCode(created);
        result = 31 * result + sequenceNumber;
        result = 31 * result + Objects.hashCode(receiversPubKey);
        return result;
    }
}
