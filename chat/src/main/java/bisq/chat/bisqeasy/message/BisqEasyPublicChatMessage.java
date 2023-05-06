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

package bisq.chat.bisqeasy.message;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.chat.message.PublicChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyPublicChatMessage extends PublicChatMessage implements BisqEasyOfferMessage {
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public BisqEasyPublicChatMessage(String channelName,
                                     String authorId,
                                     Optional<BisqEasyOffer> bisqEasyOffer,
                                     Optional<String> text,
                                     Optional<Citation> citation,
                                     long date,
                                     boolean wasEdited) {
        this(StringUtils.createShortUid(),
                ChatChannelDomain.TRADE,
                channelName,
                authorId,
                bisqEasyOffer,
                text,
                citation,
                date,
                wasEdited,
                ChatMessageType.TEXT,
                new MetaData(ChatMessage.TTL, 100000, BisqEasyPublicChatMessage.class.getSimpleName()));
    }

    private BisqEasyPublicChatMessage(String messageId,
                                      ChatChannelDomain chatChannelDomain,
                                      String channelName,
                                      String authorId,
                                      Optional<BisqEasyOffer> bisqEasyOffer,
                                      Optional<String> text,
                                      Optional<Citation> citation,
                                      long date,
                                      boolean wasEdited,
                                      ChatMessageType chatMessageType,
                                      MetaData metaData) {
        super(messageId,
                chatChannelDomain,
                channelName,
                authorId,
                text,
                citation,
                date,
                wasEdited,
                chatMessageType,
                metaData);
        this.bisqEasyOffer = bisqEasyOffer;
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        bisq.chat.protobuf.BisqEasyPublicChatMessage.Builder builder = bisq.chat.protobuf.BisqEasyPublicChatMessage.newBuilder();
        bisqEasyOffer.ifPresent(bisqEasyOffer -> builder.setBisqEasyOffer(bisqEasyOffer.toProto()));
        return getChatMessageBuilder().setPublicBisqEasyOfferChatMessage(builder).build();
    }

    public static BisqEasyPublicChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = baseProto.getPublicBisqEasyOfferChatMessage().hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(baseProto.getPublicBisqEasyOfferChatMessage().getBisqEasyOffer())) :
                Optional.empty();
        return new BisqEasyPublicChatMessage(
                baseProto.getMessageId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelName(),
                baseProto.getAuthorId(),
                bisqEasyOffer,
                text,
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    @Override
    public String getText() {
        return bisqEasyOffer.map(BisqEasyOffer::getChatMessageText).orElse(super.getText());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean hasTradeChatOffer() {
        return bisqEasyOffer.isPresent();
    }
}