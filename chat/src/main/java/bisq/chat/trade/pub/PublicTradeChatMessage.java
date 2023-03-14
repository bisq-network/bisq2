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

package bisq.chat.trade.pub;

import bisq.chat.channel.ChannelDomain;
import bisq.chat.message.BasePublicChatMessage;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.MessageType;
import bisq.chat.message.Quotation;
import bisq.chat.trade.TradeChatOffer;
import bisq.chat.trade.TradeChatOfferMessage;
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
public final class PublicTradeChatMessage extends BasePublicChatMessage implements TradeChatOfferMessage {
    private final Optional<TradeChatOffer> tradeChatOffer;

    public PublicTradeChatMessage(String channelName,
                                  String authorId,
                                  Optional<TradeChatOffer> tradeChatOffer,
                                  Optional<String> text,
                                  Optional<Quotation> quotedMessage,
                                  long date,
                                  boolean wasEdited) {
        this(StringUtils.createShortUid(),
                ChannelDomain.TRADE,
                channelName,
                authorId,
                tradeChatOffer,
                text,
                quotedMessage,
                date,
                wasEdited,
                MessageType.TEXT,
                new MetaData(ChatMessage.TTL, 100000, PublicTradeChatMessage.class.getSimpleName()));
    }

    private PublicTradeChatMessage(String messageId,
                                   ChannelDomain channelDomain,
                                   String channelName,
                                   String authorId,
                                   Optional<TradeChatOffer> tradeChatOffer,
                                   Optional<String> text,
                                   Optional<Quotation> quotedMessage,
                                   long date,
                                   boolean wasEdited,
                                   MessageType messageType,
                                   MetaData metaData) {
        super(messageId,
                channelDomain,
                channelName,
                authorId,
                text,
                quotedMessage,
                date,
                wasEdited,
                messageType,
                metaData);
        this.tradeChatOffer = tradeChatOffer;
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        bisq.chat.protobuf.PublicTradeChatMessage.Builder builder = bisq.chat.protobuf.PublicTradeChatMessage.newBuilder();
        tradeChatOffer.ifPresent(tradeChatOffer -> builder.setTradeChatOffer(tradeChatOffer.toProto()));
        return getChatMessageBuilder().setPublicTradeChatMessage(builder).build();
    }

    public static PublicTradeChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotation() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotation())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();
        Optional<TradeChatOffer> tradeChatOffer = baseProto.getPublicTradeChatMessage().hasTradeChatOffer() ?
                Optional.of(TradeChatOffer.fromProto(baseProto.getPublicTradeChatMessage().getTradeChatOffer())) :
                Optional.empty();
        return new PublicTradeChatMessage(
                baseProto.getMessageId(),
                ChannelDomain.fromProto(baseProto.getChannelDomain()),
                baseProto.getChannelName(),
                baseProto.getAuthorId(),
                tradeChatOffer,
                text,
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MessageType.fromProto(baseProto.getMessageType()),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    @Override
    public String getText() {
        return tradeChatOffer.map(TradeChatOffer::getChatMessageText).orElse(super.getText());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean hasTradeChatOffer() {
        return tradeChatOffer.isPresent();
    }
}