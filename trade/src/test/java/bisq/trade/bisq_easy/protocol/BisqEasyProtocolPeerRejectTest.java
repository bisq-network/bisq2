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
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyRejectTradeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.Set;

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
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.PEER_REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * A trader can reject a trade only before the exchange of the payment data, and it sends nothing for the trade after
 * its reject. The reject can cross with the messages and the actions of the node, so it can arrive in a later state.
 * The node applies it in every state in which the peer can still have sent it, and keeps it queued in the later
 * states (docs/specifications/trade/bisq-easy/peer-reject.md).
 */
class BisqEasyProtocolPeerRejectTest {
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

    // The seller as maker can no longer reject once it has sent its account data. Until then the buyer can have
    // received the take offer response and sent its Bitcoin address.
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "TAKER_SENT_TAKE_OFFER_REQUEST",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA_"
    })
    void buyerAsTakerAppliesRejectOfSeller(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(buyerAsTakerTrade(sellOfferContract, takerIdentity), state);

        new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade).handle(rejectOfPeer(trade));

        assertEquals(PEER_REJECTED, trade.getState());
    }

    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA",
            "BUYER_SENT_FIAT_SENT_CONFIRMATION",
            "BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION",
            "BUYER_RECEIVED_BTC_SENT_CONFIRMATION"
    })
    void buyerAsTakerKeepsRejectOfSellerQueuedAfterItReceivedTheAccountData(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(buyerAsTakerTrade(sellOfferContract, takerIdentity), state);
        BisqEasyRejectTradeMessage reject = rejectOfPeer(trade);

        new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade).handle(reject);

        assertEquals(state, trade.getState());
        assertEquals(Set.of(reject), trade.getEventQueue());
    }

    // The buyer as taker can send its Bitcoin address before it receives the take offer response, and reject after
    // that. Until the reject arrives, the seller can have received the address and sent its account data.
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS_",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS"
    })
    void sellerAsMakerAppliesRejectOfBuyer(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(sellerAsMakerTrade(sellOfferContract, makerIdentity), state);

        new BisqEasySellerAsMakerProtocol(serviceProvider, trade).handle(rejectOfPeer(trade));

        assertEquals(PEER_REJECTED, trade.getState());
    }

    // The buyer can no longer reject once it has confirmed the fiat payment
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "SELLER_RECEIVED_FIAT_SENT_CONFIRMATION",
            "SELLER_CONFIRMED_FIAT_RECEIPT",
            "SELLER_SENT_BTC_SENT_CONFIRMATION"
    })
    void sellerAsMakerKeepsRejectOfBuyerQueuedAfterTheFiatPayment(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(sellerAsMakerTrade(sellOfferContract, makerIdentity), state);
        BisqEasyRejectTradeMessage reject = rejectOfPeer(trade);

        new BisqEasySellerAsMakerProtocol(serviceProvider, trade).handle(reject);

        assertEquals(state, trade.getState());
        assertEquals(Set.of(reject), trade.getEventQueue());
    }

    // The seller as taker can send its account data before it receives the take offer response, and reject after
    // that. Until the reject arrives, the buyer can have received the account data and sent its Bitcoin address.
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA",
            "MAKER_DID_NOT_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA_",
            "MAKER_SENT_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA"
    })
    void buyerAsMakerAppliesRejectOfSeller(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(buyerAsMakerTrade(buyOfferContract, makerIdentity), state);

        new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade).handle(rejectOfPeer(trade));

        assertEquals(PEER_REJECTED, trade.getState());
    }

    // The buyer can confirm the fiat payment before a reject of the seller arrives. The desktop offers the buyer
    // mediation for the open trade, to get the payment back, but not for a rejected trade. The seller can no longer
    // reject once it has confirmed the receipt of the payment.
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "BUYER_SENT_FIAT_SENT_CONFIRMATION",
            "BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION",
            "BUYER_RECEIVED_BTC_SENT_CONFIRMATION"
    })
    void buyerAsMakerKeepsRejectOfSellerQueuedAfterTheFiatPayment(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(buyerAsMakerTrade(buyOfferContract, makerIdentity), state);
        BisqEasyRejectTradeMessage reject = rejectOfPeer(trade);

        new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade).handle(reject);

        assertEquals(state, trade.getState());
        assertEquals(Set.of(reject), trade.getEventQueue());
    }

    // The buyer as maker can no longer reject once it has sent its Bitcoin address. Until then the seller can have
    // received the take offer response and sent its account data.
    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "TAKER_SENT_TAKE_OFFER_REQUEST",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "TAKER_DID_NOT_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS_"
    })
    void sellerAsTakerAppliesRejectOfBuyer(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(sellerAsTakerTrade(buyOfferContract, takerIdentity), state);

        new BisqEasySellerAsTakerProtocol(serviceProvider, trade).handle(rejectOfPeer(trade));

        assertEquals(PEER_REJECTED, trade.getState());
    }

    @ParameterizedTest
    @EnumSource(value = BisqEasyTradeState.class, names = {
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS",
            "TAKER_RECEIVED_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_RECEIVED_BTC_ADDRESS",
            "SELLER_RECEIVED_FIAT_SENT_CONFIRMATION",
            "SELLER_CONFIRMED_FIAT_RECEIPT",
            "SELLER_SENT_BTC_SENT_CONFIRMATION"
    })
    void sellerAsTakerKeepsRejectOfBuyerQueuedAfterItReceivedTheBtcAddress(BisqEasyTradeState state) {
        BisqEasyTrade trade = inState(sellerAsTakerTrade(buyOfferContract, takerIdentity), state);
        BisqEasyRejectTradeMessage reject = rejectOfPeer(trade);

        new BisqEasySellerAsTakerProtocol(serviceProvider, trade).handle(reject);

        assertEquals(state, trade.getState());
        assertEquals(Set.of(reject), trade.getEventQueue());
    }

    private static BisqEasyRejectTradeMessage rejectOfPeer(BisqEasyTrade trade) {
        return new BisqEasyRejectTradeMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId());
    }
}
