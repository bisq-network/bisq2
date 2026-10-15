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
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.MathUtils;
import com.google.common.math.LongMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static bisq.trade.bisq_easy.validation.BisqEasySellerReputationValidator.validateSellersReputation;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    void toleranceIsFivePercentForHighScores() {
        assertEquals(20_000, BisqEasyTradeAmountLimits.withTolerance(19_048));
        assertEquals(19_999, BisqEasyTradeAmountLimits.withTolerance(19_047));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1200, 1400",
            "1300, 1500",
            "2400, 2600",
            "3999, 4199",
            "4000, 4200",
            "4010, 4211"
    })
    void toleranceCoversOneUsdForPositiveLowScores(long score, long toleratedScore) {
        assertEquals(toleratedScore, BisqEasyTradeAmountLimits.withTolerance(score));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, true",
            "0, 1, false",
            "1300, 1500, true",
            "1300, 1501, false",
            "100000, 105000, true",
            "100000, 105001, false"
    })
    void requiredScoreIsCoveredUpToTheScoreWithTolerance(long score, long requiredScore, boolean isCovered) {
        assertEquals(isCovered, BisqEasyTradeAmountLimits.isCoveredWithTolerance(score, requiredScore));
    }

    @ParameterizedTest
    @CsvSource({"60000", "83875.1546"})
    void acceptsLegacyUsdMaximumForLowScores(double btcUsdPrice) {
        PriceQuote usdPriceQuote = PriceQuote.fromFiatPrice(btcUsdPrice, "USD");
        for (long score = BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER; score <= 4_000; score++) {
            // v2.1.13 rounds score / 200 half up, converts through BTC, then rounds to whole quote units.
            Monetary legacyUsdMaximum = Fiat.fromFaceValue(MathUtils.roundDoubleToLong(score / 200d), "USD");
            Monetary btcAmount = usdPriceQuote.toBaseSideMonetary(legacyUsdMaximum);
            Monetary amount = usdPriceQuote.toQuoteSideMonetary(btcAmount).round(0);
            long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(usdPriceQuote, usdPriceQuote, amount);
            long sellersScore = score;

            assertDoesNotThrow(() -> validateSellersReputation(sellersScore, requiredScore),
                    "score=" + score + " amount=" + amount.getValue() + " requiredScore=" + requiredScore);
        }
    }

    @Test
    void coversLegacyMaximumOfSellerMakerWhichNeedsMoreThanTheScore() {
        // v2.1.13 rounds 79902 / 200 = 399.51 half up to 400 USD, which require 80000. This version derives 399 USD.
        long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(USD_QUOTE, USD_QUOTE,
                Fiat.fromFaceValue(400, "USD"));

        assertEquals(80_000, requiredScore);
        assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(79_902, requiredScore));
        assertEquals(Fiat.fromFaceValue(399, "USD"),
                BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(USD_QUOTE, USD_QUOTE, 79_902));
    }

    // A buyer applies the tolerance when taking a sell offer, so every maximum of a v2.1.13 seller maker in a USD market
    // can be taken. Up to 600 USD that maximum requires at most 100 points more than the score.
    @ParameterizedTest
    @CsvSource({"60000", "83875.1546"})
    void coversLegacyUsdMaximumOfSellerMakers(double btcUsdPrice) {
        PriceQuote usdPriceQuote = PriceQuote.fromFiatPrice(btcUsdPrice, "USD");
        long scoreForMaxUsdTradeAmount = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(
                BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT);
        for (long score = BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER;
             score <= scoreForMaxUsdTradeAmount;
             score++) {
            // v2.1.13 rounds score / 200 half up, limits it to 600 USD, converts through BTC, then rounds to whole
            // quote units.
            long legacyUsdMaximum = Math.min(600, MathUtils.roundDoubleToLong(score / 200d));
            Monetary btcAmount = usdPriceQuote.toBaseSideMonetary(Fiat.fromFaceValue(legacyUsdMaximum, "USD"));
            Monetary amount = usdPriceQuote.toQuoteSideMonetary(btcAmount).round(0);
            long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(usdPriceQuote, usdPriceQuote, amount);
            String context = "score=" + score + " amount=" + amount.getValue() + " requiredScore=" + requiredScore;

            assertTrue(requiredScore <= score + 100, context);
            assertTrue(BisqEasyTradeAmountLimits.isCoveredWithTolerance(score, requiredScore), context);
        }
    }

    @ParameterizedTest
    @CsvSource({
            // The seller's maximum limits the offer
            "50, 400, 399, 399",
            // The offer's maximum is below the seller's maximum
            "50, 300, 399, 300",
            // The seller's maximum is below the offer's minimum, which the tolerance covers. Score 79902: the seller's
            // maximum is 399 USD, the minimum of 399.6 USD requires 80000.
            "399.6, 400, 399, 399.6"
    })
    void buyersMaxAmountIsTheSellersMaximumButNotBelowTheOffersMinimum(double offersMin,
                                                                       double offersMax,
                                                                       double sellersMax,
                                                                       double buyersMax) {
        Monetary amount = BisqEasyTradeAmountLimits.getBuyersMaxQuoteSideAmount(Fiat.fromFaceValue(offersMin, "USD"),
                Fiat.fromFaceValue(offersMax, "USD"),
                Fiat.fromFaceValue(sellersMax, "USD"));

        assertEquals(Fiat.fromFaceValue(buyersMax, "USD"), amount);
    }

    @Test
    void acceptsLowScoreMaximumAcrossUsdRoundingBoundaryWithSlightPriceDifference() {
        PriceQuote usdPriceQuote = PriceQuote.fromFiatPrice(83_875.15, "USD");
        Monetary amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(
                PriceQuote.fromFiatPrice(73_808.56, "EUR"), usdPriceQuote, 2_400);
        assertEquals(Fiat.fromFaceValue(11, "EUR"), amount);

        long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(
                PriceQuote.fromFiatPrice(73_771.66, "EUR"), usdPriceQuote, amount);
        assertEquals(2_600, requiredScore);
        assertDoesNotThrow(() -> validateSellersReputation(2_400, requiredScore));
    }

    @Test
    void requiredScoreFromQuotesUsesUsdValue() {
        // 500 EUR = 0.01 BTC = 600 USD
        assertEquals(120_000, BisqEasyTradeAmountLimits.getRequiredReputationScore(EUR_QUOTE, USD_QUOTE, Fiat.fromFaceValue(500, "EUR")));
    }

    @ParameterizedTest
    @CsvSource({
            "EUR, 55200, 60000",
            "EUR, 50000, 60000",
            "GBP, 47200, 60000",
            "KWD, 18400, 60000",
            "CHF, 53000, 60000",
            "BRL, 434533.5, 83875.1546",
            "JPY, 13333584.5, 83875.1546",
            "USD, 60000, 60000",
            "USD, 83875.1546, 83875.1546"
    })
    void reputationBasedAmountIsInWholeUnitsAndAcceptedWithoutTolerance(String currencyCode,
                                                                        double btcPrice,
                                                                        double btcUsdPrice) {
        PriceQuote fiatPriceQuote = PriceQuote.fromFiatPrice(btcPrice, currencyCode);
        PriceQuote usdPriceQuote = PriceQuote.fromFiatPrice(btcUsdPrice, "USD");
        for (long score = 0; score <= 20_000; score++) {
            Monetary amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(fiatPriceQuote, usdPriceQuote, score);
            long requiredScore = BisqEasyTradeAmountLimits.getRequiredReputationScore(fiatPriceQuote, usdPriceQuote, amount);
            String context = currencyCode + " score=" + score + " amount=" + amount.getValue() + " requiredScore=" + requiredScore;

            assertEquals(0, amount.getValue() % LongMath.pow(10, amount.getPrecision()), context);
            assertTrue(requiredScore <= score, context);
            long sellersReputationScore = score;
            assertDoesNotThrow(() -> validateSellersReputation(sellersReputationScore, requiredScore), context);
            assertTrue(amount.getValue() <= convertedUsdAllowanceRoundedToWholeUnits(fiatPriceQuote, usdPriceQuote, score).getValue(), context);
        }
    }

    @Test
    void reputationBasedAmountIsLoweredWhenRoundingUpNeedsMoreThanTheScore() {
        // 6 USD are 5.52 EUR. Rounded to 6 EUR they are 6.52 USD, which the rule rounds to 7 USD, requiring 1400.
        Monetary amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(PriceQuote.fromFiatPrice(55_200, "EUR"),
                PriceQuote.fromFiatPrice(60_000, "USD"),
                1_200);

        assertEquals(Fiat.fromFaceValue(5, "EUR"), amount);
    }

    @Test
    void reputationBasedAmountKeepsWholeUnitReducedByConversionThroughBtc() {
        // 6 USD converted to BTC and back are 5.9995 USD, which the rule rounds to 6 USD, requiring 1200.
        PriceQuote usdPriceQuote = PriceQuote.fromFiatPrice(83_875.1546, "USD");

        Monetary amount = BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount(usdPriceQuote, usdPriceQuote, 1_200);

        assertEquals(Fiat.fromFaceValue(6, "USD"), amount);
    }

    private static Monetary convertedUsdAllowanceRoundedToWholeUnits(PriceQuote fiatPriceQuote,
                                                                     PriceQuote usdPriceQuote,
                                                                     long score) {
        Monetary btcAmount = usdPriceQuote.toBaseSideMonetary(BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(score));
        return fiatPriceQuote.toQuoteSideMonetary(btcAmount).round(0);
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
