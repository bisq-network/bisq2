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

package bisq.support.arbitration.mu_sig;

import bisq.support.arbitration.ArbitrationPayoutDistributionType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MuSigArbitrationPayoutResolverTest {
    private static final long TRADE_AMOUNT_SATS = 480_000; // 0.0048 BTC
    private static final long BUYER_SECURITY_DEPOSIT_SATS = 120_000; // 25% of trade amount
    private static final long SELLER_SECURITY_DEPOSIT_SATS = 120_000; // 25% of trade amount
    private static final long TOTAL_PAYOUT_SATS = 720_000;

    private static final MuSigArbitrationPayoutResolver.PayoutContext CONTEXT =
            new MuSigArbitrationPayoutResolver.PayoutContext(
                    TRADE_AMOUNT_SATS,
                    BUYER_SECURITY_DEPOSIT_SATS,
                    SELLER_SECURITY_DEPOSIT_SATS,
                    TOTAL_PAYOUT_SATS);

    @Test
    void calculateForTypeBuyerGetsTradeAmount() {
        MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts = MuSigArbitrationPayoutResolver.calculateForType(
                        ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                        CONTEXT)
                .orElseThrow();

        assertEquals(600_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(0, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeSellerGetsTradeAmount() {
        MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts = MuSigArbitrationPayoutResolver.calculateForType(
                        ArbitrationPayoutDistributionType.SELLER_GETS_TRADE_AMOUNT,
                        CONTEXT)
                .orElseThrow();

        assertEquals(0, payoutAmounts.buyerAmountAsSats());
        assertEquals(600_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void calculateForTypeCustomPayoutReturnsEmpty() {
        Optional<MuSigArbitrationPayoutResolver.PayoutAmounts> payoutAmounts = MuSigArbitrationPayoutResolver.calculateForType(
                ArbitrationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT);

        assertEquals(Optional.empty(), payoutAmounts);
    }

    @Test
    void customPayoutCanLeaveAmountUndistributed() {
        MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts = MuSigArbitrationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.of(100_000L),
                        Optional.of(200_000L),
                        true)
                .orElseThrow();

        assertEquals(100_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(200_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void customPayoutBuyerEditedReducesSellerAmountWhenTotalWouldBeExceeded() {
        MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts = MuSigArbitrationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.of(600_000L),
                        Optional.of(300_000L),
                        true)
                .orElseThrow();

        assertEquals(600_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(120_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void customPayoutSellerEditedReducesBuyerAmountWhenTotalWouldBeExceeded() {
        MuSigArbitrationPayoutResolver.PayoutAmounts payoutAmounts = MuSigArbitrationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.of(600_000L),
                        Optional.of(300_000L),
                        false)
                .orElseThrow();

        assertEquals(420_000, payoutAmounts.buyerAmountAsSats());
        assertEquals(300_000, payoutAmounts.sellerAmountAsSats());
    }

    @Test
    void customPayoutMissingInputReturnsEmpty() {
        Optional<MuSigArbitrationPayoutResolver.PayoutAmounts> payoutAmounts =
                MuSigArbitrationPayoutResolver.resolveCustomPayout(
                        CONTEXT,
                        Optional.empty(),
                        Optional.of(100_000L),
                        true);

        assertEquals(Optional.empty(), payoutAmounts);
    }

    @Test
    void checkPayoutAmountsAcceptsMatchingBuyerGetsTradeAmount() {
        assertDoesNotThrow(() -> MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                CONTEXT,
                600_000L,
                0L));
    }

    @Test
    void checkPayoutAmountsRejectsMismatchingBuyerGetsTradeAmount() {
        assertThrows(IllegalArgumentException.class, () -> MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                ArbitrationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                CONTEXT,
                500_000L,
                100_000L));
    }

    @Test
    void checkPayoutAmountsAcceptsCustomPayoutBelowTotalPayoutAmount() {
        assertDoesNotThrow(() -> MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                ArbitrationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                100_000L,
                200_000L));
    }

    @Test
    void checkPayoutAmountsRejectsCustomPayoutAboveTotalPayoutAmount() {
        assertThrows(IllegalArgumentException.class, () -> MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                ArbitrationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                600_000L,
                200_001L));
    }

    @Test
    void checkPayoutAmountsRejectsCustomPayoutOverflow() {
        assertThrows(IllegalArgumentException.class, () -> MuSigArbitrationPayoutResolver.checkPayoutAmounts(
                ArbitrationPayoutDistributionType.CUSTOM_PAYOUT,
                CONTEXT,
                Long.MAX_VALUE,
                Long.MAX_VALUE));
    }
}
