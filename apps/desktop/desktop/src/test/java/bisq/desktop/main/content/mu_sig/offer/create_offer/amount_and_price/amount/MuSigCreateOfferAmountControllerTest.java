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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount;

import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies MuSigCreateOfferAmountController#isFixedAmount, the range vs fixed
 * decision when creating a MuSig offer.
 *
 * On a fiat market the decision must use the quote side value rounded to the fiat
 * display precision, so two amounts that are equal in fiat but differ by a few sats
 * on the base side (from price conversion rounding) collapse to a fixed offer instead
 * of a spurious range. Non-fiat markets, or missing quote amounts, keep the exact
 * base side comparison.
 */
class MuSigCreateOfferAmountControllerTest {
    private static final long BASE_A = 5_000_000L;
    private static final long BASE_B = 5_000_001L; // differs from BASE_A by 1 sat

    private static Monetary eur(double faceValue) {
        return Fiat.fromFaceValue(faceValue, "EUR");
    }

    @Test
    void fiatEqualAfterRounding_withDifferentBaseAmounts_isFixed() {
        // Both quote amounts round to 100.00, base side differs by 1 sat: the bug case.
        assertTrue(MuSigCreateOfferAmountController.isFixedAmount(
                true, eur(100.001), eur(100.004), BASE_A, BASE_B));
    }

    @Test
    void fiatExactlyEqual_isFixed() {
        assertTrue(MuSigCreateOfferAmountController.isFixedAmount(
                true, eur(100.00), eur(100.00), BASE_A, BASE_B));
    }

    @Test
    void fiatDifferentAfterRounding_isRange() {
        // 100.00 and 100.05 are different fiat values.
        assertFalse(MuSigCreateOfferAmountController.isFixedAmount(
                true, eur(100.00), eur(100.05), BASE_A, BASE_A));
    }

    @Test
    void fiatDifferentAcrossRoundingBoundary_isRange() {
        // 100.004 rounds to 100.00, 100.006 rounds to 100.01, so they differ.
        assertFalse(MuSigCreateOfferAmountController.isFixedAmount(
                true, eur(100.004), eur(100.006), BASE_A, BASE_A));
    }

    @Test
    void nonFiatMarket_usesExactBaseComparison() {
        // Quote side is ignored for non-fiat markets: equal base -> fixed, differing base -> range.
        assertTrue(MuSigCreateOfferAmountController.isFixedAmount(
                false, eur(100.00), eur(100.05), BASE_A, BASE_A));
        assertFalse(MuSigCreateOfferAmountController.isFixedAmount(
                false, eur(100.00), eur(100.00), BASE_A, BASE_B));
    }

    @Test
    void fiatMarketWithMissingQuoteAmounts_fallsBackToBaseComparison() {
        assertTrue(MuSigCreateOfferAmountController.isFixedAmount(
                true, null, null, BASE_A, BASE_A));
        assertFalse(MuSigCreateOfferAmountController.isFixedAmount(
                true, null, null, BASE_A, BASE_B));
    }
}
