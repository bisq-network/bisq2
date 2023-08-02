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

package bisq.chat.bisqeasy.channel.pub;

import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class BisqEasyPublicChatChannel extends PublicChatChannel<BisqEasyPublicChatMessage> {
    static String createId(Market market) {
        return ChatChannelDomain.BISQ_EASY.name().toLowerCase() + "." +
                market.getBaseCurrencyCode() + "-" +
                market.getQuoteCurrencyCode();
    }

    private final Market market;

    public BisqEasyPublicChatChannel(Market market) {
        this(createId(market), market);
    }

    private BisqEasyPublicChatChannel(String id, Market market) {
        super(id, ChatChannelDomain.BISQ_EASY, ChatChannelNotificationType.ALL);

        this.market = market;
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        return getChatChannelBuilder().setBisqEasyPublicChatChannel(bisq.chat.protobuf.BisqEasyPublicChatChannel.newBuilder()
                        .setMarket(market.toProto()))
                .build();
    }

    public static BisqEasyPublicChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                      bisq.chat.protobuf.BisqEasyPublicChatChannel proto) {
        return new BisqEasyPublicChatChannel(
                baseProto.getId(),
                Market.fromProto(proto.getMarket()));
    }

    @Override
    public String getDisplayString() {
        return market.getMarketCodes();
    }

    public String getDescription() {
        return Res.get("bisqEasy.tradeChannel.description", market.toString());
    }
}