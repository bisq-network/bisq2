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

package bisq.bisq_easy;

import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class BisqEasyTradeAmountLimitsTest {

    @Test
    @DisplayName("get max usd trade amount zero score returns zero")
    void get_max_usd_trade_amount_zero_score_returns_zero() {
        Fiat result = BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(0);
        assertEquals(0, result.getValue());
        assertEquals("USD", result.getCode());
    }

    @Test
    @DisplayName("get max usd trade amount scales with200 per usd")
    void get_max_usd_trade_amount_scales_with200_per_usd() {
        // 30_000 score / 200 per USD = $150
        Fiat result = BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(30_000);
        assertEquals(Fiat.fromFaceValue(150, "USD").getValue(), result.getValue());
    }

    @Test
    @DisplayName("get max usd trade amount caps at600 usd")
    void get_max_usd_trade_amount_caps_at600_usd() {
        // 200_000 score / 200 = $1000, but cap is $600
        Fiat result = BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(200_000);
        assertEquals(Fiat.fromFaceValue(600, "USD").getValue(), result.getValue());
    }

    @Test
    @DisplayName("get max usd trade amount at exact cap")
    void get_max_usd_trade_amount_at_exact_cap() {
        // 120_000 / 200 = $600 exactly
        Fiat result = BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(120_000);
        assertEquals(Fiat.fromFaceValue(600, "USD").getValue(), result.getValue());
    }

    @Test
    @DisplayName("get max usd trade amount just below cap")
    void get_max_usd_trade_amount_just_below_cap() {
        // 119_999 / 200 = $599.995 → rounds to $600
        Fiat result = BisqEasyTradeAmountLimits.getMaxUsdTradeAmount(119_999);
        assertEquals(Fiat.fromFaceValue(600, "USD").getValue(), result.getValue());
    }

    @Test
    @DisplayName("get required reputation score by usd amount one usd")
    void get_required_reputation_score_by_usd_amount_one_usd() {
        Monetary usdAmount = Fiat.fromFaceValue(1, "USD");
        long score = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usdAmount);
        assertEquals(200, score);
    }

    @Test
    @DisplayName("get required reputation score by usd amount150 usd")
    void get_required_reputation_score_by_usd_amount150_usd() {
        Monetary usdAmount = Fiat.fromFaceValue(150, "USD");
        long score = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usdAmount);
        assertEquals(30_000, score);
    }

    @Test
    @DisplayName("get required reputation score by usd amount600 usd")
    void get_required_reputation_score_by_usd_amount600_usd() {
        Monetary usdAmount = Fiat.fromFaceValue(600, "USD");
        long score = BisqEasyTradeAmountLimits.getRequiredReputationScoreByUsdAmount(usdAmount);
        assertEquals(120_000, score);
    }

    @Test
    @DisplayName("get usd amount from reputation score inverse of required")
    void get_usd_amount_from_reputation_score_inverse_of_required() {
        Monetary usd = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(30_000);
        double faceValue = Monetary.toFaceValue(usd, 0);
        assertEquals(150.0, faceValue);
    }

    @Test
    @DisplayName("get usd amount from reputation score zero")
    void get_usd_amount_from_reputation_score_zero() {
        Monetary usd = BisqEasyTradeAmountLimits.getUsdAmountFromReputationScore(0);
        assertEquals(0, usd.getValue());
    }

    @Test
    @DisplayName("with tolerance applies5 percent")
    void with_tolerance_applies5_percent() {
        long result = BisqEasyTradeAmountLimits.withTolerance(10_000);
        assertEquals(10_500, result);
    }

    @Test
    @DisplayName("with tolerance zero")
    void with_tolerance_zero() {
        assertEquals(0, BisqEasyTradeAmountLimits.withTolerance(0));
    }

    @Test
    @DisplayName("min reputation score threshold at boundary")
    void min_reputation_score_threshold_at_boundary() {
        // Score exactly at threshold should be allowed
        assertTrue(1200L >= BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER);
    }

    @Test
    @DisplayName("min reputation score threshold below boundary")
    void min_reputation_score_threshold_below_boundary() {
        // Score below threshold should not be allowed
        assertFalse(1199L >= BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER);
    }

    @Test
    @DisplayName("min reputation score threshold above boundary")
    void min_reputation_score_threshold_above_boundary() {
        assertTrue(5000L >= BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER);
    }

    @Test
    @DisplayName("min reputation score threshold zero")
    void min_reputation_score_threshold_zero() {
        assertFalse(0L >= BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER);
    }

    @Test
    @DisplayName("constants have expected values")
    void constants_have_expected_values() {
        assertEquals(6, Monetary.toFaceValue(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT, 0));
        assertEquals(600, Monetary.toFaceValue(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT, 0));
        assertEquals(0, Monetary.toFaceValue(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION, 0));
        assertEquals(0.05, BisqEasyTradeAmountLimits.TOLERANCE);
        assertEquals(1200, BisqEasyTradeAmountLimits.MIN_REPUTATION_SCORE_TO_CREATE_SELL_OFFER);
    }

    @Test
    @DisplayName("result enum match score is valid")
    void result_enum_match_score_is_valid() {
        assertTrue(BisqEasyTradeAmountLimits.Result.MATCH_SCORE.isValid());
    }

    @Test
    @DisplayName("result enum match tolerated score is valid")
    void result_enum_match_tolerated_score_is_valid() {
        assertTrue(BisqEasyTradeAmountLimits.Result.MATCH_TOLERATED_SCORE.isValid());
    }

    @Test
    @DisplayName("result enum score too low is not valid")
    void result_enum_score_too_low_is_not_valid() {
        assertFalse(BisqEasyTradeAmountLimits.Result.SCORE_TOO_LOW.isValid());
    }
}
