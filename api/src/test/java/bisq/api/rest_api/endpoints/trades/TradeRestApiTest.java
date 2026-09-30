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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.support.SupportService;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService;
import bisq.trade.TradeRestrictedException;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
        when(bisqEasyTradeService.findTrade(tradeId)).thenReturn(Optional.of(trade));
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

    private TradeRestApi newTradeRestApi(BisqEasyOpenTradeChannelService openTradeChannelService,
                                         BisqEasyTradeService bisqEasyTradeService) {
        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOfferbookChannelService()).thenReturn(mock(BisqEasyOfferbookChannelService.class));
        when(chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK))
                .thenReturn(mock(ChatChannelSelectionService.class));
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(openTradeChannelService);
        when(chatService.getLeavePrivateChatManager()).thenReturn(mock(LeavePrivateChatManager.class));

        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(mock(UserIdentityService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));

        SupportService supportService = mock(SupportService.class);
        when(supportService.getBisqEasyMediationRequestService()).thenReturn(mock(BisqEasyMediationRequestService.class));

        TradeService tradeService = mock(TradeService.class);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);

        return new TradeRestApi(chatService,
                mock(MarketPriceService.class),
                userService,
                supportService,
                tradeService,
                mock(ClosedTradeItemsService.class));
    }

}
