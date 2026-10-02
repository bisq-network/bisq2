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

import bisq.api.web_socket.domain.ClosedTradeItemsService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.common.observable.Observable;
import bisq.support.SupportService;
import bisq.trade.TradeRestrictedException;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeRestApiTest {
    private static final String TRADE_ID = "trade-1";
    private static final String ACCOUNT_DATA = "Peter Tosh, 123456789";

    private final BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
    private final BisqEasyService bisqEasyService = mock(BisqEasyService.class);

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

    private void givenTrade(String paymentAccountData) {
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        when(trade.getPaymentAccountData()).thenReturn(new Observable<>(paymentAccountData));
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenReturn(Optional.of(trade));
    }

    private TradeRestApi restApi() {
        TradeService tradeService = mock(TradeService.class);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);
        return new TradeRestApi(mock(ChatService.class, RETURNS_DEEP_STUBS),
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
