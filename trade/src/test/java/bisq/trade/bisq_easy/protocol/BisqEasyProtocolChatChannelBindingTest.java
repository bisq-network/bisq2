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
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.events.BisqEasyAccountDataEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyChatChannelMismatchEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmBtcSentEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatReceiptEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyConfirmFiatSentEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasySendBtcAddressEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyTradeEvent;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.function.Supplier;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.channel;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.networkId;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.userProfile;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.BUYER_SENT_FIAT_SENT_CONFIRMATION;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.FAILED;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.SELLER_CONFIRMED_FIAT_RECEIPT;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.SELLER_RECEIVED_FIAT_SENT_CONFIRMATION;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.SELLER_SENT_BTC_SENT_CONFIRMATION;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A trade may only progress while its chat channel exists and matches the trade and its contract. A mismatch fails
 * the trade and a failed trade ignores later events.
 */
class BisqEasyProtocolChatChannelBindingTest {
    // The steps which move value or account data, each with the state it starts from and the state it leads to.
    private enum GatedStep {
        BUYER_SENDS_BTC_ADDRESS(false,
                TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_DID_NOT_RECEIVED_ACCOUNT_DATA,
                () -> new BisqEasySendBtcAddressEvent("bc1qbuyersaddress")),
        BUYER_CONFIRMS_FIAT_SENT(false,
                TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA,
                BUYER_SENT_FIAT_SENT_CONFIRMATION,
                BisqEasyConfirmFiatSentEvent::new),
        SELLER_SENDS_ACCOUNT_DATA(true,
                MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS,
                () -> new BisqEasyAccountDataEvent("sellers account data")),
        SELLER_CONFIRMS_FIAT_RECEIPT(true,
                SELLER_RECEIVED_FIAT_SENT_CONFIRMATION,
                SELLER_CONFIRMED_FIAT_RECEIPT,
                BisqEasyConfirmFiatReceiptEvent::new),
        SELLER_CONFIRMS_BTC_SENT(true,
                SELLER_CONFIRMED_FIAT_RECEIPT,
                SELLER_SENT_BTC_SENT_CONFIRMATION,
                () -> new BisqEasyConfirmBtcSentEvent(Optional.of("txId")));

        private final boolean isSellerAsMaker;
        private final BisqEasyTradeState fromState;
        private final BisqEasyTradeState toState;
        private final Supplier<BisqEasyTradeEvent> event;

        GatedStep(boolean isSellerAsMaker,
                  BisqEasyTradeState fromState,
                  BisqEasyTradeState toState,
                  Supplier<BisqEasyTradeEvent> event) {
            this.isSellerAsMaker = isSellerAsMaker;
            this.fromState = fromState;
            this.toState = toState;
            this.event = event;
        }
    }

    private ServiceProvider serviceProvider;
    private BisqEasyOpenTradeChannelService channelService;
    private Identity makerIdentity;
    private Identity takerIdentity;
    private UserProfile mediator;
    private BisqEasyContract contract;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        channelService = mock(BisqEasyOpenTradeChannelService.class);
        when(serviceProvider.getChatService().getBisqEasyOpenTradeChannelService()).thenReturn(channelService);

        makerIdentity = identity("maker");
        takerIdentity = identity("taker");
        mediator = userProfile(networkId("mediator"));
        BisqEasyOffer offer = offer("offer-id", makerIdentity.getNetworkId(), QUOTE_AMOUNT);
        contract = contract(offer, takerIdentity.getNetworkId(), Optional.of(mediator));
    }

    // The trade is read back from its protobuf form, so it carries a deserialized copy of the offer and mediator.
    private BisqEasyTrade tradeAt(GatedStep step) {
        BisqEasyTrade trade = step.isSellerAsMaker
                ? sellerAsMakerTrade(contract, makerIdentity)
                : buyerAsTakerTrade(contract, takerIdentity);
        return inState(trade, step.fromState);
    }

    private BisqEasyProtocol protocolFor(GatedStep step, BisqEasyTrade trade) {
        return step.isSellerAsMaker
                ? new BisqEasySellerAsMakerProtocol(serviceProvider, trade)
                : new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade);
    }

    private void channelHolds(BisqEasyTrade trade, UserProfile channelMediator) {
        BisqEasyOpenTradeChannel channel = channel(trade, contract.getOffer(), userProfile(trade.getPeer().getNetworkId()), Optional.of(channelMediator));
        when(channelService.findChannelByTradeId(anyString())).thenReturn(Optional.of(channel));
    }

    @ParameterizedTest
    @EnumSource(GatedStep.class)
    void gatedStepProceedsWithMatchingChannel(GatedStep step) {
        BisqEasyTrade trade = tradeAt(step);
        channelHolds(trade, mediator);

        protocolFor(step, trade).handle(step.event.get());

        assertEquals(step.toState, trade.getState());
    }

    @ParameterizedTest
    @EnumSource(GatedStep.class)
    void gatedStepFailsTradeWithoutChannel(GatedStep step) {
        BisqEasyTrade trade = tradeAt(step);
        when(channelService.findChannelByTradeId(anyString())).thenReturn(Optional.empty());

        protocolFor(step, trade).handle(step.event.get());

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
    }

    @ParameterizedTest
    @EnumSource(GatedStep.class)
    void gatedStepFailsTradeWithAnotherMediatorInChannel(GatedStep step) {
        BisqEasyTrade trade = tradeAt(step);
        channelHolds(trade, userProfile(networkId("other-mediator")));

        protocolFor(step, trade).handle(step.event.get());

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
    }

    @Test
    void mismatchEventFailsTradeFromAnyState() {
        BisqEasyTrade trade = tradeAt(GatedStep.BUYER_CONFIRMS_FIAT_SENT);

        protocolFor(GatedStep.BUYER_CONFIRMS_FIAT_SENT, trade).handle(new BisqEasyChatChannelMismatchEvent("mismatch"));

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.CHANNEL_DATA_MISMATCH, trade.getTradeProtocolFailure());
        assertEquals("mismatch", trade.getErrorMessage());
    }

    @Test
    void failedTradeIgnoresLaterEvents() {
        BisqEasyTrade trade = tradeAt(GatedStep.BUYER_CONFIRMS_FIAT_SENT);
        BisqEasyProtocol protocol = protocolFor(GatedStep.BUYER_CONFIRMS_FIAT_SENT, trade);
        protocol.handle(new BisqEasyChatChannelMismatchEvent("mismatch"));
        channelHolds(trade, mediator);

        protocol.handle(new BisqEasyConfirmFiatSentEvent());

        assertEquals(FAILED, trade.getState());
    }
}
