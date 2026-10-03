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

import bisq.common.fsm.EventHandler;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.protocol.messages.TradeMessage;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class TradeMessageHandler<T extends Trade<?, ?, ?>, M extends TradeMessage> implements EventHandler<M> {
    protected final ServiceProvider serviceProvider;
    protected final T trade;

    protected TradeMessageHandler(ServiceProvider serviceProvider, T trade) {
        this.serviceProvider = serviceProvider;
        this.trade = trade;
    }

    public void handle(M message) {
        verifyInternal(message);
        verify(message);
        process(message);
        commit();
    }

    protected abstract void verify(M message);

    protected abstract void process(M message);

    protected abstract void commit();

    protected void verifyInternal(M message) {
        checkArgument(hasTradeId(message, trade),
                "TradeId of message not matching the tradeId from the trade data");
        checkArgument(isSentByTradePeer(message, trade),
                "Message sender networkID not matching the peers networkId from the trade data");
        // We verify if the sender of the message is banned at the message handler in the service class.
        // As the message handler is optional we prefer to block banned messages at the level instead of handling it here.
    }

    /**
     * Whether the message carries the ID of the trade and was sent by the trade peer. These are the two conditions
     * which {@link #verifyInternal} requires.
     */
    public static boolean isMessageOfTradePeer(TradeMessage message, Trade<?, ?, ?> trade) {
        return hasTradeId(message, trade) && isSentByTradePeer(message, trade);
    }

    private static boolean hasTradeId(TradeMessage message, Trade<?, ?, ?> trade) {
        return message.getTradeId().equals(trade.getId());
    }

    // A party is identified by its key. The confidential message layer has checked that the sender signed the message
    // with the key of the sender's network ID. The addresses of a network ID are routing data, which change when the
    // peer adds a transport, so the network ID which we stored for the peer can have other addresses.
    private static boolean isSentByTradePeer(TradeMessage message, Trade<?, ?, ?> trade) {
        return message.getSender().getId().equals(trade.getPeer().getNetworkId().getId());
    }
}