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
import bisq.common.annotation.ExcludeForHash;
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

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedOracleNode implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGHEST_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final int version;
    private final NetworkId networkId;
    private final String profileId;
    private final String authorizedPublicKey;
    private final String bondUserName;                // username from DAO proposal
    private final String signatureBase64;             // signature created by bond with username as message

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedOracleNode(NetworkId networkId,
                                String profileId,
                                String authorizedPublicKey,
                                String bondUserName,
                                String signatureBase64,
                                boolean staticPublicKeysProvided) {
        this(VERSION,
                networkId,
                profileId,
                authorizedPublicKey,
                bondUserName,
                signatureBase64,
                staticPublicKeysProvided);
    }

    public AuthorizedOracleNode(int version,
                                 NetworkId networkId,
                                 String profileId,
                                 String authorizedPublicKey,
                                 String bondUserName,
                                 String signatureBase64,
                                 boolean staticPublicKeysProvided) {
        this.version = version;
        this.networkId = networkId;
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.staticPublicKeysProvided = staticPublicKeysProvided;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validatePubKeyHex(authorizedPublicKey);
        NetworkDataValidation.validateBondUserName(bondUserName);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedOracleNode.Builder getBuilder(boolean serializeForHash) {
        return bisq.bonded_roles.protobuf.AuthorizedOracleNode.newBuilder()
                .setNetworkId(networkId.toProto(serializeForHash))
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedOracleNode toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedOracleNode fromProto(bisq.bonded_roles.protobuf.AuthorizedOracleNode proto) {
        return new AuthorizedOracleNode(
                proto.getVersion(),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                proto.getBondUserName(),
                proto.getSignatureBase64(),
                proto.getStaticPublicKeysProvided()
        );
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
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS;
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