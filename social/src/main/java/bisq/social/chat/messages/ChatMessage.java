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

package bisq.social.chat.messages;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@ToString
@EqualsAndHashCode
public abstract class ChatMessage {
    public final static long TTL = TimeUnit.DAYS.toMillis(1);
    
    @Getter
    protected final String channelId;
    protected final Optional<String> optionalText;
    @Getter
    protected String authorId;
    @Getter
    protected final Optional<Quotation> quotation;
    @Getter
    protected final long date;
    @Getter
    protected final boolean wasEdited;
    @Getter
    protected final MetaData metaData;

    protected ChatMessage(String channelId,
                          String authorId,
                          Optional<String> text,
                          Optional<Quotation> quotation,
                          long date,
                          boolean wasEdited,
                          MetaData metaData) {
        this.channelId = channelId;
        this.authorId = authorId;
        this.optionalText = text;
        this.quotation = quotation;
        this.date = date;
        this.wasEdited = wasEdited;
        this.metaData = metaData;
    }

    public String getText() {
        return optionalText.orElse(Res.get("shared.na"));
    }

    public bisq.social.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.social.protobuf.ChatMessage.Builder builder = bisq.social.protobuf.ChatMessage.newBuilder()
                .setChannelId(channelId)
                .setAuthorId(authorId)
                .setDate(date)
                .setWasEdited(wasEdited)
                .setMetaData(metaData.toProto());
        quotation.ifPresent(quotedMessage -> builder.setQuotation(quotedMessage.toProto()));
        optionalText.ifPresent(builder::setText);
        return builder;
    }

    public static ChatMessage fromProto(bisq.social.protobuf.ChatMessage proto) {
        switch (proto.getMessageCase()) {
            case PRIVATETRADECHATMESSAGE -> {
                return PrivateTradeChatMessage.fromProto(proto);
            }
            case PRIVATEDISCUSSIONCHATMESSAGE -> {
                return PrivateDiscussionChatMessage.fromProto(proto);
            }
            case PUBLICTRADECHATMESSAGE -> {
                return PublicTradeChatMessage.fromProto(proto);
            }
            case PUBLICDISCUSSIONCHATMESSAGE -> {
                return PublicDiscussionChatMessage.fromProto(proto);
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.social.protobuf.ChatMessage proto = any.unpack(bisq.social.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case PUBLICTRADECHATMESSAGE -> {
                        return PublicTradeChatMessage.fromProto(proto);
                    }
                    case PUBLICDISCUSSIONCHATMESSAGE -> {
                        return PublicDiscussionChatMessage.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET -> {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.social.protobuf.ChatMessage proto = any.unpack(bisq.social.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case PRIVATETRADECHATMESSAGE -> {
                        return PrivateTradeChatMessage.fromProto(proto);
                    }
                    case PRIVATEDISCUSSIONCHATMESSAGE -> {
                        return PrivateDiscussionChatMessage.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET -> {
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