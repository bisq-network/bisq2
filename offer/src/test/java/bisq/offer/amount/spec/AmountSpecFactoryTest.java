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

package bisq.offer.amount.spec;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.TradeAmount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class AmountSpecFactoryTest {

    @Test
    void baseSideRangeWithQuoteAmountsEqualAtDisplayPrecisionBecomesFixed() {
        // 4000.0000 vs 4000.0040 USD: equal at the fiat display precision, but the base side
        // amounts differ by a few sats from price conversion rounding.
        TradeAmount min = tradeAmount(10_000_000, 40_000_000);
        TradeAmount max = tradeAmount(10_000_005, 40_000_040);
        BaseSideAmountSpec spec = AmountSpecFactory.createBaseSideAmountSpec(true, min, max, min);
        BaseSideFixedAmountSpec fixed = assertInstanceOf(BaseSideFixedAmountSpec.class, spec);
        assertEquals(10_000_005, fixed.getAmount());
    }

    @Test
    void baseSideRangeWithQuoteAmountsDifferingAtDisplayPrecisionStaysRange() {
        // 4000.00 vs 4001.00 USD: a real range.
        TradeAmount min = tradeAmount(10_000_000, 40_000_000);
        TradeAmount max = tradeAmount(10_002_500, 40_010_000);
        BaseSideAmountSpec spec = AmountSpecFactory.createBaseSideAmountSpec(true, min, max, min);
        BaseSideRangeAmountSpec range = assertInstanceOf(BaseSideRangeAmountSpec.class, spec);
        assertEquals(10_000_000, range.getMinAmount());
        assertEquals(10_002_500, range.getMaxAmount());
    }

    private static TradeAmount tradeAmount(long baseSideValue, long quoteSideValue) {
        return new TradeAmount(Coin.asBtcFromValue(baseSideValue), Fiat.fromValue(quoteSideValue, "USD"));
    }

    @Test
    void quoteSideWithEqualMinAndMaxBecomesFixed() {
        QuoteSideAmountSpec spec = AmountSpecFactory.createQuoteSideAmountSpec(100, 100);
        QuoteSideFixedAmountSpec fixed = assertInstanceOf(QuoteSideFixedAmountSpec.class, spec);
        assertEquals(100, fixed.getAmount());
    }

    @Test
    void quoteSideWithDifferentMinAndMaxStaysRange() {
        QuoteSideAmountSpec spec = AmountSpecFactory.createQuoteSideAmountSpec(100, 200);
        QuoteSideRangeAmountSpec range = assertInstanceOf(QuoteSideRangeAmountSpec.class, spec);
        assertEquals(100, range.getMinAmount());
        assertEquals(200, range.getMaxAmount());
    }

    @Test
    void baseSideWithEqualMinAndMaxBecomesFixed() {
        BaseSideAmountSpec spec = AmountSpecFactory.createBaseSideAmountSpec(100, 100);
        BaseSideFixedAmountSpec fixed = assertInstanceOf(BaseSideFixedAmountSpec.class, spec);
        assertEquals(100, fixed.getAmount());
    }

    @Test
    void baseSideWithDifferentMinAndMaxStaysRange() {
        BaseSideAmountSpec spec = AmountSpecFactory.createBaseSideAmountSpec(100, 200);
        BaseSideRangeAmountSpec range = assertInstanceOf(BaseSideRangeAmountSpec.class, spec);
        assertEquals(100, range.getMinAmount());
        assertEquals(200, range.getMaxAmount());
    }
}
