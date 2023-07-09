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
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.security.KeyGeneration;
import bisq.security.SignatureUtil;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 * Used for verifying if data publisher is authorized to publish this data (e.g. ProofOfBurnData, Filter, Alert, DisputeAgent...).
 * We use the provided signature and pubKey and check if the pubKey is in the set of provided authorized puKeys from
 * the authorizedDistributedData object, which will return a hard coded set of pubKeys.
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public final class AuthorizedData extends AuthenticatedData {
    private final byte[] signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;

    public AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                          byte[] signature,
                          PublicKey authorizedPublicKey) {
        this(authorizedDistributedData, signature, authorizedPublicKey, authorizedPublicKey.getEncoded());
    }

    private AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                           byte[] signature,
                           PublicKey authorizedPublicKey,
                           byte[] authorizedPublicKeyBytes) {
        super(authorizedDistributedData);
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        this.authorizedPublicKeyBytes = authorizedPublicKeyBytes;
    }

    public bisq.network.protobuf.AuthenticatedData toProto() {
        return getAuthenticatedDataBuilder().setAuthorizedData(
                        bisq.network.protobuf.AuthorizedData.newBuilder()
                                .setSignature(ByteString.copyFrom(signature))
                                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes)))
                .build();
    }

    public static AuthorizedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        bisq.network.protobuf.AuthorizedData authorizedDataProto = proto.getAuthorizedData();
        byte[] authorizedPublicKeyBytes = authorizedDataProto.getAuthorizedPublicKeyBytes().toByteArray();
        try {
            PublicKey authorizedPublicKey = KeyGeneration.generatePublic(authorizedPublicKeyBytes);
            DistributedData distributedData = DistributedData.fromAny(proto.getDistributedData());
            if (distributedData instanceof AuthorizedDistributedData) {
                return new AuthorizedData((AuthorizedDistributedData) distributedData,
                        authorizedDataProto.getSignature().toByteArray(),
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

    @Override
    public boolean isDataInvalid(byte[] ownerPubKeyHash) {
        try {
            AuthorizedDistributedData authorizedDistributedData = getAuthorizedDistributedData();
            if (authorizedDistributedData.isDataInvalid(ownerPubKeyHash)) {
                return true;
            }
            if (!SignatureUtil.verify(distributedData.serialize(), signature, authorizedPublicKey)) {
                return true;
            }

            boolean isStaticallyAuthorizedPublicKeyValidation = authorizedDistributedData instanceof StaticallyAuthorizedPublicKeyValidation;
            boolean isDeferredAuthorizedPublicKeyValidation = authorizedDistributedData instanceof DeferredAuthorizedPublicKeyValidation;
            // The authorizedDistributedData must implement at least one of those interfaces
            if (!isStaticallyAuthorizedPublicKeyValidation && !isDeferredAuthorizedPublicKeyValidation) {
                log.warn("The authorizedDistributedData must implement StaticallyAuthorizedPublicKeyValidation or DeferredAuthorizedPublicKeyValidation (or both)");
                return true;
            }

            // We could have authorizedDistributedData implementing both interfaces.
            // We only verify if it's StaticallyAuthorizedPublicKeyValidation but not DeferredAuthorizedPublicKeyValidation
            if (isStaticallyAuthorizedPublicKeyValidation && !isDeferredAuthorizedPublicKeyValidation) {
                StaticallyAuthorizedPublicKeyValidation data = (StaticallyAuthorizedPublicKeyValidation) authorizedDistributedData;
                return !data.getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes));
            }
            return false;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    @Override
    public String toString() {
        return "AuthorizedData{" +
                "\r\n     signature=" + Hex.encode(signature) +
                ",\r\n     authorizedPublicKeyBytes=" + Hex.encode(authorizedPublicKeyBytes) +
                ",\r\n     distributedData=" + distributedData +
                "\r\n} ";
    }
}
