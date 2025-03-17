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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.MetaData;
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
import java.util.Date;

@Getter
@EqualsAndHashCode
@Slf4j
public final class RefreshAuthenticatedDataRequest implements AuthenticatedDataRequest {
    private static final int VERSION = 1;

    public static RefreshAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                       AuthenticatedData authenticatedData,
                                                       KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(authenticatedData.serializeForHash());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = store.getSequenceNumber(hash) + 1;
        PublicKey publicKey = keyPair.getPublic();
        return new RefreshAuthenticatedDataRequest(VERSION,
                authenticatedData.getMetaData(),
                hash,
                publicKey.getEncoded(),
                publicKey,
                newSequenceNumber,
                signature,
                System.currentTimeMillis());
    }


    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;

    private final byte[] hash;
    private final byte[] ownerPublicKeyBytes; // 442 bytes
    transient private final PublicKey ownerPublicKey;
    private final int sequenceNumber;
    private final byte[] signature;         // 47 bytes
    private final long created;

    private RefreshAuthenticatedDataRequest(int version,
                                            MetaData metaData,
                                            byte[] hash,
                                            byte[] ownerPublicKeyBytes,
                                            PublicKey ownerPublicKey,
                                            int sequenceNumber,
                                            byte[] signature,
                                            long created) {
        this.version = version;
        this.metaData = metaData;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
        this.created = created;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateHash(hash);
        NetworkDataValidation.validateECPubKey(ownerPublicKeyBytes);
        NetworkDataValidation.validateECSignature(signature);
    }

    @Override
    public bisq.network.protobuf.DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setRefreshAuthenticatedDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.RefreshAuthenticatedDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.RefreshAuthenticatedDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.RefreshAuthenticatedDataRequest.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setHash(ByteString.copyFrom(hash))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                .setSequenceNumber(sequenceNumber)
                .setSignature(ByteString.copyFrom(signature))
                .setCreated(created);
    }

    public static RefreshAuthenticatedDataRequest fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest proto) {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            return new RefreshAuthenticatedDataRequest(
                    proto.getVersion(),
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
    public boolean isExpired() {
        // Not used as we do not persist RefreshAuthenticatedDataRequest but use it to recreate a new 
        // AddAuthenticatedDataRequest with the updated sequenceNumber
        return false;
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
        return "RefreshAuthenticatedDataRequest{" +
                "\r\n     version=" + version +
                ",\r\n     metaData=" + metaData +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     created=" + new Date(created) + " (" + created + ")" +
                "\r\n}";
    }
}
