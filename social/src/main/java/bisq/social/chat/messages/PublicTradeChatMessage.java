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

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.offer.TradeChatOffer;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PublicTradeChatMessage extends PublicDiscussionChatMessage implements DistributedData {
    private final Optional<TradeChatOffer> tradeChatOffer;

    public PublicTradeChatMessage(String channelId,
                                  ChatUser sender,
                                  Optional<TradeChatOffer> tradeChatOffer,
                                  Optional<String> text,
                                  Optional<Quotation> quotedMessage,
                                  long date,
                                  boolean wasEdited) {
        this(channelId,
                sender,
                tradeChatOffer,
                text,
                quotedMessage,
                date,
                wasEdited,
                new MetaData(TimeUnit.DAYS.toMillis(1), 100000, PublicTradeChatMessage.class.getSimpleName()));
    }

    public PublicTradeChatMessage(String channelId,
                                  ChatUser sender,
                                  Optional<TradeChatOffer> tradeChatOffer,
                                  Optional<String> text,
                                  Optional<Quotation> quotedMessage,
                                  long date,
                                  boolean wasEdited,
                                  MetaData metaData) {
        super(channelId,
                sender,
                text,
                quotedMessage,
                date,
                wasEdited,
                metaData);
        this.tradeChatOffer = tradeChatOffer;
    }

    public bisq.social.protobuf.ChatMessage toProto() {
        bisq.social.protobuf.PublicTradeChatMessage.Builder builder = bisq.social.protobuf.PublicTradeChatMessage.newBuilder();
        tradeChatOffer.ifPresent(tradeChatOffer -> builder.setTradeChatOffer(tradeChatOffer.toProto()));
        return getChatMessageBuilder().setPublicTradeChatMessage(builder).build();
    }

    public static PublicTradeChatMessage fromProto(bisq.social.protobuf.ChatMessage baseProto) {
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
                ChatUser.fromProto(baseProto.getAuthor()),
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
}