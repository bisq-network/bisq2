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

import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PublicChannel;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicTradeChannel extends PublicChannel<PublicTradeChatMessage> {
    private final Market market;

    // todo move out
    @Setter
    private boolean isVisible;

    public PublicTradeChannel(Market market, boolean isVisible) {
        this(getId(market), market, isVisible);
    }

    private PublicTradeChannel(String id, Market market, boolean isVisible) {
        super(id, ChannelNotificationType.MENTION);

        this.market = market;
        this.isVisible = isVisible;
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPublicTradeChannel(bisq.chat.protobuf.PublicTradeChannel.newBuilder()
                        .setIsVisible(isVisible)
                        .setMarket(market.toProto()))
                .build();
    }

    public static PublicTradeChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                               bisq.chat.protobuf.PublicTradeChannel proto) {
        return new PublicTradeChannel(baseProto.getId(),
                Market.fromProto(proto.getMarket()),
                baseProto.getPublicTradeChannel().getIsVisible());
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
        return Res.get("social.marketChannel.description", market.toString());
    }

    public String getDisplayString() {
        return market.getMarketCodes();
    }

    public static String getId(Market market) {
        return market.toString();
    }
}