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

import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BisqEasyOfferAmountValidatorTest {
    @Test
    void rejectsNonPositiveBaseSideAmount() {
        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(
                new BaseSideFixedAmountSpec(100),
                0,
                1));
    }

    @Test
    void rejectsNonPositiveQuoteSideAmount() {
        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(
                new BaseSideFixedAmountSpec(100),
                1,
                0));
    }

    @Test
    void acceptsBaseSideFixedAmount() {
        BaseSideFixedAmountSpec amountSpec = new BaseSideFixedAmountSpec(100);

        assertDoesNotThrow(() -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, 100, 10_000));
    }

    @ParameterizedTest
    @ValueSource(longs = {99, 101})
    void rejectsBaseSideFixedAmountOutsideOfferAmount(long baseSideAmount) {
        BaseSideFixedAmountSpec amountSpec = new BaseSideFixedAmountSpec(100);

        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, baseSideAmount, 10_000));
    }

    @ParameterizedTest
    @ValueSource(longs = {100, 150, 200})
    void acceptsBaseSideRangeAmount(long baseSideAmount) {
        BaseSideRangeAmountSpec amountSpec = new BaseSideRangeAmountSpec(100, 200);

        assertDoesNotThrow(() -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, baseSideAmount, 10_000));
    }

    @ParameterizedTest
    @ValueSource(longs = {99, 201})
    void rejectsBaseSideRangeAmountOutsideOfferRange(long baseSideAmount) {
        BaseSideRangeAmountSpec amountSpec = new BaseSideRangeAmountSpec(100, 200);

        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, baseSideAmount, 10_000));
    }

    @Test
    void acceptsQuoteSideFixedAmount() {
        QuoteSideFixedAmountSpec amountSpec = new QuoteSideFixedAmountSpec(10_000);

        assertDoesNotThrow(() -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, 100, 10_000));
    }

    @ParameterizedTest
    @ValueSource(longs = {9_999, 10_001})
    void rejectsQuoteSideFixedAmountOutsideOfferAmount(long quoteSideAmount) {
        QuoteSideFixedAmountSpec amountSpec = new QuoteSideFixedAmountSpec(10_000);

        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, 100, quoteSideAmount));
    }

    @ParameterizedTest
    @ValueSource(longs = {10_000, 15_000, 20_000})
    void acceptsQuoteSideRangeAmount(long quoteSideAmount) {
        QuoteSideRangeAmountSpec amountSpec = new QuoteSideRangeAmountSpec(10_000, 20_000);

        assertDoesNotThrow(() -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, 100, quoteSideAmount));
    }

    @ParameterizedTest
    @ValueSource(longs = {9_999, 20_001})
    void rejectsQuoteSideRangeAmountOutsideOfferRange(long quoteSideAmount) {
        QuoteSideRangeAmountSpec amountSpec = new QuoteSideRangeAmountSpec(10_000, 20_000);

        assertThrows(IllegalArgumentException.class, () -> BisqEasyOfferAmountValidator.validateOfferAmount(amountSpec, 100, quoteSideAmount));
    }
}
