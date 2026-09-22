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

package bisq.trade.bisq_easy.protocol;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.events.BisqEasyChatChannelMismatchEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatSentEvent;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.networkId;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.FAILED;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A trade may only progress while its chat channel exists and matches the contract. A mismatch fails the trade
 * and a failed trade ignores later events.
 */
class BisqEasyProtocolChatChannelBindingTest {
    private BisqEasyOpenTradeChannelService channelService;
    private NetworkId makerNetworkId;
    private BisqEasyOffer offer;
    private BisqEasyTrade trade;
    private BisqEasyProtocol protocol;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);

        makerNetworkId = networkId("maker");
        offer = offer("offer-id", makerNetworkId, QUOTE_AMOUNT);
        Identity takerIdentity = identity("taker");
        trade = inState(buyerAsTakerTrade(contract(offer, takerIdentity.getNetworkId(), Optional.empty()), takerIdentity),
                TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA);
        protocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade);
    }

    private void channelHolds(BisqEasyOffer channelOffer, NetworkId peer) {
        BisqEasyOpenTradeChannel channel = channel(trade, channelOffer, userProfile(peer), Optional.empty());
        when(channelService.findChannelByTradeId(anyString())).thenReturn(Optional.of(channel));
    }

    @Test
    void eventProceedsWithMatchingChannel() {
        channelHolds(offer, makerNetworkId);

        protocol.handle(new BisqEasyConfirmFiatSentEvent());

        assertEquals(BUYER_SENT_FIAT_SENT_CONFIRMATION, trade.getState());
    }

    @Test
    void eventFailsTradeWithoutChannel() {
        when(channelService.findChannelByTradeId(anyString())).thenReturn(Optional.empty());

        protocol.handle(new BisqEasyConfirmFiatSentEvent());

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
    }

    @Test
    void eventFailsTradeWithMismatchingChannel() {
        channelHolds(offer("offer-id", makerNetworkId, QUOTE_AMOUNT * 2), makerNetworkId);

        protocol.handle(new BisqEasyConfirmFiatSentEvent());

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
    }

    @Test
    void mismatchEventFailsTradeFromAnyState() {
        protocol.handle(new BisqEasyChatChannelMismatchEvent("mismatch"));

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
        assertEquals("mismatch", trade.getErrorMessage());
    }

    @Test
    void failedTradeIgnoresLaterEvents() {
        protocol.handle(new BisqEasyChatChannelMismatchEvent("mismatch"));
        channelHolds(offer, makerNetworkId);

        protocol.handle(new BisqEasyConfirmFiatSentEvent());

        assertEquals(FAILED, trade.getState());
    }
}
