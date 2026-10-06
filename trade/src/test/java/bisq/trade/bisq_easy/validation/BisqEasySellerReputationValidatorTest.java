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

package bisq.trade.bisq_easy.validation;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceProviderInfo;
import bisq.common.monetary.PriceQuote;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static bisq.trade.bisq_easy.validation.BisqEasySellerReputationValidator.MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BisqEasySellerReputationValidatorTest {
    // MarketPrice treats a price older than 12 hours as outdated
    private static final long OUTDATED_AGE = TimeUnit.HOURS.toMillis(12) + TimeUnit.MINUTES.toMillis(1);

    @Test
    void acceptsScoreMatchingRequiredScore() {
        assertDoesNotThrow(() -> BisqEasySellerReputationValidator.validateSellersReputation(20_000, 20_000));
    }

    @Test
    void acceptsScoreWithinTolerance() {
        // 19_048 * 1.05 = 20_000.4
        assertDoesNotThrow(() -> BisqEasySellerReputationValidator.validateSellersReputation(19_048, 20_000));
    }

    @Test
    void rejectsScoreBelowTolerance() {
        // 19_047 * 1.05 = 19_999.35
        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> BisqEasySellerReputationValidator.validateSellersReputation(19_047, 20_000));
        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
        assertTrue(exception.getMessage().contains("5% tolerance"));
    }

    @Test
    void rejectsZeroScoreForSmallestAmount() {
        TradeProtocolException exception = assertThrows(TradeProtocolException.class,
                () -> BisqEasySellerReputationValidator.validateSellersReputation(0, 1));
        assertEquals(TradeProtocolFailure.SELLER_REPUTATION_TOO_LOW, exception.getTradeProtocolFailure());
    }

    @Test
    void acceptsZeroScoreWhenNoReputationIsRequired() {
        assertDoesNotThrow(() -> BisqEasySellerReputationValidator.validateSellersReputation(0, 0));
    }

    private static MarketPrice eurMarketPrice(long timestamp) {
        return new MarketPrice(PriceQuote.fromFiatPrice(50_000, "EUR"), timestamp, mock(MarketPriceProviderInfo.class));
    }

    private static MarketPrice usdMarketPrice(long timestamp) {
        return new MarketPrice(PriceQuote.fromFiatPrice(60_000, "USD"), timestamp, mock(MarketPriceProviderInfo.class));
    }

    @Test
    void acceptsMarketPricesWithinMaxTimestampDifference() {
        long now = System.currentTimeMillis();
        assertDoesNotThrow(() -> BisqEasySellerReputationValidator.validateMarketPrices(
                eurMarketPrice(now - MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE),
                usdMarketPrice(now)));
    }

    @Test
    void rejectsMarketPricesNotFromTheSameTime() {
        long now = System.currentTimeMillis();
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                eurMarketPrice(now - MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE - 1),
                usdMarketPrice(now)));
    }

    @Test
    void rejectsOutdatedFiatMarketPrice() {
        long outdatedTimestamp = System.currentTimeMillis() - OUTDATED_AGE;
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                eurMarketPrice(outdatedTimestamp),
                usdMarketPrice(outdatedTimestamp + MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE)));
    }

    @Test
    void rejectsOutdatedUsdMarketPrice() {
        long outdatedTimestamp = System.currentTimeMillis() - OUTDATED_AGE;
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                eurMarketPrice(outdatedTimestamp + MAX_MARKET_PRICE_TIMESTAMP_DIFFERENCE),
                usdMarketPrice(outdatedTimestamp)));
    }
}
