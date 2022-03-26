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

package bisq.network.p2p.services.data.storage.auth;

import bisq.common.encoding.Hex;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageData;
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

@Getter
@EqualsAndHashCode
@Slf4j
public class RefreshAuthenticatedDataRequest implements BroadcastMessage {
    public static RefreshAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                       StorageData storageData,
                                                       KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(storageData.serialize());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = store.getSequenceNumber(hash) + 1;
        return new RefreshAuthenticatedDataRequest(storageData.getMetaData(),
                hash,
                keyPair.getPublic(),
                newSequenceNumber,
                signature);
    }

    protected final MetaData metaData;
    protected final byte[] hash;
    protected final byte[] ownerPublicKeyBytes; // 442 bytes
    transient protected final PublicKey ownerPublicKey;
    protected final int sequenceNumber;
    protected final byte[] signature;         // 47 bytes

    public RefreshAuthenticatedDataRequest(MetaData metaData,
                                           byte[] hash,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature) {
        this(metaData,
                hash,
                ownerPublicKey.getEncoded(),
                ownerPublicKey,
                sequenceNumber,
                signature);
    }

    protected RefreshAuthenticatedDataRequest(MetaData metaData,
                                              byte[] hash,
                                              byte[] ownerPublicKeyBytes,
                                              PublicKey ownerPublicKey,
                                              int sequenceNumber,
                                              byte[] signature) {
        this.metaData = metaData;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toNetworkMessageProto() {
        return getNetworkMessageBuilder().setRefreshAuthenticatedDataRequest(
                        bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                                .setMetaData(metaData.toProto())
                                .setHash(ByteString.copyFrom(hash))
                                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                                .setSequenceNumber(sequenceNumber)
                                .setSignature(ByteString.copyFrom(signature)))
                .build();
    }

    public static RefreshAuthenticatedDataRequest fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest proto) {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            return new RefreshAuthenticatedDataRequest(
                    MetaData.fromProto(proto.getMetaData()),
                    proto.getHash().toByteArray(),
                    ownerPublicKeyBytes,
                    ownerPublicKey,
                    proto.getSequenceNumber(),
                    proto.getSignature().toByteArray()
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(hash, signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyInvalid(AuthenticatedSequentialData entryFromMap) {
        try {
            return !Arrays.equals(entryFromMap.getPubKeyHash(), DigestUtil.hash(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    @Override
    public String toString() {
        return "RefreshAuthenticatedDataRequest{" +
                "\r\n     metaData=" + metaData +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     signature=" + Hex.encode(signature) +
                "\r\n}";
    }
}
