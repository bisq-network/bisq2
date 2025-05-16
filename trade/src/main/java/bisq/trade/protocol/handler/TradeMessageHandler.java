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
import bisq.common.fsm.EventHandler;
import bisq.network.identity.NetworkId;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.protocol.messages.TradeMessage;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class TradeMessageHandler<T extends Trade<?, ?, ?>, M extends TradeMessage> implements EventHandler {
    protected final ServiceProvider serviceProvider;
    protected final T trade;

    protected TradeMessageHandler(ServiceProvider serviceProvider, T trade) {
        this.serviceProvider = serviceProvider;
        this.trade = trade;
    }

    public void handle(Event event) {
        if (event instanceof TradeMessage tradeMessage) {
            M message = unsafeCast(tradeMessage);
            verifyInternal(message);
            verify(message);
            process(message);
            commit();
        } else {
            throw new IllegalArgumentException("Event must be a subclass of TradeMessage in " + getClass().getSimpleName());
        }
    }

    protected abstract void verify(M message);

    protected abstract void process(M message);

    protected abstract void commit();

    private void verifyInternal(M message) {
        checkArgument(message.getTradeId().equals(trade.getId()),
                "TradeId of message not matching the tradeId from the trade data");
        NetworkId sender = message.getSender();
        checkArgument(sender.equals(trade.getPeer().getNetworkId()),
                "Message sender networkID not matching the peers networkId from the trade data");
        // We verify if the sender of the message is banned at the message handler in the service class.
        // As the message handler is optional we prefer to block banned messages at the level instead of handling it here.
    }

    @SuppressWarnings("unchecked")
    private M unsafeCast(TradeMessage tradeMessage) {
        try {
            return (M) tradeMessage;
        } catch (Exception e) {
            throw new ClassCastException("Could not cast tradeMessage to generic TradeMessage type in " + getClass().getSimpleName() + ". " + e.getMessage());
        }
    }
}