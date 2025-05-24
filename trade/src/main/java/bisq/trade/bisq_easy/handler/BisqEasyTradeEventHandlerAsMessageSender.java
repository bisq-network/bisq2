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

package bisq.trade.bisq_easy.handler;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.fsm.Event;
import bisq.network.SendMessageResult;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.protocol.handler.TradeEventHandlerAsMessageSender;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class BisqEasyTradeEventHandlerAsMessageSender<T extends BisqEasyTrade, E extends Event> extends TradeEventHandlerAsMessageSender<T, E> {
    protected final BisqEasyTradeService tradeService;

    protected BisqEasyTradeEventHandlerAsMessageSender(ServiceProvider serviceProvider, T trade) {
        super(serviceProvider, trade);

        tradeService = serviceProvider.getBisqEasyTradeService();
    }

    public final void handle(Event event) {
        super.handle(event);

       // sendLogMessage();
    }

    //protected abstract void sendLogMessage();

    protected Optional<CompletableFuture<SendMessageResult>> sendLogMessage(String encoded) {
        BisqEasyOpenTradeChannelService openTradeChannelService = serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();
        return openTradeChannelService.findChannelByTradeId(trade.getId())
                .map(channel ->
                        openTradeChannelService.sendTradeLogMessage(encoded, channel));
    }

    @Override
    protected void persist() {
        tradeService.persist();
    }
}