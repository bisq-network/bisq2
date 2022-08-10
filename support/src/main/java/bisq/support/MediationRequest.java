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

package bisq.support;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public final class MediationRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            MediationRequest.class.getSimpleName());
    private final UserProfile requester;
    private final UserProfile peer;

    public MediationRequest(UserProfile requester, UserProfile peer) {
        this.requester = requester;
        this.peer = peer;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeAccountAgeRequestProto())))
                .build();
    }

    private bisq.support.protobuf.MediationRequest toAuthorizeAccountAgeRequestProto() {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setRequester(requester.toProto())
                .setPeer(peer.toProto())
                .build();
    }

    public static MediationRequest fromProto(bisq.support.protobuf.MediationRequest proto) {
        return new MediationRequest(UserProfile.fromProto(proto.getRequester()),
                UserProfile.fromProto(proto.getPeer()));
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediationRequest proto = any.unpack(bisq.support.protobuf.MediationRequest.class);
                return MediationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}