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

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedRole implements AuthorizedDistributedData {
    private static final int VERSION = 1;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGHEST_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final int version;
    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signatureBase64;
    private final Optional<AddressByTransportTypeMap> addressByTransportTypeMap;
    private final NetworkId networkId;
    // The oracle node which did the validation and publishing
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final Optional<AuthorizedOracleNode> authorizingOracleNode;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    public AuthorizedBondedRole(String profileId,
                                String authorizedPublicKey,
                                BondedRoleType bondedRoleType,
                                String bondUserName,
                                String signatureBase64,
                                Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                NetworkId networkId,
                                Optional<AuthorizedOracleNode> authorizingOracleNode,
                                boolean staticPublicKeysProvided) {
        this(VERSION,
                profileId,
                authorizedPublicKey,
                bondedRoleType,
                bondUserName,
                signatureBase64,
                addressByTransportTypeMap,
                networkId,
                authorizingOracleNode,
                staticPublicKeysProvided);
    }

    public AuthorizedBondedRole(int version,
                                 String profileId,
                                 String authorizedPublicKey,
                                 BondedRoleType bondedRoleType,
                                 String bondUserName,
                                 String signatureBase64,
                                 Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                 NetworkId networkId,
                                 Optional<AuthorizedOracleNode> authorizingOracleNode,
                                 boolean staticPublicKeysProvided) {
        this.version = version;
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.addressByTransportTypeMap = addressByTransportTypeMap;
        this.networkId = networkId;
        this.authorizingOracleNode = authorizingOracleNode;
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
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole.Builder getBuilder(boolean serializeForHash) {
        bisq.bonded_roles.protobuf.AuthorizedBondedRole.Builder builder = bisq.bonded_roles.protobuf.AuthorizedBondedRole.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProtoEnum())
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .setNetworkId(networkId.toProto(serializeForHash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version);
        addressByTransportTypeMap.ifPresent(e -> builder.setAddressByTransportTypeMap(e.toProto(serializeForHash)));
        authorizingOracleNode.ifPresent(oracleNode -> builder.setAuthorizingOracleNode(oracleNode.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static AuthorizedBondedRole fromProto(bisq.bonded_roles.protobuf.AuthorizedBondedRole proto) {
        return new AuthorizedBondedRole(
                proto.getVersion(),
                proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignatureBase64(),
                proto.hasAddressByTransportTypeMap() ?
                        Optional.of(AddressByTransportTypeMap.fromProto(proto.getAddressByTransportTypeMap())) :
                        Optional.empty(),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.hasAuthorizingOracleNode() ?
                        Optional.of(AuthorizedOracleNode.fromProto(proto.getAuthorizingOracleNode())) :
                        Optional.empty(),
                proto.getStaticPublicKeysProvided()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedBondedRole.class));
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
        return false;
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
        return "AuthorizedBondedRole{" +
                "\r\n                    bondedRoleType=" + bondedRoleType +
                ",\r\n                    profileId='" + profileId + '\'' +
                ",\r\n                    authorizedPublicKey='" + authorizedPublicKey + '\'' +
                ",\r\n                    bondUserName='" + bondUserName + '\'' +
                ",\r\n                    signature='" + signatureBase64 + '\'' +
                ",\r\n                    networkId=" + networkId +
                ",\r\n                    addressByTransportTypeMap=" + addressByTransportTypeMap +
                ",\r\n                    authorizedOracleNode=" + authorizingOracleNode +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    metaData=" + metaData +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}