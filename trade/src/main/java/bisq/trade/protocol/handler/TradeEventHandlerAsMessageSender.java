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

package bisq.trade.protocol.handler;

import bisq.common.fsm.Event;
import bisq.network.SendMessageResult;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.protocol.messages.TradeMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class TradeEventHandlerAsMessageSender<T extends Trade<?, ?, ?>> extends TradeEventHandler<T> implements TradeMessageSender<T> {

    protected TradeEventHandlerAsMessageSender(ServiceProvider serviceProvider, T trade) {
        super(serviceProvider, trade);
    }

    public void handle(Event event) {
        super.handle(event);

        sendMessage();
    }

    abstract protected void sendMessage();

    protected CompletableFuture<SendMessageResult> send(TradeMessage message) {
        return sendMessage(message, serviceProvider, trade);
    }

}