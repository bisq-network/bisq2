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

package bisq.social.chat.channels;

import bisq.common.currency.Market;
import bisq.common.observable.ObservableSet;
import bisq.i18n.Res;
import bisq.social.chat.NotificationSetting;
import bisq.social.chat.messages.PublicTradeChatMessage;
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
public class PublicTradeChannel extends Channel<PublicTradeChatMessage> implements PublicChannel {
    private static final String ID_ANY = "anyPublicTradeChannel";

    public Optional<Market> getMarket() {
        return market;
    }

    private final Optional<Market> market;
    @Setter
    private  boolean isVisible;

    private transient final ObservableSet<PublicTradeChatMessage> chatMessages = new ObservableSet<>();

    public PublicTradeChannel(Market market, boolean isVisible) {
        this(Optional.of(market), isVisible);
    }

    public PublicTradeChannel(Optional<Market> market, boolean isVisible) {
        this(getId(market), market, isVisible);
    }


    private PublicTradeChannel(String id, Optional<Market> market, boolean isVisible) {
        super(id, NotificationSetting.MENTION);

        this.market = market;
        this.isVisible = isVisible;
    }

    @Override
    public bisq.social.protobuf.Channel toProto() {
        bisq.social.protobuf.PublicTradeChannel.Builder builder = bisq.social.protobuf.PublicTradeChannel.newBuilder()
                .setIsVisible(isVisible);
        market.ifPresent(market -> builder.setMarket(market.toProto()));
        return getChannelBuilder().setPublicTradeChannel(builder).build();
    }

    public static PublicTradeChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                               bisq.social.protobuf.PublicTradeChannel proto) {
        Optional<Market> market = proto.hasMarket() ? Optional.of(Market.fromProto(proto.getMarket())) : Optional.empty();
        return new PublicTradeChannel(baseProto.getId(), market, baseProto.getPublicTradeChannel().getIsVisible());
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PublicTradeChatMessage chatMessage) {
        return chatMessage.toProto();
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
        return market.map(Market::toString).orElse(Res.get("tradeChat.addMarketChannel.any"));
    }

    public static String getId(Optional<Market> market) {
        return market.map(Market::toString).orElse(ID_ANY);
    }
}