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
    void calculateForTypeNoPayoutReturnsEmpty() {
        Optional<MuSigMediationPayoutResolver.PayoutAmounts> payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.empty());

        assertTrue(payoutAmounts.isEmpty());
    }

    @Test
    void calculateForTypeBuyerGetsTradeAmount() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                        CONTEXT,
                        Optional.empty())
                .orElseThrow();

        assertEquals(600_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(120_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeSellerGetsTradeAmount() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT,
                        CONTEXT,
                        Optional.empty())
                .orElseThrow();

        assertEquals(120_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(600_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeBuyerPlusCompensationWithTenPercent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(648_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(72_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeBuyerMinusPenaltyWithTenPercent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(552_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(168_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeBuyerMinusPenaltyWithNinetyPercent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.90))
                .orElseThrow();

        assertEquals(168_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(552_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeSellerPlusCompensationWithTenPercent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(72_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(648_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeSellerMinusPenaltyWithTenPercent() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(0.10))
                .orElseThrow();

        assertEquals(168_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(552_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void compensationTransferIsCappedByMinimumRefundConstraint() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                        CONTEXT,
                        Optional.of(0.50))
                .orElseThrow();

        assertEquals(696_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(24_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void buyerPenaltyAtHundredPercentKeepsMinimumRefundConstraint() {
        MuSigMediationPayoutResolver.PayoutAmounts payoutAmounts = MuSigMediationPayoutResolver.calculateForType(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY,
                        CONTEXT,
                        Optional.of(1.0))
                .orElseThrow();

        assertEquals(120_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(600_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void customPayoutBuyerEditedIsResolvedToMinimumRefund() {
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
    void customPayoutSellerEditedIsResolvedToMinimumRefund() {
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
    void customPayoutMissingInputReturnsEmpty() {
        Optional<MuSigMediationPayoutResolver.PayoutAmounts> payoutAmounts =
                MuSigMediationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.empty(),
                        Optional.empty(),
                        true);

        assertTrue(payoutAmounts.isEmpty());
    }

    @Test
    void checkPayoutAmountsAcceptsNoPayoutWithoutAmounts() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    void checkPayoutAmountsRejectsNoPayoutWithAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.NO_PAYOUT,
                CONTEXT,
                Optional.of(1L),
                Optional.empty(),
                Optional.empty()));
    }

    @Test
    void checkPayoutAmountsAcceptsMatchingBuyerPlusCompensation() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(648_000L),
                Optional.of(72_000L),
                Optional.of(0.10)));
    }

    @Test
    void checkPayoutAmountsRejectsMismatchingBuyerPlusCompensation() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(600_000L),
                Optional.of(120_000L),
                Optional.of(0.10)));
    }

    @Test
    void checkPayoutAmountsRejectsMissingAdjustmentForAdjustmentType() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION,
                CONTEXT,
                Optional.of(600_000L),
                Optional.of(120_000L),
                Optional.empty()));
    }

    @Test
    void checkPayoutAmountsAcceptsCustomPayoutWithinMinimumRefundBounds() {
        assertDoesNotThrow(() -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(100_000L),
                Optional.of(620_000L),
                Optional.empty()));
    }

    @Test
    void checkPayoutAmountsRejectsCustomPayoutBelowMinimumRefundBounds() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(10_000L),
                Optional.of(710_000L),
                Optional.empty()));
    }

    @Test
    void checkPayoutAmountsRejectsCustomPayoutOverflow() {
        assertThrows(IllegalArgumentException.class, () -> MuSigMediationPayoutResolver.checkPayoutAmounts(
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Optional.of(Long.MAX_VALUE),
                Optional.of(Long.MAX_VALUE),
                Optional.empty()));
    }
}
