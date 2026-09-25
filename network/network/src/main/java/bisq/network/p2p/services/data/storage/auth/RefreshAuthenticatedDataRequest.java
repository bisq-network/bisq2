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
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Slf4j
public final class RefreshAuthenticatedDataRequest implements AuthenticatedDataRequest {
    private static final int VERSION = 1;

    public static RefreshAuthenticatedDataRequest from(AuthenticatedDataStorageService store,
                                                       AuthenticatedData authenticatedData,
                                                       KeyPair keyPair)
            throws GeneralSecurityException {
        MetaData metaData = authenticatedData.getMetaData();
        byte[] authenticatedDataHash = DigestUtil.hash(authenticatedData.serializeForHash());

        // We use the AddAuthenticatedDataRequest for signing and then copy out the data we need for RefreshAuthenticatedDataRequest
        AddAuthenticatedDataRequest addAuthenticatedDataRequest = AddAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
        return new RefreshAuthenticatedDataRequest(VERSION,
                metaData,
                authenticatedDataHash,
                addAuthenticatedDataRequest.getSignaturePubKeyEncoded(),
                addAuthenticatedDataRequest.getSignaturePublicKey(),
                addAuthenticatedDataRequest.getSequenceNumber(),
                addAuthenticatedDataRequest.getSignature(),
                addAuthenticatedDataRequest.getCreated());
    }


    @ExcludeForHash
    private final MetaData metaData;

    @ExcludeForHash
    private final int version;
    private final byte[] hash; // of authenticatedData
    private final byte[] ownerPublicKeyBytes; // 442 bytes
    private final transient PublicKey ownerPublicKey;
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
        NetworkDataValidation.validateDate(created);
        NetworkDataValidation.validateECSignature(signature);
    }


    /* --------------------------------------------------------------------- */
    // Protobuf
    /* --------------------------------------------------------------------- */

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
            log.error("Failed to generate public key");
            throw new RuntimeException(e);
        }
    }


    // Overrides Proto.getVersion(). Proto uses it to resolve @ExcludeForHash(excludeOnlyInVersions = ...), so it
    // determines the result of serializeForHash(), which is the payload of the proof of work. It must return the
    // version of the message.
    @Override
    public int getVersion() {
        return version;
    }


    /* --------------------------------------------------------------------- */
    // CostBearingMessage implementation
    /* --------------------------------------------------------------------- */

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.1, 0.3, metaData.getCostFactor());
    }


    /* --------------------------------------------------------------------- */
    // DataRequest implementation
    /* --------------------------------------------------------------------- */

    @Override
    public ByteArray getStorageKey() {
        return new ByteArray(hash);
    }


    /* --------------------------------------------------------------------- */
    // SignedRequest implementation
    /* --------------------------------------------------------------------- */

    // The signature is created over the AddAuthenticatedDataRequest which the refresh produces (see from()).
    // It can only be verified on that request, which the receiver rebuilds from its stored entry with
    // AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest). The refresh request alone has no pre-image.
    @Override
    public byte[] getPreImage() {
        throw new UnsupportedOperationException("RefreshAuthenticatedDataRequest has no pre-image. " +
                "Verify the AddAuthenticatedDataRequest created with AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest).");
    }

    @Override
    public boolean isSignatureInvalid() {
        throw new UnsupportedOperationException("RefreshAuthenticatedDataRequest cannot be verified alone. " +
                "Verify the AddAuthenticatedDataRequest created with AddAuthenticatedDataRequest.from(requestFromMap, refreshRequest).");
    }

    @Override
    public PublicKey getSignaturePublicKey() {
        return ownerPublicKey;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }


    /* --------------------------------------------------------------------- */
    // SignaturePubKeyHashVerifier implementation
    /* --------------------------------------------------------------------- */


    @Override
    public byte[] getSignaturePubKeyEncoded() {
        return ownerPublicKeyBytes;
    }


    /* --------------------------------------------------------------------- */
    // Sequential implementation
    /* --------------------------------------------------------------------- */

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }


    /* --------------------------------------------------------------------- */
    // Expirable implementation
    /* --------------------------------------------------------------------- */

    @Override
    public long getCreated() {
        return created;
    }

    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - getCreated()) > getTtl();
    }


    /* --------------------------------------------------------------------- */
    // StorageConstraint
    /* --------------------------------------------------------------------- */

    @Override
    public MetaData getMetaData() {
        return metaData;
    }


    /* --------------------------------------------------------------------- */
    // Misc
    /* --------------------------------------------------------------------- */

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RefreshAuthenticatedDataRequest that)) return false;

        return sequenceNumber == that.sequenceNumber &&
                created == that.created &&
                Arrays.equals(hash, that.hash) &&
                Arrays.equals(ownerPublicKeyBytes, that.ownerPublicKeyBytes) &&
                Objects.equals(ownerPublicKey, that.ownerPublicKey) &&
                Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(hash);
        result = 31 * result + Arrays.hashCode(ownerPublicKeyBytes);
        result = 31 * result + Objects.hashCode(ownerPublicKey);
        result = 31 * result + sequenceNumber;
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + Long.hashCode(created);
        return result;
    }
}
