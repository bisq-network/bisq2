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
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.AmountFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class OfferAmountFormatter {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static String formatBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return formatBaseAmount(marketPriceService, offer, true);
    }

    public static String formatBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        if (offer.hasAmountRange()) {
            return formatMinMaxBaseAmount(marketPriceService, offer, withCode);
        } else {
            return formatFixOrMaxBaseAmount(marketPriceService, offer, withCode);
        }
    }

    public static String formatMinBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return formatMinBaseAmount(marketPriceService, offer, true);
    }

    public static String formatMinBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findMinBaseAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMaxBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return formatMaxBaseAmount(marketPriceService, offer, true);
    }

    public static String formatMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findMaxBaseAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatFixOrMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findFixOrMaxBaseAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMinMaxBaseAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return formatMinBaseAmount(marketPriceService, offer) + " - " +
                formatMaxBaseAmount(marketPriceService, offer, withCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static String formatQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return formatQuoteAmount(marketPriceService,
                offer.getAmountSpec(),
                offer.getPriceSpec(),
                offer.getMarket(),
                offer.hasAmountRange(),
                withCode);
    }

    public static String formatQuoteAmount(MarketPriceService marketPriceService,
                                           AmountSpec amountSpec,
                                           PriceSpec priceSpec,
                                           Market market,
                                           boolean hasAmountRange,
                                           boolean withCode) {
        if (hasAmountRange) {
            return formatMinMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        } else {
            return formatFixOrMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        }
    }

    public static String formatMinQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return formatMinQuoteAmount(marketPriceService, offer, true);
    }


    public static String formatMinQuoteAmount(MarketPriceService marketPriceService,
                                              AmountSpec amountSpec,
                                              PriceSpec priceSpec,
                                              Market market,
                                              boolean withCode) {
        return AmountUtil.findMinQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMinQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findMinQuoteAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMaxQuoteAmount(MarketPriceService marketPriceService,
                                              AmountSpec amountSpec,
                                              PriceSpec priceSpec,
                                              Market market,
                                              boolean withCode) {
        return AmountUtil.findMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return formatMaxQuoteAmount(marketPriceService, offer, true);
    }

    public static String formatMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findMaxQuoteAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatFixOrMaxQuoteAmount(MarketPriceService marketPriceService, AmountSpec amountSpec,
                                                   PriceSpec priceSpec,
                                                   Market market, boolean withCode) {
        return AmountUtil.findFixOrMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatFixOrMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return AmountUtil.findFixOrMaxQuoteAmount(marketPriceService, offer).map(getFormatFunction(withCode)).orElse(Res.get("na"));
    }

    public static String formatMinMaxQuoteAmount(MarketPriceService marketPriceService,
                                                 AmountSpec amountSpec,
                                                 PriceSpec priceSpec,
                                                 Market market,
                                                 boolean withCode) {
        return formatMinQuoteAmount(marketPriceService, amountSpec, priceSpec, market, false) + " - " +
                formatMaxQuoteAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
    }

    public static String formatMinMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer, boolean withCode) {
        return formatMinQuoteAmount(marketPriceService, offer, withCode) + " - " +
                formatMaxQuoteAmount(marketPriceService, offer, withCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static Function<Monetary, String> getFormatFunction(boolean withCode) {
        return withCode ? AmountFormatter::formatAmountWithCode : AmountFormatter::formatAmount;
    }
}
