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
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.api.web_socket.domain.ClosedTradeItemsService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.observable.Observable;
import bisq.i18n.Res;
import bisq.support.SupportService;
import bisq.trade.TradeRestrictedException;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeRestApiTest {
    private static final String TRADE_ID = "trade-1";
    private static final String ACCOUNT_DATA = "Peter Tosh, 123456789";
    private static final String USER_NAME = "alice";

    private final ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
    private final BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
    private final BisqEasyService bisqEasyService = mock(BisqEasyService.class);
    private final BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class, RETURNS_DEEP_STUBS);
    private final BisqEasyTrade openTrade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);

    @Test
    void accountDataBannedReturnsTrueWhenTheSellersAccountDataIsBanned() {
        givenTrade(ACCOUNT_DATA);
        when(bisqEasyService.isAccountDataBanned(ACCOUNT_DATA)).thenReturn(true);

        Response response = restApi().isAccountDataBanned(TRADE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(new AccountDataBannedResponse(true));
        verify(bisqEasyTradeService, never()).cancelTrade(any());
    }

    @Test
    void accountDataBannedReturnsFalseWhenTheSellersAccountDataIsNotBanned() {
        givenTrade(ACCOUNT_DATA);
        when(bisqEasyService.isAccountDataBanned(ACCOUNT_DATA)).thenReturn(false);

        Response response = restApi().isAccountDataBanned(TRADE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(new AccountDataBannedResponse(false));
        verify(bisqEasyService).isAccountDataBanned(ACCOUNT_DATA);
    }

    @Test
    void accountDataBannedReturnsFalseWithoutCheckingWhenTheTradeHasNoAccountDataYet() {
        givenTrade(null);

        Response response = restApi().isAccountDataBanned(TRADE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(new AccountDataBannedResponse(false));
        verify(bisqEasyService, never()).isAccountDataBanned(any());
    }

    @Test
    void accountDataBannedReturnsInternalErrorWhenTheLookupFails() {
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenThrow(new RuntimeException("boom"));

        Response response = restApi().isAccountDataBanned(TRADE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        assertThat(response.getEntity()).isEqualTo(Map.of("error", "An unexpected error occurred"));
    }

    @Test
    void accountDataBannedReturnsNotFoundForAnUnknownTrade() {
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenReturn(Optional.empty());

        Response response = restApi().isAccountDataBanned(TRADE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        assertThat(response.getEntity()).isEqualTo("Trade not found for ID " + TRADE_ID);
    }

    @Test
    void cancelTradeEventSendsTheCancelledTradeLogMessage() {
        givenOpenTradeWithChannel();

        processTradeEvent(TradeEventTypeDto.CANCEL_TRADE);

        verify(openTradeChannelService()).sendTradeLogMessage(
                Res.encode("bisqEasy.openTrades.tradeLogMessage.cancelled", USER_NAME), channel);
    }

    @Test
    void cancelTradeEventCancelsTheTrade() {
        givenOpenTradeWithChannel();

        Response response = processTradeEvent(TradeEventTypeDto.CANCEL_TRADE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(bisqEasyTradeService).cancelTrade(openTrade);
        verify(bisqEasyTradeService, never()).rejectTrade(any());
    }

    @Test
    void cancelTradeEventCancelsTheTradeWhenTheTradeLogMessageFails() {
        givenOpenTradeWithChannel();
        givenTradeLogMessageFails();

        Response response = processTradeEvent(TradeEventTypeDto.CANCEL_TRADE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(bisqEasyTradeService).cancelTrade(openTrade);
    }

    @Test
    void rejectTradeEventSendsTheRejectedTradeLogMessage() {
        givenOpenTradeWithChannel();

        processTradeEvent(TradeEventTypeDto.REJECT_TRADE);

        verify(openTradeChannelService()).sendTradeLogMessage(
                Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", USER_NAME), channel);
    }

    @Test
    void rejectTradeEventRejectsTheTrade() {
        givenOpenTradeWithChannel();

        Response response = processTradeEvent(TradeEventTypeDto.REJECT_TRADE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(bisqEasyTradeService).rejectTrade(openTrade);
        verify(bisqEasyTradeService, never()).cancelTrade(any());
    }

    @Test
    void rejectTradeEventRejectsTheTradeWhenTheTradeLogMessageFails() {
        givenOpenTradeWithChannel();
        givenTradeLogMessageFails();

        Response response = processTradeEvent(TradeEventTypeDto.REJECT_TRADE);

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(bisqEasyTradeService).rejectTrade(openTrade);
    }

    private void givenOpenTradeWithChannel() {
        when(channel.getMyUserIdentity().getUserName()).thenReturn(USER_NAME);
        when(openTradeChannelService().findChannelByTradeId(TRADE_ID)).thenReturn(Optional.of(channel));
        // Deep stubs cannot provide the payment rail enum
        BitcoinPaymentMethod paymentMethod = mock(BitcoinPaymentMethod.class);
        doReturn(BitcoinPaymentRail.MAIN_CHAIN).when(paymentMethod).getPaymentRail();
        when(openTrade.getContract().getBaseSidePaymentMethodSpec().getPaymentMethod()).thenReturn(paymentMethod);
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenReturn(Optional.of(openTrade));
    }

    private void givenTradeLogMessageFails() {
        when(openTradeChannelService().sendTradeLogMessage(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("PEER_BANNED")));
    }

    private Response processTradeEvent(TradeEventTypeDto tradeEventType) {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        restApi().processTradeEvent(TRADE_ID, new TradeEventDto(tradeEventType, null), asyncResponse);
        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(asyncResponse).resume(response.capture());
        return response.getValue();
    }

    private BisqEasyOpenTradeChannelService openTradeChannelService() {
        return chatService.getBisqEasyOpenTradeChannelService();
    }

    private void givenTrade(String paymentAccountData) {
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        when(trade.getPaymentAccountData()).thenReturn(new Observable<>(paymentAccountData));
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenReturn(Optional.of(trade));
    }

    private TradeRestApi restApi() {
        TradeService tradeService = mock(TradeService.class);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);
        return new TradeRestApi(chatService,
                mock(MarketPriceService.class),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(SupportService.class, RETURNS_DEEP_STUBS),
                tradeService,
                mock(ClosedTradeItemsService.class),
                bisqEasyService);
    }

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
}
