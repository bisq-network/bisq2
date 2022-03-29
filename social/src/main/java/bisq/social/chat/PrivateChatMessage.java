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

package bisq.social.chat;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.network.protobuf.NetworkMessage;
import bisq.social.user.ChatUser;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * PrivateChatMessage is sent as direct message to peer and in case peer is not online it can be stores as
 * mailbox message.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PrivateChatMessage extends ChatMessage implements MailboxMessage {
    public PrivateChatMessage(String channelId,
                              ChatUser sender,
                              String text,
                              Optional<QuotedMessage> quotedMessage,
                              long date,
                              boolean wasEdited) {
        this(channelId,
                sender,
                text,
                quotedMessage,
                date,
                ChannelType.PRIVATE,
                wasEdited,
                new MetaData(TimeUnit.DAYS.toMillis(10), 100000, PrivateChatMessage.class.getSimpleName()));
    }

    private PrivateChatMessage(String channelId,
                               ChatUser sender,
                               String text,
                               Optional<QuotedMessage> quotedMessage,
                               long date,
                               ChannelType channelType,
                               boolean wasEdited,
                               MetaData metaData) {
        super(channelId,
                sender,
                text,
                quotedMessage,
                date,
                channelType,
                wasEdited,
                metaData);
    }

    @Override
    public NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder().setAny(Any.pack(toChatMessageProto())))
                .build();
    }

    bisq.social.protobuf.ChatMessage toChatMessageProto() {
        return getChatMessageBuilder().setPrivateChatMessage(bisq.social.protobuf.PrivateChatMessage.newBuilder()).build();
    }

    public static PrivateChatMessage fromProto(bisq.social.protobuf.ChatMessage baseProto) {
        Optional<QuotedMessage> quotedMessage = baseProto.hasQuotedMessage() ?
                Optional.of(QuotedMessage.fromProto(baseProto.getQuotedMessage())) :
                Optional.empty();
        return new PrivateChatMessage(
                baseProto.getChannelId(),
                ChatUser.fromProto(baseProto.getAuthor()),
                baseProto.getText(),
                quotedMessage,
                baseProto.getDate(),
                ChannelType.fromProto(baseProto.getChannelType()),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.ChatMessage.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    // Required for MailboxMessage use case
    @Override
    public MetaData getMetaData() {
        return metaData;
    }
}