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

package bisq.user.node;

import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.profile.UserProfile;
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
public final class AuthorizedNodeRegistrationData implements AuthorizedDistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(Long.MAX_VALUE);
    // The pubKeys which are authorized for publishing that data.
    // todo Production key not set yet - we use devMode key only yet
    public static final Set<String> authorizedPublicKeys = Set.of();

    private final MetaData metaData = new MetaData(TTL,
            100000,
            AuthorizedNodeRegistrationData.class.getSimpleName());

    private final UserProfile userProfile;
    private final NodeType nodeType;
    private final String publicKeyAsHex;
    private final Map<Transport.Type, Address> addressByNetworkType;

    public AuthorizedNodeRegistrationData(UserProfile userProfile, NodeType nodeType, String publicKeyAsHex, Map<Transport.Type, Address> addressByNetworkType) {
        this.userProfile = userProfile;
        this.nodeType = nodeType;
        this.publicKeyAsHex = publicKeyAsHex;
        this.addressByNetworkType = addressByNetworkType;
    }

    @Override
    public bisq.user.protobuf.AuthorizedNodeRegistrationData toProto() {
        return bisq.user.protobuf.AuthorizedNodeRegistrationData.newBuilder()
                .setUserProfile(userProfile.toProto())
                .setNodeType(nodeType.toProto())
                .setPublicKeyAsHex(publicKeyAsHex)
                .putAllAddressByNetworkType(addressByNetworkType.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().toProto())))
                .build();
    }

    public static AuthorizedNodeRegistrationData fromProto(bisq.user.protobuf.AuthorizedNodeRegistrationData proto) {
        Map<Transport.Type, Address> addressByNetworkType = proto.getAddressByNetworkTypeMap().entrySet().stream()
                .collect(Collectors.toMap(e -> ProtobufUtils.enumFromProto(Transport.Type.class, e.getKey()),
                        e -> Address.fromProto(e.getValue())));
        return new AuthorizedNodeRegistrationData(UserProfile.fromProto(proto.getUserProfile()),
                NodeType.fromProto(proto.getNodeType()),
                proto.getPublicKeyAsHex(),
                addressByNetworkType);
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.AuthorizedNodeRegistrationData.class));
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
        return !userProfile.getId().equals(Hex.encode(pubKeyHash));
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