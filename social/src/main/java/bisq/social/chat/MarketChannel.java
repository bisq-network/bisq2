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

import bisq.common.monetary.Market;
import bisq.common.observable.ObservableSet;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class MarketChannel extends Channel<MarketChatMessage> {
    private final Market market;

    private transient final ObservableSet<MarketChatMessage> chatMessages = new ObservableSet<>();

    MarketChannel(Market market) {
        this(market.toString(), market);
    }

    private MarketChannel(String id, Market market) {
        super(id, NotificationSetting.MENTION);

        this.market = market;
    }

    @Override
    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setMarketChannel(bisq.social.protobuf.MarketChannel.newBuilder()
                        .setMarket(market.toProto()))
                .build();
    }

    public static MarketChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                          bisq.social.protobuf.MarketChannel proto) {
        return new MarketChannel(baseProto.getId(), Market.fromProto(proto.getMarket()));
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(MarketChatMessage chatMessage) {
        return chatMessage.toProto();
    }

    @Override
    public void addChatMessage(MarketChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(MarketChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<MarketChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    public String getDescription() {
        return Res.get("social.marketChannel.description", market.toString());
    }

}