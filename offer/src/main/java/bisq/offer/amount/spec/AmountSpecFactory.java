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

import bisq.common.monetary.TradeAmount;

/**
 * Util for getting the AmountSpec implementation and amounts from the AmountSpec.
 * Should not be used by client code directly but rather an util for amount package internal use.
 * AmountUtil and OfferAmountFormatter exposes public APIs.
 */
public class AmountSpecFactory {
    public static AmountSpec createAmountSpec(boolean isBtcFiatMarket,
                                              boolean useRangeAmount,
                                              TradeAmount minTradeAmount,
                                              TradeAmount maxTradeAmount,
                                              TradeAmount fixTradeAmount) {
        if (isBtcFiatMarket) {
            return createBaseSideAmountSpec(useRangeAmount,
                    minTradeAmount,
                    maxTradeAmount,
                    fixTradeAmount);
        } else {
            return createQuoteSideAmountSpec(useRangeAmount,
                    minTradeAmount,
                    maxTradeAmount,
                    fixTradeAmount);
        }
    }

    public static BaseSideAmountSpec createBaseSideAmountSpec(boolean useRangeAmount,
                                                              TradeAmount minTradeAmount,
                                                              TradeAmount maxTradeAmount,
                                                              TradeAmount fixTradeAmount) {
        if (useRangeAmount) {
            long minAmount = minTradeAmount.getBaseSideAmount().getValue();
            long maxAmount = maxTradeAmount.getBaseSideAmount().getValue();
            return new BaseSideRangeAmountSpec(minAmount, maxAmount);
        } else {
            long fixAmount = fixTradeAmount.getBaseSideAmount().getValue();
            return new BaseSideFixedAmountSpec(fixAmount);
        }
    }

    public static QuoteSideAmountSpec createQuoteSideAmountSpec(boolean useRangeAmount,
                                                                TradeAmount minTradeAmount,
                                                                TradeAmount maxTradeAmount,
                                                                TradeAmount fixTradeAmount) {
        if (useRangeAmount) {
            long minAmount = minTradeAmount.getQuoteSideAmount().getValue();
            long maxAmount = maxTradeAmount.getQuoteSideAmount().getValue();
            return new QuoteSideRangeAmountSpec(minAmount, maxAmount);
        } else {
            long fixAmount = fixTradeAmount.getQuoteSideAmount().getValue();
            return new QuoteSideFixedAmountSpec(fixAmount);
        }
    }
}