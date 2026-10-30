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

import bisq.common.util.StringUtils;
import bisq.contract.Role;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.events.BisqEasyCancelTradeEvent;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyCancelTradeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyOffer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsTakerTrade;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.CANCELLED;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.PEER_CANCELLED;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * The own cancel of a trader moves its trade to CANCELLED, and the cancel of its peer moves the trade to
 * PEER_CANCELLED, as for a reject. The state names the same trader as the role which interrupted the trade
 * (docs/specifications/trade/bisq-easy/interrupted-trade-states.md).
 */
class BisqEasyProtocolCancelTest {
    private ServiceProvider serviceProvider;
    private Identity makerIdentity;
    private Identity takerIdentity;
    private BisqEasyContract sellOfferContract;
    private BisqEasyContract buyOfferContract;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        makerIdentity = identity("maker");
        takerIdentity = identity("taker");
        sellOfferContract = contract(offer("sell-offer", makerIdentity.getNetworkId(), QUOTE_AMOUNT),
                takerIdentity.getNetworkId(),
                Optional.empty());
        buyOfferContract = contract(buyOffer("buy-offer", makerIdentity.getNetworkId(), QUOTE_AMOUNT),
                takerIdentity.getNetworkId(),
                Optional.empty());
    }

    @Test
    void buyerAsTakerMovesToCancelledAtItsOwnCancel() {
        BisqEasyTrade trade = inState(buyerAsTakerTrade(sellOfferContract, takerIdentity),
                BUYER_SENT_FIAT_SENT_CONFIRMATION);

        new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade).handle(new BisqEasyCancelTradeEvent());

        assertInterrupted(trade, CANCELLED, Role.TAKER);
    }

    @Test
    void buyerAsTakerMovesToPeerCancelledAtTheCancelOfTheSeller() {
        BisqEasyTrade trade = inState(buyerAsTakerTrade(sellOfferContract, takerIdentity),
                BUYER_SENT_FIAT_SENT_CONFIRMATION);

        new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade).handle(cancelOfPeer(trade));

        assertInterrupted(trade, PEER_CANCELLED, Role.MAKER);
    }

    @Test
    void sellerAsMakerMovesToCancelledAtItsOwnCancel() {
        BisqEasyTrade trade = inState(sellerAsMakerTrade(sellOfferContract, makerIdentity),
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        new BisqEasySellerAsMakerProtocol(serviceProvider, trade).handle(new BisqEasyCancelTradeEvent());

        assertInterrupted(trade, CANCELLED, Role.MAKER);
    }

    @Test
    void sellerAsMakerMovesToPeerCancelledAtTheCancelOfTheBuyer() {
        BisqEasyTrade trade = inState(sellerAsMakerTrade(sellOfferContract, makerIdentity),
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        new BisqEasySellerAsMakerProtocol(serviceProvider, trade).handle(cancelOfPeer(trade));

        assertInterrupted(trade, PEER_CANCELLED, Role.TAKER);
    }

    @Test
    void buyerAsMakerMovesToCancelledAtItsOwnCancel() {
        BisqEasyTrade trade = inState(buyerAsMakerTrade(buyOfferContract, makerIdentity),
                BUYER_SENT_FIAT_SENT_CONFIRMATION);

        new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade).handle(new BisqEasyCancelTradeEvent());

        assertInterrupted(trade, CANCELLED, Role.MAKER);
    }

    @Test
    void buyerAsMakerMovesToPeerCancelledAtTheCancelOfTheSeller() {
        BisqEasyTrade trade = inState(buyerAsMakerTrade(buyOfferContract, makerIdentity),
                BUYER_SENT_FIAT_SENT_CONFIRMATION);

        new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade).handle(cancelOfPeer(trade));

        assertInterrupted(trade, PEER_CANCELLED, Role.TAKER);
    }

    @Test
    void sellerAsTakerMovesToCancelledAtItsOwnCancel() {
        BisqEasyTrade trade = inState(sellerAsTakerTrade(buyOfferContract, takerIdentity),
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        new BisqEasySellerAsTakerProtocol(serviceProvider, trade).handle(new BisqEasyCancelTradeEvent());

        assertInterrupted(trade, CANCELLED, Role.TAKER);
    }

    @Test
    void sellerAsTakerMovesToPeerCancelledAtTheCancelOfTheBuyer() {
        BisqEasyTrade trade = inState(sellerAsTakerTrade(buyOfferContract, takerIdentity),
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION);

        new BisqEasySellerAsTakerProtocol(serviceProvider, trade).handle(cancelOfPeer(trade));

        assertInterrupted(trade, PEER_CANCELLED, Role.MAKER);
    }

    private static void assertInterrupted(BisqEasyTrade trade, BisqEasyTradeState state, Role initiator) {
        assertEquals(state, trade.getState());
        assertEquals(initiator, trade.getInterruptTradeInitiator().get());
    }

    private static BisqEasyCancelTradeMessage cancelOfPeer(BisqEasyTrade trade) {
        return new BisqEasyCancelTradeMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId());
    }
}
