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

import bisq.common.proto.Proto;
import bisq.security.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

@Getter
@ToString
@EqualsAndHashCode
public class MailboxSequentialData implements Proto {
    private final MailboxData mailboxData;
    private final byte[] senderPublicKeyHash;
    private final byte[] receiversPublicKeyHash;
    private final byte[] receiversPubKeyBytes;
    private final long created;
    private final int sequenceNumber;
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
    }

    public bisq.network.protobuf.MailboxSequentialData toProto() {
        return bisq.network.protobuf.MailboxSequentialData.newBuilder()
                .setMailboxData(mailboxData.toProto())
                .setSenderPublicKeyHash(ByteString.copyFrom(senderPublicKeyHash))
                .setReceiversPubKeyHash(ByteString.copyFrom(receiversPublicKeyHash))
                .setReceiversPubKeyBytes(ByteString.copyFrom(receiversPubKeyBytes))
                .setCreated(created)
                .setSequenceNumber(sequenceNumber)
                .build();
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
        return (System.currentTimeMillis() - created) > mailboxData.getMetaData().getTtl();
    }
}
