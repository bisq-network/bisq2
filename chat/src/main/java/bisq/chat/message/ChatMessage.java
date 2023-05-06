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

package bisq.chat.message;

import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.common.proto.Proto;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.user.identity.UserIdentity;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class ChatMessage implements Proto {
    //todo for dev testing we keep it short.
    public final static long TTL = TimeUnit.DAYS.toMillis(1);

    protected final String messageId;
    private final ChatChannelDomain chatChannelDomain;
    protected final String channelName;
    protected final Optional<String> optionalText;
    protected String authorId;
    protected final Optional<Citation> citation;
    protected final long date;
    protected final boolean wasEdited;
    protected final ChatMessageType chatMessageType;
    protected final MetaData metaData;

    protected ChatMessage(String messageId,
                          ChatChannelDomain chatChannelDomain,
                          String channelName,
                          String authorId,
                          Optional<String> text,
                          Optional<Citation> citation,
                          long date,
                          boolean wasEdited,
                          ChatMessageType chatMessageType,
                          MetaData metaData) {
        this.messageId = messageId;
        this.chatChannelDomain = chatChannelDomain;
        this.channelName = channelName;
        this.authorId = authorId;
        this.optionalText = text;
        this.citation = citation;
        this.date = date;
        this.wasEdited = wasEdited;
        this.chatMessageType = chatMessageType;
        this.metaData = metaData;
    }

    public String getText() {
        return optionalText.orElse(Res.get("na"));
    }

    public boolean wasMentioned(UserIdentity userIdentity) {
        return getText().contains("@" + userIdentity.getUserName());
    }

    public bisq.chat.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.chat.protobuf.ChatMessage.Builder builder = bisq.chat.protobuf.ChatMessage.newBuilder()
                .setMessageId(messageId)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChannelName(channelName)
                .setAuthorId(authorId)
                .setDate(date)
                .setWasEdited(wasEdited)
                .setChatMessageType(chatMessageType.toProto())
                .setMetaData(metaData.toProto());
        citation.ifPresent(citation -> builder.setCitation(citation.toProto()));
        optionalText.ifPresent(builder::setText);
        return builder;
    }

    public static ChatMessage fromProto(bisq.chat.protobuf.ChatMessage proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATMESSAGE: {
                return TwoPartyPrivateChatMessage.fromProto(proto);
            }

            case PUBLICBISQEASYOFFERCHATMESSAGE: {
                return BisqEasyPublicChatMessage.fromProto(proto);
            }
            case PRIVATEBISQEASYTRADECHATMESSAGE: {
                return BisqEasyPrivateTradeChatMessage.fromProto(proto);
            }

            case COMMONPUBLICCHATMESSAGE: {
                return CommonPublicChatMessage.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
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
    }
}