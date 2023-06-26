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

package bisq.trade.protocol.events;

import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyTradeMessage;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class TradeMessageHandler<M extends Trade<?, ?, ?>, S extends BisqEasyTradeMessage> extends TradeEventHandler<M> {

    protected TradeMessageHandler(ServiceProvider serviceProvider, M model) {
        super(serviceProvider, model);
    }

    protected void verifyMessage(S message) {
        checkArgument(message.getTradeId().equals(trade.getId()));
        checkArgument(message.getSender().equals(trade.getPeer().getNetworkId()));
    }
}