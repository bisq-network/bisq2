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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.components;

import bisq.common.util.MathUtils;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.options.CollateralOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.trade.mu_sig.MuSigTradeUtils;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

final class MuSigMediationPayoutDistributionCalculator {
    static final double MIN_REFUND_PERCENTAGE = 0.05; // 5%

    record PayoutContext(long tradeAmount, long buyerSecurityDeposit, long sellerSecurityDeposit,
                         long minimumRefundAmount, long totalPayoutAmount) {
        long minPayoutAmount() {
            return minimumRefundAmount;
        }

        long maxPayoutAmount() {
            return totalPayoutAmount - minimumRefundAmount;
        }

        long maxTransferAmountKeepingMinimum(long losingSideBaseAmount) {
            return Math.max(0, losingSideBaseAmount - minimumRefundAmount);
        }
    }

    record PayoutAmounts(long buyerAmountAsSats, long sellerAmountAsSats) {
    }

    static Optional<PayoutContext> createPayoutContext(MuSigContract contract) {
        Optional<CollateralOption> collateralOption =
                OfferOptionUtil.findCollateralOption(contract.getOffer().getOfferOptions());
        if (collateralOption.isEmpty()) {
            return Optional.empty();
        }

        long tradeAmount = contract.getBtcSideAmount();
        long buyerSecurityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(
                MuSigTradeUtils.getBtcSideMonetary(contract),
                collateralOption.get().getBuyerSecurityDeposit()).getValue();
        long sellerSecurityDeposit = OfferAmountUtil.calculateSecurityDepositAsBTC(
                MuSigTradeUtils.getBtcSideMonetary(contract),
                collateralOption.get().getSellerSecurityDeposit()).getValue();
        long minimumRefundAmount = MathUtils.roundDoubleToLong(tradeAmount * MIN_REFUND_PERCENTAGE);
        long totalPayoutAmount = tradeAmount + buyerSecurityDeposit + sellerSecurityDeposit;
        return Optional.of(new PayoutContext(
                tradeAmount,
                buyerSecurityDeposit,
                sellerSecurityDeposit,
                minimumRefundAmount,
                totalPayoutAmount));
    }

    static Optional<PayoutAmounts> calculateForType(MediationPayoutDistributionType payoutDistributionType,
                                                    PayoutContext context,
                                                    Optional<Double> payoutAdjustmentPercentageValue) {
        return switch (payoutDistributionType) {
            case NO_PAYOUT -> Optional.empty();
            case CUSTOM_PAYOUT -> Optional.empty();
            case BUYER_GETS_TRADE_AMOUNT -> Optional.of(new PayoutAmounts(
                    context.tradeAmount() + context.buyerSecurityDeposit(),
                    context.sellerSecurityDeposit()));
            case SELLER_GETS_TRADE_AMOUNT -> Optional.of(new PayoutAmounts(
                    context.buyerSecurityDeposit(),
                    context.tradeAmount() + context.sellerSecurityDeposit()));
            case BUYER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION -> Optional.of(applyBuyerPlusCompensation(context, payoutAdjustmentPercentageValue));
            case BUYER_GETS_TRADE_AMOUNT_MINUS_PENALTY -> Optional.of(applyBuyerMinusPenalty(context, payoutAdjustmentPercentageValue));
            case SELLER_GETS_TRADE_AMOUNT_PLUS_COMPENSATION -> Optional.of(applySellerPlusCompensation(context, payoutAdjustmentPercentageValue));
            case SELLER_GETS_TRADE_AMOUNT_MINUS_PENALTY -> Optional.of(applySellerMinusPenalty(context, payoutAdjustmentPercentageValue));
        };
    }

    static Optional<PayoutAmounts> alignCustomPayout(PayoutContext context,
                                                     Optional<Long> buyerPayoutAmountAsSats,
                                                     Optional<Long> sellerPayoutAmountAsSats,
                                                     boolean buyerFieldEdited) {
        long minPayoutAmount = context.minPayoutAmount();
        long maxPayoutAmount = context.maxPayoutAmount();
        if (buyerFieldEdited) {
            return buyerPayoutAmountAsSats
                    .map(buyerAmountAsSats -> {
                        long clampedBuyerAmount = MathUtils.bounded(minPayoutAmount, maxPayoutAmount, buyerAmountAsSats);
                        long sellerAmountAsSats = context.totalPayoutAmount() - clampedBuyerAmount;
                        return new PayoutAmounts(clampedBuyerAmount, sellerAmountAsSats);
                    });
        } else {
            return sellerPayoutAmountAsSats
                    .map(sellerAmountAsSats -> {
                        long clampedSellerAmount = MathUtils.bounded(minPayoutAmount, maxPayoutAmount, sellerAmountAsSats);
                        long buyerAmountAsSats = context.totalPayoutAmount() - clampedSellerAmount;
                        return new PayoutAmounts(buyerAmountAsSats, clampedSellerAmount);
                    });
        }
    }

    private static PayoutAmounts applyBuyerPlusCompensation(PayoutContext context,
                                                            Optional<Double> payoutAdjustmentPercentageValue) {
        long buyersBaseAmount = context.tradeAmount() + context.buyerSecurityDeposit();
        long sellersBaseAmount = context.sellerSecurityDeposit();
        long maxTransferAmount = context.maxTransferAmountKeepingMinimum(sellersBaseAmount);
        long transferAmount = getTransferAmount(context, payoutAdjustmentPercentageValue, maxTransferAmount);
        return new PayoutAmounts(buyersBaseAmount + transferAmount, sellersBaseAmount - transferAmount);
    }

    private static PayoutAmounts applyBuyerMinusPenalty(PayoutContext context,
                                                        Optional<Double> payoutAdjustmentPercentageValue) {
        long buyerBaseAmount = context.tradeAmount() + context.buyerSecurityDeposit();
        long maxTransferAmount = context.maxTransferAmountKeepingMinimum(buyerBaseAmount);
        long transferAmount = getTransferAmount(context, payoutAdjustmentPercentageValue, maxTransferAmount);
        return new PayoutAmounts(buyerBaseAmount - transferAmount, context.sellerSecurityDeposit() + transferAmount);
    }

    private static PayoutAmounts applySellerPlusCompensation(PayoutContext context,
                                                             Optional<Double> payoutAdjustmentPercentageValue) {
        long buyersBaseAmount = context.buyerSecurityDeposit();
        long sellersBaseAmount = context.tradeAmount() + context.sellerSecurityDeposit();
        long maxTransferAmount = context.maxTransferAmountKeepingMinimum(buyersBaseAmount);
        long transferAmount = getTransferAmount(context, payoutAdjustmentPercentageValue, maxTransferAmount);
        return new PayoutAmounts(buyersBaseAmount - transferAmount, sellersBaseAmount + transferAmount);
    }

    private static PayoutAmounts applySellerMinusPenalty(PayoutContext context,
                                                         Optional<Double> payoutAdjustmentPercentageValue) {
        long sellerBaseAmount = context.tradeAmount() + context.sellerSecurityDeposit();
        long maxTransferAmount = context.maxTransferAmountKeepingMinimum(sellerBaseAmount);
        long transferAmount = getTransferAmount(context, payoutAdjustmentPercentageValue, maxTransferAmount);
        return new PayoutAmounts(context.buyerSecurityDeposit() + transferAmount, sellerBaseAmount - transferAmount);
    }

    private static long getTransferAmount(PayoutContext context,
                                          Optional<Double> payoutAdjustmentPercentageValue,
                                          long maxTransferAmount) {
        Optional<Long> optionalRequestedRefundAmount = getRequestedRefundAmount(context.tradeAmount(), payoutAdjustmentPercentageValue);
        if (optionalRequestedRefundAmount.isEmpty()) {
            return 0;
        }
        long requestedTransferAmount = optionalRequestedRefundAmount.get();
        return Math.min(requestedTransferAmount, maxTransferAmount);
    }

    private static Optional<Long> getRequestedRefundAmount(long tradeAmount, Optional<Double> payoutAdjustmentPercentageValue) {
        return payoutAdjustmentPercentageValue
                .map(percentage -> {
                    checkArgument(percentage >= 0 && percentage <= 1,
                            "payoutAdjustmentPercentageValue must be within [0, 1]");
                    return MathUtils.roundDoubleToLong(tradeAmount * percentage);
                });
    }
}
