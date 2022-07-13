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

import bisq.chat.message.ChatMessage;
import bisq.chat.message.PublicChatMessage;
import bisq.chat.message.Quotation;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PublicTradeChatMessage extends PublicChatMessage {
    private final Optional<TradeChatOffer> tradeChatOffer;

    public PublicTradeChatMessage(String channelId,
                                  String authorId,
                                  Optional<TradeChatOffer> tradeChatOffer,
                                  Optional<String> text,
                                  Optional<Quotation> quotedMessage,
                                  long date,
                                  boolean wasEdited) {
        this(channelId,
                authorId,
                tradeChatOffer,
                text,
                quotedMessage,
                date,
                wasEdited,
                new MetaData(ChatMessage.TTL, 100000, PublicTradeChatMessage.class.getSimpleName()));
    }

    private PublicTradeChatMessage(String channelId,
                                   String authorId,
                                   Optional<TradeChatOffer> tradeChatOffer,
                                   Optional<String> text,
                                   Optional<Quotation> quotedMessage,
                                   long date,
                                   boolean wasEdited,
                                   MetaData metaData) {
        super(channelId,
                authorId,
                text,
                quotedMessage,
                date,
                wasEdited,
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
                baseProto.getChannelId(),
                baseProto.getAuthorId(),
                tradeChatOffer,
                text,
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
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
    public boolean isDataInvalid() {
        return false;
    }

    public boolean hasTradeChatOffer() {
        return tradeChatOffer.isPresent();
    }
}