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

package bisq.bonded_roles.oracle;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_100_DAYS;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedOracleNode implements AuthorizedDistributedData {
    private final MetaData metaData = new MetaData(TTL_100_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    private final NetworkId networkId;
    private final String profileId;
    private final String authorizedPublicKey;
    private final String bondUserName;                // username from DAO proposal
    private final String signatureBase64;             // signature created by bond with username as message
    private final boolean staticPublicKeysProvided;

    public AuthorizedOracleNode(NetworkId networkId,
                                String profileId,
                                String authorizedPublicKey,
                                String bondUserName,
                                String signatureBase64,
                                boolean staticPublicKeysProvided) {
        this.networkId = networkId;
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validatePubKeyHex(authorizedPublicKey);
        NetworkDataValidation.validateBondUserName(bondUserName);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize());//326
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedOracleNode toProto() {
        return bisq.bonded_roles.protobuf.AuthorizedOracleNode.newBuilder()
                .setNetworkId(networkId.toProto())
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static AuthorizedOracleNode fromProto(bisq.bonded_roles.protobuf.AuthorizedOracleNode proto) {
        return new AuthorizedOracleNode(NetworkId.fromProto(proto.getNetworkId()),
                proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                proto.getBondUserName(),
                proto.getSignatureBase64(),
                proto.getStaticPublicKeysProvided());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedOracleNode.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return !Arrays.equals(networkId.getPubKey().getHash(), pubKeyHash);
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return DevMode.AUTHORIZED_DEV_PUBLIC_KEYS;
        } else {
            return AuthorizedPubKeys.KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "AuthorizedOracleNode{" +
                "metaData=" + metaData +
                ", networkId=" + networkId +
                ", profileId='" + profileId + '\'' +
                ", authorizedPublicKey='" + authorizedPublicKey + '\'' +
                ", bondUserName='" + bondUserName + '\'' +
                ", signatureBase64='" + signatureBase64 + '\'' +
                ", staticPublicKeysProvided=" + staticPublicKeysProvided +
                '}';
    }
}