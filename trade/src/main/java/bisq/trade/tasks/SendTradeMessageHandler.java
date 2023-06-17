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

package bisq.trade.tasks;

import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.trade.Trade;
import bisq.trade.TradeMessage;
import bisq.trade.bisq_easy.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SendTradeMessageHandler<M extends Trade<?, ?, ?>> extends TradeEventHandler<M> {

    protected SendTradeMessageHandler(ServiceProvider serviceProvider, M model) {
        super(serviceProvider, model);
    }

    protected void sendMessage(TradeMessage message, NetworkId receiverNetworkId, NetworkIdWithKeyPair senderNetworkIdWithKeyPair) {
        serviceProvider.getNetworkService().confidentialSend(message, receiverNetworkId, senderNetworkIdWithKeyPair)
                .whenComplete((result, throwable) -> {
                });
    }
}