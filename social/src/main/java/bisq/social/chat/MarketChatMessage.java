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

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.offer.MarketChatOffer;
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
public class MarketChatMessage extends PublicChatMessage implements DistributedData {
    private final Optional<MarketChatOffer> marketChatOffer;

    public MarketChatMessage(String channelId,
                             ChatUser sender,
                             Optional<MarketChatOffer> marketChatOffer,
                             Optional<String> text,
                             Optional<QuotedMessage> quotedMessage,
                             long date,
                             boolean wasEdited) {
        this(channelId,
                sender,
                marketChatOffer,
                text,
                quotedMessage,
                date,
                wasEdited,
                new MetaData(TimeUnit.DAYS.toMillis(10), 100000, MarketChatMessage.class.getSimpleName()));
    }

    public MarketChatMessage(String channelId,
                              ChatUser sender,
                              Optional<MarketChatOffer> marketChatOffer,
                              Optional<String> text,
                              Optional<QuotedMessage> quotedMessage,
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
        this.marketChatOffer = marketChatOffer;
    }

    public bisq.social.protobuf.ChatMessage toProto() {
        bisq.social.protobuf.MarketChatMessage.Builder builder = bisq.social.protobuf.MarketChatMessage.newBuilder();
        marketChatOffer.ifPresent(marketChatOffer -> builder.setMarketChatOffer(marketChatOffer.toProto()));
        return getChatMessageBuilder().setMarketChatMessage(builder).build();
    }

    public static MarketChatMessage fromProto(bisq.social.protobuf.ChatMessage baseProto) {
        Optional<QuotedMessage> quotedMessage = baseProto.hasQuotedMessage() ?
                Optional.of(QuotedMessage.fromProto(baseProto.getQuotedMessage())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();
        Optional<MarketChatOffer> marketChatOffer = baseProto.getMarketChatMessage().hasMarketChatOffer() ?
                Optional.of(MarketChatOffer.fromProto(baseProto.getMarketChatMessage().getMarketChatOffer())) :
                Optional.empty();
        return new MarketChatMessage(
                baseProto.getChannelId(),
                ChatUser.fromProto(baseProto.getAuthor()),
                marketChatOffer,
                text,
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    @Override
    public String getText() {
        return marketChatOffer.map(MarketChatOffer::getChatMessageText).orElse(optionalText.orElse(""));
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