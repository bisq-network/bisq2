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

package bisq.bonded_roles.node.bisq1_bridge.data;

import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedRoleData implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(100);
    // The pubKeys which are authorized for publishing that data.
    // todo Production key not set yet - we use devMode key only yet
    private static final Set<String> authorizedPublicKeys = Set.of();

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedBondedRoleData.class.getSimpleName());

    private final String profileId;
    private final String roleType;
    private final String bondUserName;
    private final String signature;
    AuthorizedOracleNode oracleNode;

    public AuthorizedBondedRoleData(String profileId, String roleType, String bondUserName, String signature, AuthorizedOracleNode oracleNode) {
        this.profileId = profileId;
        this.roleType = roleType;
        this.bondUserName = bondUserName;
        this.signature = signature;
        this.oracleNode = oracleNode;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRoleData toProto() {
        return bisq.bonded_roles.protobuf.AuthorizedBondedRoleData.newBuilder()
                .setProfileId(profileId)
                .setRoleType(roleType)
                .setBondUserName(bondUserName)
                .setSignature(signature)
                .setOracleNode(oracleNode.toProto())
                .build();
    }

    public static AuthorizedBondedRoleData fromProto(bisq.bonded_roles.protobuf.AuthorizedBondedRoleData proto) {
        return new AuthorizedBondedRoleData(proto.getProfileId(),
                proto.getRoleType(),
                proto.getBondUserName(),
                proto.getSignature(),
                AuthorizedOracleNode.fromProto(proto.getOracleNode()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedBondedRoleData.class));
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
            return authorizedPublicKeys;
        }
    }
}