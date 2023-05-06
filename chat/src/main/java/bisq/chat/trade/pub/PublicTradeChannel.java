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
import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PublicChannel;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicTradeChannel extends PublicChannel<PublicTradeChatMessage> {
    public static String getChannelName(Market market) {
        return market.toString();
    }

    private final Market market;

    public PublicTradeChannel(Market market) {
        this(getChannelName(market), market);
    }

    private PublicTradeChannel(String channelName, Market market) {
        super(ChannelDomain.TRADE, channelName, ChannelNotificationType.ALL);

        this.market = market;
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        return getChannelBuilder().setPublicTradeChannel(bisq.chat.protobuf.PublicTradeChannel.newBuilder()
                        .setMarket(market.toProto()))
                .build();
    }

    public static PublicTradeChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                               bisq.chat.protobuf.PublicTradeChannel proto) {
        PublicTradeChannel publicTradeChannel = new PublicTradeChannel(baseProto.getChannelName(), Market.fromProto(proto.getMarket()));
        publicTradeChannel.getSeenChatMessageIds().addAll(new HashSet<>(baseProto.getSeenChatMessageIdsList()));
        return publicTradeChannel;
    }

    public String getDescription() {
        return Res.get("social.marketChannel.description", market.toString());
    }

    public String getDisplayString() {
        return market.getMarketCodes();
    }
}