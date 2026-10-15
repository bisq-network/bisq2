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

package bisq.api.dto.presentation.closed_trades;

import bisq.api.dto.DtoMappings;
import bisq.api.dto.common.monetary.PriceQuoteDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.common.market.MarketRepository;
import bisq.trade.TradeRole;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ClosedTradeListItemDtoFactoryTest {
    @Test
    void indexesCancelledTradeBeforePaymentDetailsWereExchanged() {
        BisqEasyTrade trade = trade();
        when(trade.getPaymentAccountData()).thenReturn(Optional.empty());
        when(trade.getBitcoinPaymentData()).thenReturn(Optional.empty());
        when(trade.getPaymentProof()).thenReturn(Optional.empty());

        ClosedTradeListItemDto dto = create(trade).dto();

        assertEquals(trade.getId(), dto.trade().id());
        assertTrue(dto.paymentAccountData().isEmpty());
        assertTrue(dto.bitcoinPaymentData().isEmpty());
        assertTrue(dto.paymentProof().isEmpty());
    }

    @Test
    void retainsPaymentDetailsWhenPresent() {
        BisqEasyTrade trade = trade();
        when(trade.getPaymentAccountData()).thenReturn(Optional.of("account"));
        when(trade.getBitcoinPaymentData()).thenReturn(Optional.of("bitcoin-address"));
        when(trade.getPaymentProof()).thenReturn(Optional.of("transaction-id"));

        ClosedTradeListItemDto dto = create(trade).dto();

        assertEquals(Optional.of("account"), dto.paymentAccountData());
        assertEquals(Optional.of("bitcoin-address"), dto.bitcoinPaymentData());
        assertEquals(Optional.of("transaction-id"), dto.paymentProof());
    }

    private static BisqEasyTrade trade() {
        BisqEasyTrade trade = mock(BisqEasyTrade.class, RETURNS_DEEP_STUBS);
        when(trade.getId()).thenReturn("closed-trade");
        when(trade.getTradeRole()).thenReturn(TradeRole.BUYER_AS_TAKER);
        when(trade.getTradeState()).thenReturn(BisqEasyTradeState.CANCELLED);
        when(trade.getOffer().getMarket()).thenReturn(MarketRepository.getUSDBitcoinMarket());
        return trade;
    }

    private static ClosedTradeIndexedItem create(BisqEasyTrade trade) {
        UserProfile profile = mock(UserProfile.class);
        when(profile.getId()).thenReturn("profile");
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore("profile")).thenReturn(new ReputationScore(0, 0, 0));
        UserProfileDto profileDto = new UserProfileDto(0, "profile", null, 0, null, "", "", "", "nym", "user", 0);
        PriceQuoteDto priceQuoteDto = new PriceQuoteDto(60_000_0000L, null, null, 0, 0, null);
        // Profile and price mapping and locale rendering have their own tests. Keep this test
        // focused on indexing incomplete trade history, without constructing cryptographic keys.
        try (MockedStatic<DtoMappings.UserProfileMapping> profiles = mockStatic(DtoMappings.UserProfileMapping.class,
                invocation -> profileDto);
             MockedStatic<DtoMappings.PriceQuoteMapping> prices = mockStatic(DtoMappings.PriceQuoteMapping.class,
                     invocation -> priceQuoteDto);
             MockedStatic<BisqEasyTradeFormatter> formatter = mockStatic(BisqEasyTradeFormatter.class,
                     invocation -> "formatted")) {
            return ClosedTradeListItemDtoFactory.create(new BisqEasyClosedTrade(trade, profile, profile), reputationService);
        }
    }
}
