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

import bisq.common.monetary.Market;
import bisq.common.observable.ObservableSet;
import bisq.i18n.Res;
import bisq.social.chat.NotificationSetting;
import bisq.social.chat.messages.PublicTradeChatMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PublicTradeChannel extends Channel<PublicTradeChatMessage> implements PublicChannel {
    private final Market market;

    private transient final ObservableSet<PublicTradeChatMessage> chatMessages = new ObservableSet<>();

    public PublicTradeChannel(Market market) {
        this(market.toString(), market);
    }

    private PublicTradeChannel(String id, Market market) {
        super(id, NotificationSetting.MENTION);

        this.market = market;
    }

    @Override
    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setPublicTradeChannel(bisq.social.protobuf.PublicTradeChannel.newBuilder()
                        .setMarket(market.toProto()))
                .build();
    }

    public static PublicTradeChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                               bisq.social.protobuf.PublicTradeChannel proto) {
        return new PublicTradeChannel(baseProto.getId(), Market.fromProto(proto.getMarket()));
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
        return Res.get("social.marketChannel.description", market.toString());
    }
}