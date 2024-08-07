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
import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

@Slf4j
@EqualsAndHashCode
@Getter
public final class RemoveMailboxRequest implements MailboxRequest, RemoveDataRequest {
    private static final int VERSION = 1;

    public static RemoveMailboxRequest from(MailboxData mailboxData, KeyPair receiverKeyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(mailboxData.serializeForHash());
        byte[] signature = SignatureUtil.sign(hash, receiverKeyPair.getPrivate());
        PublicKey publicKey = receiverKeyPair.getPublic();
        long created = System.currentTimeMillis();
        return new RemoveMailboxRequest(VERSION,
                mailboxData.getMetaData(),
                hash,
                publicKey.getEncoded(),
                publicKey,
                signature,
                created);
    }

    public static RemoveMailboxRequest cloneWithVersion0(RemoveMailboxRequest request) {
        return new RemoveMailboxRequest(0,
                request.getMetaData(),
                request.getHash(),
                request.getReceiverPublicKeyBytes(),
                request.getReceiverPublicKey(),
                request.getSignature(),
                request.getCreated());
    }

    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;

    private final byte[] hash;
    private final byte[] receiverPublicKeyBytes;
    private final byte[] signature;
    private final long created;
    private transient PublicKey receiverPublicKey;
    @Setter
    private transient Optional<MetaData> metaDataFromDistributedData = Optional.empty();

    // Receiver is owner for remove request
    private RemoveMailboxRequest(int version,
                                 MetaData metaData,
                                 byte[] hash,
                                 byte[] receiverPublicKeyBytes,
                                 PublicKey receiverPublicKey,
                                 byte[] signature,
                                 long created) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.receiverPublicKeyBytes = receiverPublicKeyBytes;
        this.receiverPublicKey = receiverPublicKey;
        this.signature = signature;
        this.created = created;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateECPubKey(receiverPublicKeyBytes);
        NetworkDataValidation.validateECSignature(signature);
        NetworkDataValidation.validateDate(created);
    }

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
        return bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setHash(ByteString.copyFrom(hash))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(receiverPublicKeyBytes))
                .setSignature(ByteString.copyFrom(signature))
                .setCreated(created);
    }

    public static RemoveMailboxRequest fromProto(bisq.network.protobuf.RemoveMailboxRequest proto) {
        byte[] receiverPublicKeyBytes = proto.getReceiverPublicKeyBytes().toByteArray();
        try {
            PublicKey receiverPublicKey = KeyGeneration.generatePublic(receiverPublicKeyBytes);
            return new RemoveMailboxRequest(
                    proto.getVersion(),
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

    public MetaData getMetaDataFromProto() {
        return metaData;
    }

    public MetaData getMetaData() {
        return metaDataFromDistributedData.orElse(metaData);
    }

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.1, 0.3, getMetaData().getCostFactor());
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

    public String getClassName() {
        return getMetaData().getClassName();
    }

    @Override
    public int getSequenceNumber() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > Math.min(MailboxData.MAX_TLL, getMetaData().getTtl());
    }

    @Override
    public int getMaxMapSize() {
        return getMetaData().getMaxMapSize();
    }


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
                '}';
    }
}
