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

package bisq.bonded_roles.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@ToString
@EqualsAndHashCode
public final class BondedRoleRegistrationRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            BondedRoleRegistrationRequest.class.getSimpleName());

    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signatureBase64;
    private final Map<Transport.Type, Address> addressByNetworkType;

    public BondedRoleRegistrationRequest(String profileId,
                                         String authorizedPublicKey,
                                         BondedRoleType bondedRoleType,
                                         String bondUserName,
                                         String signatureBase64,
                                         Map<Transport.Type, Address> addressByNetworkType) {
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.addressByNetworkType = addressByNetworkType;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeRoleRegistrationRequestProto())))
                .build();
    }

    public bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest toAuthorizeRoleRegistrationRequestProto() {
        return bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProto())
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .putAllAddressByNetworkType(addressByNetworkType.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().toProto())))
                .build();
    }

    public static BondedRoleRegistrationRequest fromProto(bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest proto) {
        Map<Transport.Type, Address> addressByNetworkType = proto.getAddressByNetworkTypeMap().entrySet().stream()
                .collect(Collectors.toMap(e -> ProtobufUtils.enumFromProto(Transport.Type.class, e.getKey()),
                        e -> Address.fromProto(e.getValue())));
        return new BondedRoleRegistrationRequest(proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignatureBase64(), addressByNetworkType);
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest proto = any.unpack(bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.class);
                return BondedRoleRegistrationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}