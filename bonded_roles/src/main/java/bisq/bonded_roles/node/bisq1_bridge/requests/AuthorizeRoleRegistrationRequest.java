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

package bisq.bonded_roles.node.bisq1_bridge.requests;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public final class AuthorizeRoleRegistrationRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            AuthorizeRoleRegistrationRequest.class.getSimpleName());

    private final String profileId;
    private final String roleType;
    private final String bondUserName;
    private final String signatureBase64;

    public AuthorizeRoleRegistrationRequest(String profileId, String roleType, String bondUserName, String signatureBase64) {
        this.profileId = profileId;
        this.roleType = roleType;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeRoleRegistrationRequestProto())))
                .build();
    }

    public bisq.bonded_roles.protobuf.AuthorizeRoleRegistrationRequest toAuthorizeRoleRegistrationRequestProto() {
        return bisq.bonded_roles.protobuf.AuthorizeRoleRegistrationRequest.newBuilder()
                .setProfileId(profileId)
                .setRoleType(roleType)
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .build();
    }

    public static AuthorizeRoleRegistrationRequest fromProto(bisq.bonded_roles.protobuf.AuthorizeRoleRegistrationRequest proto) {
        return new AuthorizeRoleRegistrationRequest(proto.getProfileId(),
                proto.getRoleType(),
                proto.getBondUserName(),
                proto.getSignatureBase64());
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.bonded_roles.protobuf.AuthorizeRoleRegistrationRequest proto = any.unpack(bisq.bonded_roles.protobuf.AuthorizeRoleRegistrationRequest.class);
                return AuthorizeRoleRegistrationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}