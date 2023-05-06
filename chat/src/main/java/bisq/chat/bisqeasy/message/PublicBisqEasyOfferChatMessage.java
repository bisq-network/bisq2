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
import bisq.chat.message.MessageType;
import bisq.chat.message.PublicChatMessage;
import bisq.chat.message.Quotation;
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
public final class PublicBisqEasyOfferChatMessage extends PublicChatMessage implements BisqEasyOfferMessage {
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public PublicBisqEasyOfferChatMessage(String channelName,
                                          String authorId,
                                          Optional<BisqEasyOffer> bisqEasyOffer,
                                          Optional<String> text,
                                          Optional<Quotation> quotedMessage,
                                          long date,
                                          boolean wasEdited) {
        this(StringUtils.createShortUid(),
                ChatChannelDomain.TRADE,
                channelName,
                authorId,
                bisqEasyOffer,
                text,
                quotedMessage,
                date,
                wasEdited,
                MessageType.TEXT,
                new MetaData(ChatMessage.TTL, 100000, PublicBisqEasyOfferChatMessage.class.getSimpleName()));
    }

    private PublicBisqEasyOfferChatMessage(String messageId,
                                           ChatChannelDomain chatChannelDomain,
                                           String channelName,
                                           String authorId,
                                           Optional<BisqEasyOffer> bisqEasyOffer,
                                           Optional<String> text,
                                           Optional<Quotation> quotedMessage,
                                           long date,
                                           boolean wasEdited,
                                           MessageType messageType,
                                           MetaData metaData) {
        super(messageId,
                chatChannelDomain,
                channelName,
                authorId,
                text,
                quotedMessage,
                date,
                wasEdited,
                messageType,
                metaData);
        this.bisqEasyOffer = bisqEasyOffer;
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        bisq.chat.protobuf.PublicBisqEasyOfferChatMessage.Builder builder = bisq.chat.protobuf.PublicBisqEasyOfferChatMessage.newBuilder();
        bisqEasyOffer.ifPresent(bisqEasyOffer -> builder.setBisqEasyOffer(bisqEasyOffer.toProto()));
        return getChatMessageBuilder().setPublicBisqEasyOfferChatMessage(builder).build();
    }

    public static PublicBisqEasyOfferChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotation() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotation())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = baseProto.getPublicBisqEasyOfferChatMessage().hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(baseProto.getPublicBisqEasyOfferChatMessage().getBisqEasyOffer())) :
                Optional.empty();
        return new PublicBisqEasyOfferChatMessage(
                baseProto.getMessageId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelName(),
                baseProto.getAuthorId(),
                bisqEasyOffer,
                text,
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MessageType.fromProto(baseProto.getMessageType()),
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