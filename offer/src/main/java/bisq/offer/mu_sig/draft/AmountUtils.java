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

package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.util.MathUtils;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class for amount conversions and calculations in the offer creation workflow.
 * Provides methods for converting between input/passive amounts and slider values.
 */
class AmountUtils {
    /**
     * Extracts the input amount from a TradeAmount based on user preference.
     * The input amount is the currency that the user directly controls (e.g., enters in a text field or slider).
     *
     * @param tradeAmount                    The trade amount containing both base and quote sides
     * @param limits                         The limits to clamp the amount to
     * @param useBaseCurrencyForAmountInput If true, base currency is the input; otherwise quote currency
     * @return The clamped input amount (either base or quote side)
     */
    static Monetary toInputAmount(TradeAmount tradeAmount,
                                  TradeAmountRange limits,
                                  boolean useBaseCurrencyForAmountInput) {
        if (useBaseCurrencyForAmountInput) {
            Monetary baseSideAmount = tradeAmount.getBaseSideAmount();
            return TradeAmountLimits.clampBaseSideAmount(limits, baseSideAmount);
        } else {
            Monetary quoteSideAmount = tradeAmount.getQuoteSideAmount();
            return TradeAmountLimits.clampQuoteSideAmount(limits, quoteSideAmount);
        }
    }

    /**
     * Extracts the passive amount from a TradeAmount based on user preference.
     * The passive amount is automatically calculated from the input amount
     * using the current price quote. It is the non-editable side.
     *
     * @param tradeAmount                    The trade amount containing both base and quote sides
     * @param limits                         The limits to clamp the amount to
     * @param useBaseCurrencyForAmountInput If true, quote currency is passive; otherwise base currency
     * @return The clamped passive amount (opposite side of input amount)
     */
    static Monetary toPassiveAmount(TradeAmount tradeAmount,
                                    TradeAmountRange limits,
                                    boolean useBaseCurrencyForAmountInput) {
        if (useBaseCurrencyForAmountInput) {
            Monetary quoteSideAmount = tradeAmount.getQuoteSideAmount();
            return TradeAmountLimits.clampQuoteSideAmount(limits, quoteSideAmount);

        } else {
            Monetary baseSideAmount = tradeAmount.getBaseSideAmount();
            return TradeAmountLimits.clampBaseSideAmount(limits, baseSideAmount);
        }
    }

    /**
     * Converts a monetary input amount to a slider value between 0 and 1.
     *
     * @param inputAmount       The current input amount
     * @param inputAmountLimits The min/max limits for the input amount
     * @return A slider value between 0 and 1, where 0 represents the minimum and 1 represents the maximum
     */
    static double toSliderValue(Monetary inputAmount, MonetaryRange inputAmountLimits) {
        long min = inputAmountLimits.getMin().getValue();
        long max = inputAmountLimits.getMax().getValue();
        double diff = max - min;
        double sliderValue = diff != 0 ? (inputAmount.getValue() - min) / diff : 0;
        return MathUtils.bounded(0, 1, sliderValue);
    }

    /**
     * Converts a slider value (0-1) to a TradeAmount based on input amount limits.
     *
     * @param market            The market for the trade
     * @param priceQuote        The current price quote
     * @param inputAmountLimits The min/max limits for the input amount
     * @param inputAmount       Used to determine the currency type for the calculated amount
     * @param sliderValue       A value between 0 and 1
     * @return A TradeAmount corresponding to the slider position
     */
    static TradeAmount toTradeAmountFromSliderValue(Market market,
                                                    PriceQuote priceQuote,
                                                    MonetaryRange inputAmountLimits,
                                                    Monetary inputAmount,
                                                    double sliderValue) {
        checkArgument(sliderValue >= 0 && sliderValue <= 1, "sliderValue must be in range of 0 and 1");

        long min = inputAmountLimits.getMin().getValue();
        long max = inputAmountLimits.getMax().getValue();
        long diff = max - min;
        long sliderAmountValue = min + Math.round(sliderValue * diff);
        Monetary sliderAmount = Monetary.from(inputAmount, sliderAmountValue);
        return TradeAmountConversion.toTradeAmount(market, priceQuote, sliderAmount);
    }
}
