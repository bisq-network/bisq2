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

package bisq.support.mediation.mu_sig;

import bisq.support.mediation.MediationPayoutDistributionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuSigMediationPayoutResolverTest {
    private static final long TRADE_AMOUNT_SATS = 480_000; // 0.0048 BTC
    private static final long SECURITY_DEPOSIT_SATS = 120_000; // 25% of trade amount
    private static final long MIN_REFUND_AMOUNT_SATS = 24_000; // 5% of trade amount
    private static final long TOTAL_PAYOUT_SATS = 720_000;

    private static final MuSigMediationPayoutResolver.PayoutContext CONTEXT =
            new MuSigMediationPayoutResolver.PayoutContext(
                    TRADE_AMOUNT_SATS,
                    SECURITY_DEPOSIT_SATS,
                    SECURITY_DEPOSIT_SATS,
                    MIN_REFUND_AMOUNT_SATS,
                    TOTAL_PAYOUT_SATS);

    @Test
    @DisplayName("calculate for type no payout returns empty")
    void calculate_for_type_no_payout_returns_empty() {
        Optional<MuSigMediationPayoutResolver.PayoutAmounts> payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.empty());

        assertTrue(payoutAmounts.isEmpty());
    }

    @Test
    @DisplayName("calculate for type buyer gets trade amount")
    void calculate_for_type_buyer_gets_trade_amount() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                        CONTEXT,
                        Optional.empty())
                .orElseThrow();

        assertEquals(600_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(120_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type seller gets trade amount")
    void calculate_for_type_seller_gets_trade_amount() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT,
                        CONTEXT,
                        Optional.empty())
                .orElseThrow();

        assertEquals(120_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(600_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type buyer plus compensation with ten percent")
    void calculate_for_type_buyer_plus_compensation_with_ten_percent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(648_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(72_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type buyer minus penalty with ten percent")
    void calculate_for_type_buyer_minus_penalty_with_ten_percent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(552_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(168_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type buyer minus penalty with ninety percent")
    void calculate_for_type_buyer_minus_penalty_with_ninety_percent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.90))
                .orElseThrow();

        assertEquals(168_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(552_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type seller plus compensation with ten percent")
    void calculate_for_type_seller_plus_compensation_with_ten_percent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(72_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(648_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("calculate for type seller minus penalty with ten percent")
    void calculate_for_type_seller_minus_penalty_with_ten_percent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(168_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(552_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("compensation transfer is capped by minimum refund constraint")
    void compensation_transfer_is_capped_by_minimum_refund_constraint() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.50))
                .orElseThrow();

        assertEquals(696_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(24_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("buyer penalty at hundred percent keeps minimum refund constraint")
    void buyer_penalty_at_hundred_percent_keeps_minimum_refund_constraint() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(1.0))
                .orElseThrow();

        assertEquals(120_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(600_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("custom payout buyer edited is resolved to minimum refund")
    void custom_payout_buyer_edited_is_resolved_to_minimum_refund() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.of(10_000L),
                        Optional.empty(),
                        true)
                .orElseThrow();

        assertEquals(24_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(696_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("custom payout seller edited is resolved to minimum refund")
    void custom_payout_seller_edited_is_resolved_to_minimum_refund() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.empty(),
                        Optional.of(1_000_000L),
                        false)
                .orElseThrow();

        assertEquals(24_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(696_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    @DisplayName("custom payout missing input returns empty")
    void custom_payout_missing_input_returns_empty() {
        Optional<MuSigMediationPayoutResolver.PayoutAmounts> payoutAmounts =
                MuSigMediationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.empty(),
                        Optional.empty(),
                        true);

        assertTrue(payoutAmounts.isEmpty());
    }

    @Test
    @DisplayName("check payout amounts accepts no payout without amounts")
    void check_payout_amounts_accepts_no_payout_without_amounts() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    @DisplayName("check payout amounts rejects no payout with amounts")
    void check_payout_amounts_rejects_no_payout_with_amounts() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.of(1L),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    @DisplayName("check payout amounts accepts matching buyer plus compensation")
    void check_payout_amounts_accepts_matching_buyer_plus_compensation() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(648_000L),
                Optional.of(72_000L),
                Optional.of(0.10)));
    }

    @Test
    @DisplayName("check payout amounts rejects mismatching buyer plus compensation")
    void check_payout_amounts_rejects_mismatching_buyer_plus_compensation() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(600_000L),
                Optional.of(120_000L),
                Optional.of(0.10)));
    }

    @Test
    @DisplayName("check payout amounts rejects missing adjustment for adjustment type")
    void check_payout_amounts_rejects_missing_adjustment_for_adjustment_type() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(600_000L),
                Optional.of(120_000L),
                Optional.empty()));
    }

    @Test
    @DisplayName("check payout amounts accepts custom payout within minimum refund bounds")
    void check_payout_amounts_accepts_custom_payout_within_minimum_refund_bounds() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(100_000L),
                Optional.of(620_000L),
                Optional.empty()));
    }

    @Test
    @DisplayName("check payout amounts rejects custom payout below minimum refund bounds")
    void check_payout_amounts_rejects_custom_payout_below_minimum_refund_bounds() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(10_000L),
                Optional.of(710_000L),
                Optional.empty()));
    }

    @Test
    @DisplayName("check payout amounts rejects custom payout overflow")
    void check_payout_amounts_rejects_custom_payout_overflow() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(Long.MAX_VALUE),
                Optional.of(Long.MAX_VALUE),
                Optional.empty()));
    }
}
