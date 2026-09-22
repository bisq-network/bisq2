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
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BisqEasySellerReputationValidatorTest {
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

    private static MarketPrice marketPrice(long timestamp, boolean isValidDate) {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getTimestamp()).thenReturn(timestamp);
        when(marketPrice.isValidDate()).thenReturn(isValidDate);
        return marketPrice;
    }

    @Test
    void acceptsMarketPricesWithinStaleAgeOfEachOther() {
        long now = System.currentTimeMillis();
        assertDoesNotThrow(() -> BisqEasySellerReputationValidator.validateMarketPrices(
                marketPrice(now - MarketPrice.STALE_AGE, true),
                marketPrice(now, true)));
    }

    @Test
    void rejectsMarketPricesNotFromTheSameTime() {
        long now = System.currentTimeMillis();
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                marketPrice(now - MarketPrice.STALE_AGE - 1, true),
                marketPrice(now, true)));
    }

    @Test
    void rejectsOutdatedFiatMarketPrice() {
        long now = System.currentTimeMillis();
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                marketPrice(now, false),
                marketPrice(now, true)));
    }

    @Test
    void rejectsOutdatedUsdMarketPrice() {
        long now = System.currentTimeMillis();
        assertThrows(IllegalArgumentException.class, () -> BisqEasySellerReputationValidator.validateMarketPrices(
                marketPrice(now, true),
                marketPrice(now, false)));
    }
}
