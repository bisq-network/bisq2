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
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.protobuf.DataRequest;
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

@Getter
@EqualsAndHashCode
@Slf4j
public final class RemoveAuthenticatedDataRequest implements AuthenticatedDataRequest, RemoveDataRequest {
    private static final int VERSION = 1;

    public static RemoveAuthenticatedDataRequest from(AuthenticatedDataStorageService store, AuthenticatedData authenticatedData, KeyPair keyPair)
            throws GeneralSecurityException {
        byte[] hash = DigestUtil.hash(authenticatedData.serializeForHash());
        byte[] signature = SignatureUtil.sign(hash, keyPair.getPrivate());
        int newSequenceNumber = store.getSequenceNumber(hash) + 1;
        PublicKey publicKey = keyPair.getPublic();
        return new RemoveAuthenticatedDataRequest(VERSION,
                authenticatedData.getMetaData(),
                hash,
                publicKey.getEncoded(),
                publicKey,
                newSequenceNumber,
                signature);
    }


    public static RemoveAuthenticatedDataRequest cloneWithVersion0(RemoveAuthenticatedDataRequest request) {
        return new RemoveAuthenticatedDataRequest(0,
                request.getMetaData(),
                request.getHash(),
                request.getOwnerPublicKeyBytes(),
                request.getOwnerPublicKey(),
                request.getSequenceNumber(),
                request.getSignature());
    }

    @EqualsAndHashCode.Exclude
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final MetaData metaData;

    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;

    private final byte[] hash;
    private final byte[] ownerPublicKeyBytes;
    transient private PublicKey ownerPublicKey;
    private final int sequenceNumber;
    private final byte[] signature;
    private final long created;
    @Setter
    private transient Optional<MetaData> metaDataFromDistributedData = Optional.empty();

    private RemoveAuthenticatedDataRequest(int version,
                                           MetaData metaData,
                                           byte[] hash,
                                           byte[] ownerPublicKeyBytes,
                                           PublicKey ownerPublicKey,
                                           int sequenceNumber,
                                           byte[] signature) {
        this(version,
                metaData,
                hash,
                ownerPublicKeyBytes,
                ownerPublicKey,
                sequenceNumber,
                signature,
                System.currentTimeMillis());
    }

    private RemoveAuthenticatedDataRequest(int version,
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
        NetworkDataValidation.validateDate(created);
    }

    @Override
    public DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setRemoveAuthenticatedDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.RemoveAuthenticatedDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.RemoveAuthenticatedDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.RemoveAuthenticatedDataRequest.newBuilder()
                .setVersion(version)
                .setMetaData(metaData.toProto(serializeForHash))
                .setHash(ByteString.copyFrom(hash))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(ownerPublicKeyBytes))
                .setSequenceNumber(sequenceNumber)
                .setSignature(ByteString.copyFrom(signature))
                .setCreated(created);
    }

    public static RemoveAuthenticatedDataRequest fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest proto) {
        byte[] ownerPublicKeyBytes = proto.getOwnerPublicKeyBytes().toByteArray();
        try {
            PublicKey ownerPublicKey = KeyGeneration.generatePublic(ownerPublicKeyBytes);
            return new RemoveAuthenticatedDataRequest(
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
        return (System.currentTimeMillis() - created) > getMetaData().getTtl();
    }

    @Override
    public int getMaxMapSize() {
        return getMetaData().getMaxMapSize();
    }

    public String getClassName() {
        return getMetaData().getClassName();
    }

    @Override
    public String toString() {
        return "RemoveAuthenticatedDataRequest{" +
                "\r\n     version=" + version +
                ",\r\n     metaData=" + metaData +
                ",\r\n     metaDataFromDistributedData=" + metaDataFromDistributedData +
                ",\r\n     hash=" + Hex.encode(hash) +
                ",\r\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                ",\r\n     ownerPublicKey=" + ownerPublicKey +
                ",\r\n     sequenceNumber=" + sequenceNumber +
                ",\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     created=" + new Date(created) + " (" + created + ")" +
                "\r\n}";
    }
}
