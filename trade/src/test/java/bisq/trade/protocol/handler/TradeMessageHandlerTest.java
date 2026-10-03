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

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.ServiceProvider;
import bisq.trade.Trade;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The trade services drop a message for which isMessageOfTradePeer is false, and the handlers throw for it in
 * verifyInternal. Both must decide in the same way: a message which passes the services must not fail the trade.
 */
class TradeMessageHandlerTest {
    private static final String TRADE_ID = "5a6c1a0c-30e0-3c33-a8a5-d1d0e2a3a7a1";

    private final NetworkId peer = networkId("peer", 1002);
    private final NetworkId thirdParty = networkId("third-party", 1003);
    private final Trade<?, ?, ?> trade = trade();

    @Test
    void messageOfTheTradePeerWithTheTradeIdIsAccepted() {
        TradeMessage message = message(TRADE_ID, peer);

        assertThat(TradeMessageHandler.isMessageOfTradePeer(message, trade)).isTrue();
        assertThatCode(() -> new Handler(trade).verifyInternal(message)).doesNotThrowAnyException();
    }

    @Test
    void messageOfAThirdPartyWithTheTradeIdIsRejected() {
        TradeMessage message = message(TRADE_ID, thirdParty);

        assertThat(TradeMessageHandler.isMessageOfTradePeer(message, trade)).isFalse();
        assertThatThrownBy(() -> new Handler(trade).verifyInternal(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void messageOfTheTradePeerWithAnotherTradeIdIsRejected() {
        TradeMessage message = message("another-trade-id", peer);

        assertThat(TradeMessageHandler.isMessageOfTradePeer(message, trade)).isFalse();
        assertThatThrownBy(() -> new Handler(trade).verifyInternal(message))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Trade<?, ?, ?> trade() {
        Trade<?, ?, ?> trade = mock(Trade.class, RETURNS_DEEP_STUBS);
        when(trade.getId()).thenReturn(TRADE_ID);
        when(trade.getPeer().getNetworkId()).thenReturn(peer);
        return trade;
    }

    private static TradeMessage message(String tradeId, NetworkId sender) {
        TradeMessage message = mock(TradeMessage.class);
        when(message.getTradeId()).thenReturn(tradeId);
        when(message.getSender()).thenReturn(sender);
        return message;
    }

    private static NetworkId networkId(String keyId, int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }

    private static final class Handler extends TradeMessageHandler<Trade<?, ?, ?>, TradeMessage> {
        private Handler(Trade<?, ?, ?> trade) {
            super(mock(ServiceProvider.class), trade);
        }

        @Override
        protected void verify(TradeMessage message) {
        }

        @Override
        protected void process(TradeMessage message) {
        }

        @Override
        protected void commit() {
        }
    }
}
