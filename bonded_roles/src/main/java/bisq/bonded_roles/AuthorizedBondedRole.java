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

package bisq.bonded_roles;

import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedRole implements AuthorizedDistributedData, StaticallyAuthorizedPublicKeyValidation {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);

    // todo Production key not set yet - we use devMode key only yet
    private static final Set<String> AUTHORIZED_PUBLIC_KEYS = Set.of();

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedBondedRole.class.getSimpleName());

    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signature;
    private final Map<Transport.Type, Address> addressByNetworkType;
    private final AuthorizedOracleNode authorizedOracleNode;

    public AuthorizedBondedRole(String profileId,
                                String authorizedPublicKey,
                                BondedRoleType bondedRoleType,
                                String bondUserName,
                                String signature,
                                Map<Transport.Type, Address> addressByNetworkType,
                                AuthorizedOracleNode authorizedOracleNode) {
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signature = signature;
        this.addressByNetworkType = addressByNetworkType;
        this.authorizedOracleNode = authorizedOracleNode;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole toProto() {
        return bisq.bonded_roles.protobuf.AuthorizedBondedRole.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProto())
                .setBondUserName(bondUserName)
                .setSignature(signature)
                .putAllAddressByNetworkType(addressByNetworkType.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().toProto())))
                .setOracleNode(authorizedOracleNode.toProto())
                .build();
    }

    public static AuthorizedBondedRole fromProto(bisq.bonded_roles.protobuf.AuthorizedBondedRole proto) {
        Map<Transport.Type, Address> addressByNetworkType = proto.getAddressByNetworkTypeMap().entrySet().stream()
                .collect(Collectors.toMap(e -> ProtobufUtils.enumFromProto(Transport.Type.class, e.getKey()),
                        e -> Address.fromProto(e.getValue())));
        return new AuthorizedBondedRole(proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignature(),
                addressByNetworkType,
                AuthorizedOracleNode.fromProto(proto.getOracleNode()));
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
    public MetaData getMetaData() {
        return metaData;
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
            return AUTHORIZED_PUBLIC_KEYS;
        }
    }
}