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

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class ChatMessage {
    protected final String channelId;
    protected final String text;
    protected ChatUser author;
    protected final Optional<QuotedMessage> quotedMessage;
    protected final long date;
    protected final ChannelType channelType;
    protected final boolean wasEdited;
    protected final MetaData metaData;

    protected ChatMessage(String channelId,
                          ChatUser author,
                          String text,
                          Optional<QuotedMessage> quotedMessage,
                          long date,
                          ChannelType channelType,
                          boolean wasEdited,
                          MetaData metaData) {
        this.channelId = channelId;
        this.author = author;
        this.text = text;
        this.quotedMessage = quotedMessage;
        this.date = date;
        this.channelType = channelType;
        this.wasEdited = wasEdited;
        this.metaData = metaData;
    }

    bisq.social.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.social.protobuf.ChatMessage.Builder builder = bisq.social.protobuf.ChatMessage.newBuilder()
                .setChannelId(channelId)
                .setAuthor(author.toProto())
                .setText(text)
                .setDate(date)
                .setChannelType(channelType.toProto())
                .setWasEdited(wasEdited)
                .setMetaData(metaData.toProto());
        quotedMessage.ifPresent(quotedMessage -> builder.setQuotedMessage(quotedMessage.toProto()));
        return builder;
    }

    public static ChatMessage fromProto(bisq.social.protobuf.ChatMessage proto) {
        switch (proto.getMessageCase()) {
            case PRIVATECHATMESSAGE -> {
                return PrivateChatMessage.fromProto(proto);
            }
            case PUBLICCHATMESSAGE -> {
                return PublicChatMessage.fromProto(proto);
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}