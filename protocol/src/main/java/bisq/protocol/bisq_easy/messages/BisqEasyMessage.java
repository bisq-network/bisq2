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

package bisq.protocol.bisq_easy.messages;

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class BisqEasyMessage implements MailboxMessage {
    public final static long TTL = TimeUnit.DAYS.toMillis(10);

    protected final MetaData metaData;

    protected BisqEasyMessage(MetaData metaData) {
        this.metaData = metaData;
    }
/*
    public bisq.chat.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.chat.protobuf.ChatMessage.Builder builder = bisq.chat.protobuf.ChatMessage.newBuilder()
                .setId(id)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChannelId(channelId)
                .setAuthorUserProfileId(authorUserProfileId)
                .setDate(date)
                .setWasEdited(wasEdited)
                .setChatMessageType(chatMessageType.toProto())
                .setMetaData(metaData.toProto());
        citation.ifPresent(citation -> builder.setCitation(citation.toProto()));
        optionalText.ifPresent(builder::setText);
        return builder;
    }

    public static ChatMessage fromProto(bisq.chat.protobuf.ChatMessage proto) {
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case PUBLICBISQEASYOFFERCHATMESSAGE: {
                        return BisqEasyPublicChatMessage.fromProto(proto);
                    }
                    case COMMONPUBLICCHATMESSAGE: {
                        return CommonPublicChatMessage.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public static ProtoResolver<NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case TWOPARTYPRIVATECHATMESSAGE: {
                        return TwoPartyPrivateChatMessage.fromProto(proto);
                    }

                    case PRIVATEBISQEASYTRADECHATMESSAGE: {
                        return BisqEasyPrivateTradeChatMessage.fromProto(proto);
                    }

                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }*/

}