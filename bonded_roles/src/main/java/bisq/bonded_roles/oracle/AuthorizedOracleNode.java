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

import bisq.common.application.DevMode;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedOracleNode implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(30);

    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> AUTHORIZED_PUBLIC_KEYS = Set.of("3056301006072a8648ce3d020106052b8104000a03420004b9f698d9644d01193eaa2e7a823570aeea50e4f96749305db523c010e998b3a8f2ef0a567bb9282e80ff66b6de8f0df39d242f609728def1dbaa6f1862429188");

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedOracleNode.class.getSimpleName());

    private final NetworkId networkId;
    private final String bondUserName;          // username from DAO proposal
    private final String signature;   // signature created by bond with username as message
    private final boolean staticPublicKeysProvided;

    public AuthorizedOracleNode(NetworkId networkId,
                                String bondUserName,
                                String signature,
                                boolean staticPublicKeysProvided) {
        this.networkId = networkId;
        this.bondUserName = bondUserName;
        this.signature = signature;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedOracleNode toProto() {
        return bisq.bonded_roles.protobuf.AuthorizedOracleNode.newBuilder()
                .setNetworkId(networkId.toProto())
                .setBondUserName(bondUserName)
                .setSignature(signature)
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .build();
    }

    public static AuthorizedOracleNode fromProto(bisq.bonded_roles.protobuf.AuthorizedOracleNode proto) {
        return new AuthorizedOracleNode(NetworkId.fromProto(proto.getNetworkId()),
                proto.getBondUserName(),
                proto.getSignature(),
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
    public MetaData getMetaData() {
        return metaData;
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
            return AUTHORIZED_PUBLIC_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "AuthorizedOracleNode{" +
                "\r\n                    networkId=" + networkId +
                ",\r\n                    metaData=" + metaData +
                ",\r\n                    verifyStaticPublicKeys=" + staticPublicKeysProvided() +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}