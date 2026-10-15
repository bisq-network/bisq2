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
import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import bisq.security.DigestUtil;
import bisq.security.keys.EcPublicKeyValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public final class MailboxSequentialData implements NetworkProto, Canonical {
    private final MailboxData mailboxData;
    // Sequence number is always 1 as it is used only in AddMailboxRequests and those are permitted only once.
    // The RemoveMailboxRequest will set it to Integer.MAX and disallow by that another message.
    private final int sequenceNumber;
    private final byte[] senderPublicKeyHash;
    private final long created;
    private final byte[] receiversPubKeyBytes;
    private final byte[] receiversPublicKeyHash;
    private transient final PublicKey receiversPubKey;

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
        checkArgument(sequenceNumber == 1,
                "sequenceNumber must be 1. sequenceNumber=" + sequenceNumber);
        NetworkDataValidation.validateHash(senderPublicKeyHash);
        NetworkDataValidation.validateHash(receiversPublicKeyHash);
        EcPublicKeyValidation.validate(receiversPubKeyBytes);
        // Storage key V2 and removal authorization must identify the same receiver.
        checkArgument(Arrays.equals(receiversPublicKeyHash, DigestUtil.hash(receiversPubKeyBytes)),
                "Receiver public key hash must match receiver public key bytes");
        // The sender must be the owner of the confidential message (see docs/specifications/network/storage-ownership.md)
        checkArgument(Arrays.equals(senderPublicKeyHash, mailboxData.getConfidentialMessage().getOwnerPubKeyHash()),
                "Sender public key hash must match the sender of the confidential message");
        NetworkDataValidation.validateDate(created);
    }

    @Override
    public bisq.network.protobuf.MailboxSequentialData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
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

    public static MailboxSequentialData fromProto(bisq.network.protobuf.MailboxSequentialData proto)
            throws UnresolvableProtobufException {
        try {
            byte[] receiversPubKeyBytes = proto.getReceiversPubKeyBytes().toByteArray();
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
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    public static final CanonicalSchema<MailboxSequentialData> DEFAULT_SCHEMA =
            CanonicalSchema.<MailboxSequentialData>newBuilder()
                    .canonical(1, MailboxSequentialData::getMailboxData)
                    .sint32(2, MailboxSequentialData::getSequenceNumber)
                    .bytes(3, MailboxSequentialData::getSenderPublicKeyHash)
                    .sint64(4, MailboxSequentialData::getCreated)
                    .bytes(5, MailboxSequentialData::getReceiversPubKeyBytes)
                    .bytes(6, MailboxSequentialData::getReceiversPublicKeyHash)
                    .build();

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<MailboxSequentialData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            // Only the stored data has a storage key encoding (see docs/specifications/network/storage-keys.md)
            case STORAGE_KEY ->
                    throw new IllegalArgumentException("Storage key not supported for MailboxSequentialData");
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
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
