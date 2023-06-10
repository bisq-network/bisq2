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

package bisq.offer.amount;

import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.offer.price.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class OfferAmountFormatter {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return getBaseAmount(marketPriceService, offer, true);
    }

    public static String getBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        if (offer.hasAmountRange()) {
            return getMinMaxBaseAmount(marketPriceService, offer, showCode);
        } else {
            return getAmountOrMaxBaseAmount(marketPriceService, offer, showCode);
        }
    }

    public static String getMinBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return getMinBaseAmount(marketPriceService, offer, true);
    }

    public static String getMinBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findMinBaseAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMaxBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return getMaxBaseAmount(marketPriceService, offer, true);
    }

    public static String getMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findMaxBaseAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getAmountOrMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findAmountOrMaxBaseAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMinMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return getMinBaseAmount(marketPriceService, offer) + " - " + getMaxBaseAmount(marketPriceService, offer, showCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static String getQuoteAmount(MarketPriceService marketPriceService,
                                        AmountSpec amountSpec,
                                        PriceSpec priceSpec,
                                        Market market,
                                        boolean hasAmountRange,
                                        boolean showCode) {
        if (hasAmountRange) {
            return getMinMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, showCode);
        } else {
            return getAmountOrMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, showCode);
        }
    }

    public static String getQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        if (offer.hasAmountRange()) {
            return getMinMaxQuoteAmount(marketPriceService, offer, showCode);
        } else {
            return getAmountOrMaxQuoteAmount(marketPriceService, offer, showCode);
        }
    }

    public static String getMinQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return getMinQuoteAmount(marketPriceService, offer, true);
    }


    public static String getMinQuoteAmount(MarketPriceService marketPriceService,
                                           AmountSpec amountSpec,
                                           PriceSpec priceSpec,
                                           Market market,
                                           boolean showCode) {
        return AmountUtil.findMinQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMinQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findMinQuoteAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMaxQuoteAmount(MarketPriceService marketPriceService,
                                           AmountSpec amountSpec,
                                           PriceSpec priceSpec,
                                           Market market,
                                           boolean showCode) {
        return AmountUtil.findMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return getMaxQuoteAmount(marketPriceService, offer, true);
    }

    public static String getMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findMaxQuoteAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getAmountOrMaxQuoteAmount(MarketPriceService marketPriceService, AmountSpec amountSpec,
                                                   PriceSpec priceSpec,
                                                   Market market, boolean showCode) {
        return AmountUtil.findAmountOrMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getAmountOrMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return AmountUtil.findAmountOrMaxQuoteAmount(marketPriceService, offer).map(getFormatFunction(showCode)).orElse(Res.get("na"));
    }

    public static String getMinMaxQuoteAmount(MarketPriceService marketPriceService,
                                              AmountSpec amountSpec,
                                              PriceSpec priceSpec,
                                              Market market,
                                              boolean showCode) {
        return getMinQuoteAmount(marketPriceService, amountSpec, priceSpec, market, false) + " - " +
                getMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, showCode);
    }

    public static String getMinMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean showCode) {
        return getMinQuoteAmount(marketPriceService, offer, showCode) + " - " +
                getMaxQuoteAmount(marketPriceService, offer, showCode);
    }

    private static Function<Monetary, String> getFormatFunction(boolean showCode) {
        return showCode ? AmountFormatter::formatAmountWithCode : AmountFormatter::formatAmount;
    }
}
