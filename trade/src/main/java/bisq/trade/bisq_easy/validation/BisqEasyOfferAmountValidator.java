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

import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;

import static com.google.common.base.Preconditions.checkArgument;

public final class BisqEasyOfferAmountValidator {
    private BisqEasyOfferAmountValidator() {
    }

    public static void validateOfferAmount(BisqEasyOffer offer, long baseSideAmount, long quoteSideAmount) {
        validateOfferAmount(offer.getAmountSpec(), baseSideAmount, quoteSideAmount);
    }

    static void validateOfferAmount(AmountSpec amountSpec, long baseSideAmount, long quoteSideAmount) {
        checkArgument(baseSideAmount > 0, "Base side amount must be positive");
        checkArgument(quoteSideAmount > 0, "Quote side amount must be positive");
        if (amountSpec instanceof BaseSideFixedAmountSpec fixedAmountSpec) {
            checkArgument(baseSideAmount == fixedAmountSpec.getAmount(),
                    "Base side amount must match the offer fixed amount. baseSideAmount=%s; fixedAmount=%s",
                    baseSideAmount, fixedAmountSpec.getAmount());
        } else if (amountSpec instanceof BaseSideRangeAmountSpec rangeAmountSpec) {
            checkArgument(baseSideAmount >= rangeAmountSpec.getMinAmount(),
                    "Base side amount must not be below the offer minimum amount. baseSideAmount=%s; minAmount=%s",
                    baseSideAmount, rangeAmountSpec.getMinAmount());
            checkArgument(baseSideAmount <= rangeAmountSpec.getMaxAmount(),
                    "Base side amount must not be above the offer maximum amount. baseSideAmount=%s; maxAmount=%s",
                    baseSideAmount, rangeAmountSpec.getMaxAmount());
        } else if (amountSpec instanceof QuoteSideFixedAmountSpec fixedAmountSpec) {
            checkArgument(quoteSideAmount == fixedAmountSpec.getAmount(),
                    "Quote side amount must match the offer fixed amount. quoteSideAmount=%s; fixedAmount=%s",
                    quoteSideAmount, fixedAmountSpec.getAmount());
        } else if (amountSpec instanceof QuoteSideRangeAmountSpec rangeAmountSpec) {
            checkArgument(quoteSideAmount >= rangeAmountSpec.getMinAmount(),
                    "Quote side amount must not be below the offer minimum amount. quoteSideAmount=%s; minAmount=%s",
                    quoteSideAmount, rangeAmountSpec.getMinAmount());
            checkArgument(quoteSideAmount <= rangeAmountSpec.getMaxAmount(),
                    "Quote side amount must not be above the offer maximum amount. quoteSideAmount=%s; maxAmount=%s",
                    quoteSideAmount, rangeAmountSpec.getMaxAmount());
        } else {
            throw new IllegalArgumentException("Unsupported amountSpec: " + amountSpec);
        }
    }
}
