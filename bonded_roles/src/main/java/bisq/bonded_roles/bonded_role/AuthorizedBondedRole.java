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
import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedRole implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);

    private final MetaData metaData = new MetaData(TTL, 100_000, AuthorizedBondedRole.class.getSimpleName());
    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signature;
    private final Map<Transport.Type, Address> addressByNetworkType;
    private final NetworkId networkId;
    private final Optional<AuthorizedOracleNode> authorizedOracleNode;
    private final boolean staticPublicKeysProvided;

    public AuthorizedBondedRole(String profileId,
                                String authorizedPublicKey,
                                BondedRoleType bondedRoleType,
                                String bondUserName,
                                String signature,
                                Map<Transport.Type, Address> addressByNetworkType,
                                NetworkId networkId,
                                Optional<AuthorizedOracleNode> authorizedOracleNode,
                                boolean staticPublicKeysProvided) {
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signature = signature;
        this.addressByNetworkType = addressByNetworkType;
        this.networkId = networkId;
        this.authorizedOracleNode = authorizedOracleNode;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole toProto() {
        bisq.bonded_roles.protobuf.AuthorizedBondedRole.Builder builder = bisq.bonded_roles.protobuf.AuthorizedBondedRole.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProto())
                .setBondUserName(bondUserName)
                .setSignature(signature)
                .setNetworkId(networkId.toProto())
                .addAllAddressNetworkTypeTuple(NetworkId.AddressTransportTypeTuple.mapToProtoList(addressByNetworkType))
                .setStaticPublicKeysProvided(staticPublicKeysProvided);
        authorizedOracleNode.ifPresent(oracleNode -> builder.setAuthorizedOracleNode(oracleNode.toProto()));
        return builder.build();
    }

    public static AuthorizedBondedRole fromProto(bisq.bonded_roles.protobuf.AuthorizedBondedRole proto) {
        return new AuthorizedBondedRole(proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignature(),
                NetworkId.AddressTransportTypeTuple.protoListToMap(proto.getAddressNetworkTypeTupleList()),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.hasAuthorizedOracleNode() ? Optional.of(AuthorizedOracleNode.fromProto(proto.getAuthorizedOracleNode())) : Optional.empty(),
                proto.getStaticPublicKeysProvided());
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
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
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
        return "AuthorizedBondedRole{" +
                "\r\n                    metaData=" + metaData +
                ",\r\n                    profileId='" + profileId + '\'' +
                ",\r\n                    authorizedPublicKey='" + authorizedPublicKey + '\'' +
                ",\r\n                    bondedRoleType=" + bondedRoleType +
                ",\r\n                    bondUserName='" + bondUserName + '\'' +
                ",\r\n                    signature='" + signature + '\'' +
                ",\r\n                    networkId=" + networkId +
                ",\r\n                    addressByNetworkType=" + addressByNetworkType +
                ",\r\n                    authorizedOracleNode=" + authorizedOracleNode +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}