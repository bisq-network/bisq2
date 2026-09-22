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

package bisq.trade.bisq_easy;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasyTradeAmountLimitsTest {
    private static final Market EUR_MARKET = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Market USD_MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final PriceQuote EUR_QUOTE = PriceQuote.fromFiatPrice(50_000, "EUR");
    private static final PriceQuote USD_QUOTE = PriceQuote.fromFiatPrice(60_000, "USD");

    @Test
    void requiredScoreIs200PerUsd() {
        assertEquals(120_000, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(Fiat.fromFaceValue(600, "USD")));
        assertEquals(1_200, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(Fiat.fromFaceValue(6, "USD")));
    }

    @Test
    void requiredScoreUsesRoundedUsdAmount() {
        assertEquals(20_000, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(Fiat.fromFaceValue(100.49, "USD")));
        assertEquals(20_200, BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(Fiat.fromFaceValue(100.5, "USD")));
    }

    @Test
    void minSellOfferScoreCoversMinUsdAmount() {
        assertEquals(BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER,
                BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT));
    }

    @Test
    void usdAmountFromScoreIsRoundedDown() {
        assertEquals(Fiat.fromFaceValue(10, "USD"), BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(2_000));
        assertEquals(Fiat.fromFaceValue(9, "USD"), BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(1_999));
    }

    @Test
    void usdAmountFromScoreNeverRequiresMoreThanTheScore() {
        for (long score = 0; score <= 2_000; score++) {
            long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(
                    BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(score));
            assertTrue(requiredScore <= score, "score=" + score + " requiredScore=" + requiredScore);
        }
    }

    @Test
    void toleranceIsFivePercent() {
        assertEquals(20_000, BisqEasyTradeAmountLimits.withTolerance(19_048));
        assertEquals(19_999, BisqEasyTradeAmountLimits.withTolerance(19_047));
    }

    @Test
    void requiredScoreFromQuotesUsesUsdValue() {
        // 500 EUR = 0.01 BTC = 600 USD
        assertEquals(120_000, BisqEasyTradeAmountLimits.getRequiredReputationScore(EUR_QUOTE, USD_QUOTE, Fiat.fromFaceValue(500, "EUR")));
    }

    @Test
    void requiredScoreForFiatAmountUsesUsdValue() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(EUR_MARKET)).thenReturn(Optional.of(EUR_QUOTE));
        when(marketPriceService.findMarketPriceQuote(USD_MARKET)).thenReturn(Optional.of(USD_QUOTE));

        Optional<Long> requiredScore = BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService,
                EUR_MARKET,
                Fiat.fromFaceValue(500, "EUR"));

        assertEquals(Optional.of(120_000L), requiredScore);
    }

    @Test
    void requiredScoreForFiatAmountIsEmptyWithoutMarketPrice() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(EUR_MARKET)).thenReturn(Optional.empty());
        when(marketPriceService.findMarketPriceQuote(USD_MARKET)).thenReturn(Optional.of(USD_QUOTE));

        assertTrue(BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService,
                EUR_MARKET,
                Fiat.fromFaceValue(500, "EUR")).isEmpty());
    }

    @Test
    void requiredScoreForFiatAmountIsEmptyWithoutUsdPrice() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(EUR_MARKET)).thenReturn(Optional.of(EUR_QUOTE));
        when(marketPriceService.findMarketPriceQuote(USD_MARKET)).thenReturn(Optional.empty());

        assertTrue(BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount(marketPriceService,
                EUR_MARKET,
                Fiat.fromFaceValue(500, "EUR")).isEmpty());
    }
}
