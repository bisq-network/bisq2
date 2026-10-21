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

package bisq.api.rest_api.endpoints.trades;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.api.web_socket.domain.ClosedTradeItemsService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.market.MarketRepository;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.support.SupportService;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService;
import bisq.trade.TradeRestrictedException;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeRestApiTest {

    @Test
    void haltTradingErrorEntityKeepsLegacyErrorTextAndAddsErrorCode() {
        Map<String, String> entity = TradeRestApi.toTradeRestrictedErrorEntity(TradeRestrictedException.haltTrading());

        // Released mobile clients match on the "error" text; the prefix and fragments must not change
        assertThat(entity.get("error"))
                .startsWith("Invalid input: ")
                .contains("Trading is on halt");
        assertThat(entity.get("errorCode")).isEqualTo("HALT_TRADING");
        assertThat(entity).doesNotContainKey("minRequiredVersion");
    }

    @Test
    void minVersionErrorEntityCarriesVersionInTextAndField() {
        Map<String, String> entity =
                TradeRestApi.toTradeRestrictedErrorEntity(TradeRestrictedException.minVersionRequired("2.1.12"));

        assertThat(entity.get("error"))
                .startsWith("Invalid input: ")
                .contains("version 2.1.12 installed")
                .contains("min. version required for trading");
        assertThat(entity.get("errorCode")).isEqualTo("MIN_VERSION_REQUIRED");
        assertThat(entity.get("minRequiredVersion")).isEqualTo("2.1.12");
    }

    @Test
    void makerVersionErrorEntityCarriesVersionOnlyInField() {
        Map<String, String> entity =
                TradeRestApi.toTradeRestrictedErrorEntity(TradeRestrictedException.makerVersionBelowMinRequired("2.1.14"));

        // Released mobile clients read these fragments of the "error" text as a restriction of their own node
        assertThat(entity.get("error"))
                .startsWith("Invalid input: ")
                .doesNotContain("min. version required for trading")
                .doesNotContainPattern("version (\\S+) installed");
        assertThat(entity.get("errorCode")).isEqualTo("MAKER_VERSION_BELOW_MIN_REQUIRED");
        assertThat(entity.get("minRequiredVersion")).isEqualTo("2.1.14");
    }

    @Test
    @SuppressWarnings("unchecked")
    void processTradeEventReturnsStructured400AndSkipsTradeLogMessage() {
        BisqEasyOpenTradeChannelService openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        TradeRestApi restApi = newTradeRestApi(openTradeChannelService, bisqEasyTradeService);

        String tradeId = "trade-1";
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class, RETURNS_DEEP_STUBS);
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        BisqEasyContract contract = mock(BisqEasyContract.class);
        BitcoinPaymentMethodSpec paymentMethodSpec = mock(BitcoinPaymentMethodSpec.class);
        BitcoinPaymentMethod paymentMethod = mock(BitcoinPaymentMethod.class);
        when(openTradeChannelService.findChannelByTradeId(tradeId)).thenReturn(Optional.of(channel));
        when(bisqEasyTradeService.findOpenTrade(tradeId)).thenReturn(Optional.of(trade));
        when(trade.getContract()).thenReturn(contract);
        when(contract.getBaseSidePaymentMethodSpec()).thenReturn(paymentMethodSpec);
        when(paymentMethodSpec.getPaymentMethod()).thenReturn(paymentMethod);
        when(paymentMethod.getPaymentRail()).thenReturn(BitcoinPaymentRail.MAIN_CHAIN);

        // The guarded trade action throws because a security manager alert disallows trading.
        doThrow(TradeRestrictedException.minVersionRequired("2.1.12"))
                .when(bisqEasyTradeService).buyerConfirmFiatSent(trade);

        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        TradeEventDto event = new TradeEventDto(TradeEventTypeDto.BUYER_CONFIRM_FIAT_SENT, null);

        restApi.processTradeEvent(tradeId, event, asyncResponse);

        // The restriction surfaces as a structured 400 (not a generic 500) with the machine-readable code.
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(asyncResponse).resume(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        Map<String, String> entity = (Map<String, String>) captor.getValue().getEntity();
        assertThat(entity.get("errorCode")).isEqualTo("MIN_VERSION_REQUIRED");
        assertThat(entity.get("minRequiredVersion")).isEqualTo("2.1.12");

        // No trade-log message is sent because the action failed before any chat message is emitted.
        verify(openTradeChannelService, never()).sendTradeLogMessage(any(), any());
    }

    @Test
    void processTradeEventReturns400ForRefusedBitcoinPaymentDataAndSkipsTradeLogMessage() {
        BisqEasyOpenTradeChannelService openTradeChannelService = mock(BisqEasyOpenTradeChannelService.class);
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        TradeRestApi restApi = newTradeRestApi(openTradeChannelService, bisqEasyTradeService);

        String tradeId = "trade-1";
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class, RETURNS_DEEP_STUBS);
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        BisqEasyContract contract = mock(BisqEasyContract.class);
        BitcoinPaymentMethodSpec paymentMethodSpec = mock(BitcoinPaymentMethodSpec.class);
        BitcoinPaymentMethod paymentMethod = mock(BitcoinPaymentMethod.class);
        when(openTradeChannelService.findChannelByTradeId(tradeId)).thenReturn(Optional.of(channel));
        when(bisqEasyTradeService.findOpenTrade(tradeId)).thenReturn(Optional.of(trade));
        when(trade.getContract()).thenReturn(contract);
        when(contract.getBaseSidePaymentMethodSpec()).thenReturn(paymentMethodSpec);
        when(paymentMethodSpec.getPaymentMethod()).thenReturn(paymentMethod);
        when(paymentMethod.getPaymentRail()).thenReturn(BitcoinPaymentRail.MAIN_CHAIN);

        // The buyer's node refuses data which requests another amount than the trade amount.
        String bitcoinPaymentData = "bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4?amount=0.01";
        String refusal = "Bitcoin payment data must not request another amount than the trade amount";
        doThrow(new IllegalArgumentException(refusal))
                .when(bisqEasyTradeService).buyerSendBitcoinPaymentData(trade, bitcoinPaymentData);

        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        TradeEventDto event = new TradeEventDto(TradeEventTypeDto.BUYER_SEND_BITCOIN_PAYMENT_DATA, bitcoinPaymentData);

        restApi.processTradeEvent(tradeId, event, asyncResponse);

        // The refusal surfaces as invalid input (not a generic 500), as at taking an offer.
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(asyncResponse).resume(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(captor.getValue().getEntity()).isEqualTo("Invalid input: " + refusal);

        verify(openTradeChannelService, never()).sendTradeLogMessage(any(), any());
    }

    @Test
    void takeOfferReturns400WhenTheSellersReputationDoesNotCoverTheAmount() {
        BisqEasyOfferbookChannelService offerbookChannelService = mock(BisqEasyOfferbookChannelService.class);
        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        TradeRestApi restApi = newTradeRestApi(mock(BisqEasyOpenTradeChannelService.class),
                bisqEasyTradeService,
                offerbookChannelService,
                userIdentityService,
                marketPriceService);

        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-1");
        when(offer.getMarket()).thenReturn(MarketRepository.getUSDBitcoinMarket());
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        BisqEasyOfferbookChannel channel = mock(BisqEasyOfferbookChannel.class);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(List.of(message)));
        when(offerbookChannelService.getChannels()).thenReturn(new ObservableSet<>(List.of(channel)));
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(mock(UserIdentity.class, RETURNS_DEEP_STUBS));
        MarketPrice marketPrice = mock(MarketPrice.class, RETURNS_DEEP_STUBS);
        when(marketPrice.getPriceQuote().getValue()).thenReturn(100_000_0000L);
        when(marketPriceService.findMarketPrice(offer.getMarket())).thenReturn(Optional.of(marketPrice));

        // The buyer's node refuses the take because the seller's reputation does not cover the amount
        String refusal = "The seller's reputation score of 0 is too low for the trade amount.";
        doThrow(new TradeProtocolException(refusal, TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW))
                .when(bisqEasyTradeService).takerCreatesProtocol(any(), any(), any(), any(), any(), any(), any(), anyLong());

        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        TakeOfferRequest request = new TakeOfferRequest("offer-1", 100_000, 1_000_0000, "MAIN_CHAIN", "SEPA");

        restApi.takeOffer(request, asyncResponse);

        // The refusal surfaces as invalid input (not a generic 500)
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(asyncResponse).resume(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(captor.getValue().getEntity()).isEqualTo("Invalid input: " + refusal);
        verify(bisqEasyTradeService, never()).takeOffer(any());
    }

    private TradeRestApi newTradeRestApi(BisqEasyOpenTradeChannelService openTradeChannelService,
                                         BisqEasyTradeService bisqEasyTradeService) {
        return newTradeRestApi(openTradeChannelService,
                bisqEasyTradeService,
                mock(BisqEasyOfferbookChannelService.class),
                mock(UserIdentityService.class),
                mock(MarketPriceService.class));
    }

    private TradeRestApi newTradeRestApi(BisqEasyOpenTradeChannelService openTradeChannelService,
                                         BisqEasyTradeService bisqEasyTradeService,
                                         BisqEasyOfferbookChannelService offerbookChannelService,
                                         UserIdentityService userIdentityService,
                                         MarketPriceService marketPriceService) {
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOfferbookChannelService()).thenReturn(offerbookChannelService);
        when(chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK))
                .thenReturn(mock(ChatChannelSelectionService.class));
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);
        when(chatService.getLeavePrivateChatManager()).thenReturn(mock(LeavePrivateChatManager.class));

        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));

        SupportService supportService = mock(SupportService.class);
        when(supportService.getBisqEasyMediationRequestService()).thenReturn(mock(BisqEasyMediationRequestService.class));

        TradeService tradeService = mock(TradeService.class);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);

        return new TradeRestApi(chatService,
                marketPriceService,
                userService,
                supportService,
                tradeService,
                mock(ClosedTradeItemsService.class));
    }

}
