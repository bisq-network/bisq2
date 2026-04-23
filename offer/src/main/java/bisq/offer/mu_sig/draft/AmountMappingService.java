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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maps between domain trade amounts and the different UI-facing amount representations.
 * <p>
 * Design: this service keeps conversion and clamping logic in one place so workflow/state
 * orchestration can stay focused on transition ordering.
 */
class AmountMappingService {

    /* --------------------------------------------------------------------- */
    // UI mapping (trade amount -> input/passive/slider representations)
    /* --------------------------------------------------------------------- */

    Monetary toInputAmount(TradeAmount tradeAmount,
                           TradeAmountRange limits,
                           boolean useBaseCurrencyForAmountInput) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkNotNull(limits, "limits must not be null");
        return AmountUtils.toInputAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }

    Monetary toPassiveAmount(TradeAmount tradeAmount,
                             TradeAmountRange limits,
                             boolean useBaseCurrencyForAmountInput) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkNotNull(limits, "limits must not be null");
        return AmountUtils.toPassiveAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
    }

    MonetaryRange toInputAmountLimits(TradeAmountRange tradeAmountLimits,
                                      boolean useBaseCurrencyForAmountInput) {
        checkNotNull(tradeAmountLimits, "tradeAmountLimits must not be null");
        TradeAmount min = tradeAmountLimits.getMin();
        TradeAmount max = tradeAmountLimits.getMax();
        Monetary minInputAmount = toInputAmount(min, tradeAmountLimits, useBaseCurrencyForAmountInput);
        Monetary maxInputAmount = toInputAmount(max, tradeAmountLimits, useBaseCurrencyForAmountInput);
        return new MonetaryRange(minInputAmount, maxInputAmount);
    }

    double toSliderValue(TradeAmount tradeAmount,
                         TradeAmountRange limits,
                         MonetaryRange inputAmountLimits,
                         boolean useBaseCurrencyForAmountInput) {
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkNotNull(limits, "limits must not be null");
        checkNotNull(inputAmountLimits, "inputAmountLimits must not be null");

        Monetary inputAmount = toInputAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
        return AmountUtils.toSliderValue(inputAmount, inputAmountLimits);
    }

    /* --------------------------------------------------------------------- */
    // Domain conversion (input/slider/passive representations -> trade amount)
    /* --------------------------------------------------------------------- */

    TradeAmount toTradeAmountFromInputAmount(Market market,
                                             PriceQuote priceQuote,
                                             Monetary inputAmount,
                                             TradeAmountRange clampLimits) {
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(inputAmount, "inputAmount must not be null");
        checkNotNull(clampLimits, "clampLimits must not be null");

        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
        return TradeAmountLimits.clampTradeAmount(clampLimits, tradeAmount);
    }

    TradeAmount toTradeAmountFromSliderValue(Market market,
                                             PriceQuote priceQuote,
                                             TradeAmount tradeAmount,
                                             TradeAmountRange limits,
                                             MonetaryRange inputAmountLimits,
                                             boolean useBaseCurrencyForAmountInput,
                                             double sliderValue) {
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkNotNull(limits, "limits must not be null");
        checkNotNull(inputAmountLimits, "inputAmountLimits must not be null");

        Monetary inputAmount = toInputAmount(tradeAmount, limits, useBaseCurrencyForAmountInput);
        TradeAmount fromSliderValue = AmountUtils.toTradeAmountFromSliderValue(market,
                priceQuote,
                inputAmountLimits,
                inputAmount,
                sliderValue);
        return TradeAmountLimits.clampTradeAmount(limits, fromSliderValue);
    }

    TradeAmount toUpdatedPassiveAmount(Market market,
                                       PriceQuote priceQuote,
                                       TradeAmount tradeAmount,
                                       TradeAmountRange oldLimits,
                                       TradeAmountRange newLimits,
                                       boolean useBaseCurrencyForAmountInput) {
        checkNotNull(market, "market must not be null");
        checkNotNull(priceQuote, "priceQuote must not be null");
        checkNotNull(tradeAmount, "tradeAmount must not be null");
        checkNotNull(oldLimits, "oldLimits must not be null");
        checkNotNull(newLimits, "newLimits must not be null");

        Monetary inputAmount = toInputAmount(tradeAmount, oldLimits, useBaseCurrencyForAmountInput);
        TradeAmount converted = TradeAmountConversion.toTradeAmount(market, priceQuote, inputAmount);
        return TradeAmountLimits.clampTradeAmount(newLimits, converted);
    }
}
