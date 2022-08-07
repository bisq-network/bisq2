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

package bisq.oracle.timestamp;

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
public final class AuthorizeTimestampRequest implements MailboxMessage {
    private final String profileId;
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            AuthorizeTimestampRequest.class.getSimpleName());

    public AuthorizeTimestampRequest(String profileId) {
        this.profileId = profileId;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeTimestampRequestProto())))
                .build();
    }

    private bisq.oracle.protobuf.AuthorizeTimestampRequest toAuthorizeTimestampRequestProto() {
        return bisq.oracle.protobuf.AuthorizeTimestampRequest.newBuilder()
                .setProfileId(profileId)
                .build();
    }

    public static AuthorizeTimestampRequest fromProto(bisq.oracle.protobuf.AuthorizeTimestampRequest proto) {
        return new AuthorizeTimestampRequest(proto.getProfileId());
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.oracle.protobuf.AuthorizeTimestampRequest proto = any.unpack(bisq.oracle.protobuf.AuthorizeTimestampRequest.class);
                return AuthorizeTimestampRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}