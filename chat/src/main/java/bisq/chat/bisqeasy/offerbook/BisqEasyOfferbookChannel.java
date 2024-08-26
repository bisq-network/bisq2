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

package bisq.chat.bisqeasy.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.pub.PublicChatChannel;
import bisq.common.currency.Market;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class BisqEasyOfferbookChannel extends PublicChatChannel<BisqEasyOfferbookMessage> {
    static String createId(Market market) {
        return ChatChannelDomain.BISQ_EASY_OFFERBOOK.name().toLowerCase() + "." +
                market.getBaseCurrencyCode() + "-" +
                market.getQuoteCurrencyCode();
    }

    private final Market market;

    public BisqEasyOfferbookChannel(Market market) {
        this(createId(market), ChatChannelNotificationType.ALL, market);
    }

    private BisqEasyOfferbookChannel(String id,
                                     ChatChannelNotificationType chatChannelNotificationType,
                                     Market market) {
        super(id, ChatChannelDomain.BISQ_EASY_OFFERBOOK, chatChannelNotificationType);

        this.market = market;
    }

    @Override
    public bisq.chat.protobuf.ChatChannel.Builder getBuilder(boolean serializeForHash) {
        return getChatChannelBuilder().setBisqEasyOfferbookChannel(bisq.chat.protobuf.BisqEasyOfferbookChannel.newBuilder()
                .setMarket(market.toProto(serializeForHash)));
    }

    public static BisqEasyOfferbookChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                     bisq.chat.protobuf.BisqEasyOfferbookChannel proto) {
        return new BisqEasyOfferbookChannel(
                baseProto.getId(),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType()),
                Market.fromProto(proto.getMarket()));
    }

    @Override
    public String getDisplayString() {
        return market.getMarketCodes();
    }

    public String getDescription() {
        return Res.get("bisqEasy.offerBookChannel.description", market.toString());
    }

    public String getShortDescription() {
        return Res.get("bisqEasy.offerBookChannel.description", market.getFiatCurrencyName());
    }
}