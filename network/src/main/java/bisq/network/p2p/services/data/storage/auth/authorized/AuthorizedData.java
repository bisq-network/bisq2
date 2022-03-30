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
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

/**
 * Used for verifying if data publisher is authorized to publish this data (e.g. Filter, Alert, DisputeAgent...).
 * We use the provided signature and pubkey and check if the pubKey is in the set of authorized puKeys.
 */
@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public class AuthorizedData extends AuthenticatedData {
    private final byte[] signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;
    private final Set<String> authorizedPublicKeys; //todo

    public AuthorizedData(DistributedData distributedData,
                          byte[] signature,
                          PublicKey authorizedPublicKey,
                          Set<String> authorizedPublicKeys) {
        this(distributedData, signature, authorizedPublicKey, authorizedPublicKey.getEncoded(), authorizedPublicKeys);
    }

    private AuthorizedData(DistributedData distributedData,
                           byte[] signature,
                           PublicKey authorizedPublicKey,
                           byte[] authorizedPublicKeyBytes,
                           Set<String> authorizedPublicKeys) {
        super(distributedData);
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        this.authorizedPublicKeyBytes = authorizedPublicKeyBytes;
        this.authorizedPublicKeys = authorizedPublicKeys;
    }

    public bisq.network.protobuf.AuthenticatedData toProto() {
        return getAuthenticatedDataBuilder().setAuthorizedData(
                        bisq.network.protobuf.AuthorizedData.newBuilder()
                                .setSignature(ByteString.copyFrom(signature))
                                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes))
                                .addAllAuthorizedPublicKeys(authorizedPublicKeys))
                .build();
    }

    public static AuthorizedData fromProto(bisq.network.protobuf.AuthenticatedData proto) {
        bisq.network.protobuf.AuthorizedData authorizedDataProto = proto.getAuthorizedData();
        byte[] authorizedPublicKeyBytes = authorizedDataProto.getAuthorizedPublicKeyBytes().toByteArray();
        try {
            PublicKey authorizedPublicKey = KeyGeneration.generatePublic(authorizedPublicKeyBytes);
            return new AuthorizedData(DistributedData.fromAny(proto.getDistributedData()),
                    authorizedDataProto.getSignature().toByteArray(),
                    authorizedPublicKey,
                    authorizedPublicKeyBytes,
                    new HashSet<>(authorizedDataProto.getAuthorizedPublicKeysList())
            );
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isDataInvalid() {
        try {
            return distributedData.isDataInvalid() ||
                    !getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes)) ||
                    !SignatureUtil.verify(distributedData.serialize(), signature, authorizedPublicKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }
}
