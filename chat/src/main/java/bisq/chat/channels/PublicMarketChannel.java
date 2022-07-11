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

package bisq.chat.channels;

import bisq.common.currency.Market;
import bisq.common.observable.ObservableSet;
import bisq.i18n.Res;
import bisq.chat.ChannelNotificationType;
import bisq.chat.messages.PublicTradeChatMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicMarketChannel extends Channel<PublicTradeChatMessage> implements PublicChannel {

    @Getter
    private final Optional<Market> market;
    
    // todo move out
    @Setter
    private boolean isVisible;

    // We do not persist the messages as they are persisted in the P2P data store.
    private transient final ObservableSet<PublicTradeChatMessage> chatMessages = new ObservableSet<>();

    public PublicMarketChannel(Market market, boolean isVisible) {
        this(Optional.of(market), isVisible);
    }

    public PublicMarketChannel(Optional<Market> market, boolean isVisible) {
        this(getId(market), market, isVisible);
    }

    private PublicMarketChannel(String id, Optional<Market> market, boolean isVisible) {
        super(id, ChannelNotificationType.MENTION);

        this.market = market;
        this.isVisible = isVisible;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        bisq.chat.protobuf.PublicMarketChannel.Builder builder = bisq.chat.protobuf.PublicMarketChannel.newBuilder()
                .setIsVisible(isVisible);
        market.ifPresent(market -> builder.setMarket(market.toProto()));
        return getChannelBuilder().setPublicMarketChannel(builder).build();
    }

    public static PublicMarketChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                bisq.chat.protobuf.PublicMarketChannel proto) {
        Optional<Market> market = proto.hasMarket() ? Optional.of(Market.fromProto(proto.getMarket())) : Optional.empty();
        return new PublicMarketChannel(baseProto.getId(), market, baseProto.getPublicMarketChannel().getIsVisible());
    }

    @Override
    public void addChatMessage(PublicTradeChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PublicTradeChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PublicTradeChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    public String getDescription() {
        return market.map(market -> Res.get("social.marketChannel.description", market.toString()))
                .orElse(Res.get("social.marketChannel.description.any"));
    }

    public String getDisplayString() {
        return market.map(Market::getMarketCodes).orElseThrow();
    }

    //todo make market non-optional
    public static String getId(Optional<Market> market) {
        return market.map(Market::toString).orElseThrow();
    }
}