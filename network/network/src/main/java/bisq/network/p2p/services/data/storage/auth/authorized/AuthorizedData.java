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

package bisq.network.p2p.services.data.storage.auth.authorized;

import bisq.common.encoding.Hex;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Optional;

/**
 * Used for verifying if data publisher is authorized to publish this data (e.g. ProofOfBurnData, Filter, Alert, DisputeAgent...).
 * We use the provided signature and pubKey and check if the pubKey is in the set of provided authorized puKeys from
 * the authorizedDistributedData object, which will return a hard coded set of pubKeys.
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public final class AuthorizedData extends AuthenticatedData {
    private final Optional<byte[]> signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;

    // At remove, we do not need to authorizedPublicKey as the normal keypair is used to verify right to remove.
    public AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                          PublicKey authorizedPublicKey) {
        this(authorizedDistributedData, Optional.empty(), authorizedPublicKey, authorizedPublicKey.getEncoded());
    }

    public AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                          Optional<byte[]> signature,
                          PublicKey authorizedPublicKey) {
        this(authorizedDistributedData, signature, authorizedPublicKey, authorizedPublicKey.getEncoded());
    }

    private AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                           Optional<byte[]> signature,
                           PublicKey authorizedPublicKey,
                           byte[] authorizedPublicKeyBytes) {
        super(authorizedDistributedData);
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        this.authorizedPublicKeyBytes = authorizedPublicKeyBytes;

        verify();
    }

    @Override
    public void verify() {
        signature.ifPresent(NetworkDataValidation::validateECSignature);
        NetworkDataValidation.validateECPubKey(authorizedPublicKeyBytes);
    }

    @Override
    public byte[] serializeForHash() {
        // We omit the signature for the hash, otherwise we would get a new map entry for the same data at each republishing
        return getAuthenticatedDataBuilder(true).setAuthorizedData(
                        bisq.network.protobuf.AuthorizedData.newBuilder()
                                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes)))
                .build().toByteArray();
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData.Builder getBuilder(boolean serializeForHash) {
        return getAuthenticatedDataBuilder(serializeForHash).setAuthorizedData(toValueProto(serializeForHash));
    }

    public bisq.network.protobuf.AuthorizedData toValueProto(boolean serializeForHash) {
        return resolveBuilder(getValueBuilder(serializeForHash), serializeForHash).build();
    }

    public bisq.network.protobuf.AuthorizedData.Builder getValueBuilder(boolean serializeForHash) {
        bisq.network.protobuf.AuthorizedData.Builder builder = bisq.network.protobuf.AuthorizedData.newBuilder()
                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes));
        signature.ifPresent(signature -> builder.setSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    public static AuthorizedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        bisq.network.protobuf.AuthorizedData authorizedDataProto = proto.getAuthorizedData();
        byte[] authorizedPublicKeyBytes = authorizedDataProto.getAuthorizedPublicKeyBytes().toByteArray();
        try {
            PublicKey authorizedPublicKey = KeyGeneration.generatePublic(authorizedPublicKeyBytes);
            DistributedData distributedData = DistributedData.fromAny(proto.getDistributedData());
            if (distributedData instanceof AuthorizedDistributedData) {
                Optional<byte[]> signature = authorizedDataProto.hasSignature() ?
                        Optional.of(authorizedDataProto.getSignature().toByteArray()) :
                        Optional.empty();
                return new AuthorizedData((AuthorizedDistributedData) distributedData,
                        signature,
                        authorizedPublicKey,
                        authorizedPublicKeyBytes
                );
            } else {
                throw new RuntimeException("DistributedData must be type of AuthorizedDistributedData");
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public AuthorizedDistributedData getAuthorizedDistributedData() {
        return (AuthorizedDistributedData) distributedData;
    }

    public boolean isNotAuthorized() {
        try {
            AuthorizedDistributedData authorizedDistributedData = getAuthorizedDistributedData();
            if (!SignatureUtil.verify(distributedData.serializeForHash(), signature.orElseThrow(), authorizedPublicKey)) {
                return true;
            }

            if (authorizedDistributedData.staticPublicKeysProvided()) {
                return !authorizedDistributedData.getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes));
            }

            return false;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public boolean isDataInvalid(byte[] ownerPubKeyHash) {
        return distributedData.isDataInvalid(ownerPubKeyHash);
    }

    @Override
    public String toString() {
        return "AuthorizedData{" +
                "\r\n               signature=" + signature.map(Hex::encode).orElse("null") +
                ",\r\n               authorizedPublicKeyBytes=" + Hex.encode(authorizedPublicKeyBytes) +
                ",\r\n               distributedData=" + distributedData +
                "\r\n} ";
    }
}
