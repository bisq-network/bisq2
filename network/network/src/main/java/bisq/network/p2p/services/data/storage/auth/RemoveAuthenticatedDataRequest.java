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
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
@Slf4j
public final class RemoveAuthenticatedDataRequest implements AuthenticatedDataRequest, RemoveDataRequest {

    public static RemoveAuthenticatedDataRequest from(AuthenticatedDataStorageService store, AuthenticatedData authenticatedData, KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(authenticatedData.serialize());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = store.getSequenceNumber(hash) + 1;
        return new RemoveAuthenticatedDataRequest(authenticatedData.getMetaData(),
                hash,
                keyPair.getPublic(),
                newSequenceNumber,
                signature);
    }

    private final MetaData metaData;
    private final byte[] hash;
    private final byte[] ownerPublicKeyBytes;
    @Nullable
    transient private PublicKey ownerPublicKey;
    private final int sequenceNumber;
    private final byte[] signature;
    private final long created;

    public RemoveAuthenticatedDataRequest(MetaData metaData,
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

    private RemoveAuthenticatedDataRequest(MetaData metaData,
                                           byte[] hash,
                                           byte[] ownerPublicKeyBytes,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature) {
        this(metaData,
                hash,
                ownerPublicKeyBytes,
                ownerPublicKey,
                sequenceNumber,
                signature,
                System.currentTimeMillis());
    }

    private RemoveAuthenticatedDataRequest(MetaData metaData,
                                           byte[] hash,
                                           byte[] ownerPublicKeyBytes,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature,
                                           long created) {
        this.metaData = metaData;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
        this.created = created;

        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateECPubKey(ownerPublicKeyBytes);
        NetworkDataValidation.validateECSignature(signature);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        return getNetworkMessageBuilder().setDataRequest(getDataRequestBuilder().setRemoveAuthenticatedDataRequest(
                        bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                                .setMetaData(metaData.toProto())
                                .setHash(ByteString.copyFrom(hash))
                                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                                .setSequenceNumber(sequenceNumber)
                                .setSignature(ByteString.copyFrom(signature))
                                .setCreated(created)))
                .build();
    }

    public static RemoveAuthenticatedDataRequest fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest proto) {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            return new RemoveAuthenticatedDataRequest(
                    MetaData.fromProto(proto.getMetaData()),
                    proto.getHash().toByteArray(),
                    ownerPublicKeyBytes,
                    ownerPublicKey,
                    proto.getSequenceNumber(),
                    proto.getSignature().toByteArray(),
                    proto.getCreated()
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.1, 0.3, metaData.getCostFactor());
    }

    public boolean isSignatureInvalid() {
        try {
            if (ownerPublicKey == null) {
                ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            }
            return !SignatureUtil.verify(hash, signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyHashInvalid(AuthenticatedSequentialData entryFromMap) {
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
    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > metaData.getTtl();
    }

    @Override
    public int getMaxMapSize() {
        return metaData.getMaxMapSize();
    }

    public String getClassName() {
        return metaData.getClassName();
    }

    @Override
    public String toString() {
        return "RemoveAuthenticatedDataRequest{" +
                "\r\n     metaData=" + metaData +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ",\r\n     ownerPublicKey=" + ownerPublicKey +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     created=" + created +
                "\r\n}";
    }
}
