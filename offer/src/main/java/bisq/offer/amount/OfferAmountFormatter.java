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

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.i18n.Res;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class OfferAmountFormatter {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Either min-max or fixed
    public static String formatBaseAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), offer.hasAmountRange(), true);
    }

    public static String formatBaseAmount(MarketPriceService marketPriceService, Offer<?, ?> offer, boolean withCode) {
        return formatBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), offer.hasAmountRange(), withCode);
    }

    public static String formatBaseAmount(MarketPriceService marketPriceService,
                                          AmountSpec amountSpec,
                                          PriceSpec priceSpec,
                                          Market market,
                                          boolean hasAmountRange,
                                          boolean withCode) {
        if (hasAmountRange) {
            return formatBaseSideRangeAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        } else {
            return formatBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        }
    }

    // Fixed
    public static String formatBaseSideFixedAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatBaseSideFixedAmount(marketPriceService, offer, true);
    }

    public static String formatBaseSideFixedAmount(MarketPriceService marketPriceService,
                                                   Offer<?, ?> offer,
                                                   boolean withCode) {
        return formatBaseSideFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatBaseSideFixedAmount(MarketPriceService marketPriceService,
                                                   AmountSpec amountSpec,
                                                   PriceSpec priceSpec,
                                                   Market market,
                                                   boolean withCode) {
        return OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Min
    public static String formatBaseSideMinAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatBaseSideMinAmount(marketPriceService, offer, true);
    }

    public static String formatBaseSideMinAmount(MarketPriceService marketPriceService,
                                                 Offer<?, ?> offer,
                                                 boolean withCode) {
        return formatBaseSideMinAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatBaseSideMinAmount(MarketPriceService marketPriceService,
                                                 AmountSpec amountSpec,
                                                 PriceSpec priceSpec,
                                                 Market market,
                                                 boolean withCode) {
        return OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Max
    public static String formatBaseSideMaxAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatBaseSideMaxAmount(marketPriceService, offer, true);
    }

    public static String formatBaseSideMaxAmount(MarketPriceService marketPriceService,
                                                 Offer<?, ?> offer,
                                                 boolean withCode) {
        return formatBaseSideMaxAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatBaseSideMaxAmount(MarketPriceService marketPriceService,
                                                 AmountSpec amountSpec,
                                                 PriceSpec priceSpec,
                                                 Market market,
                                                 boolean withCode) {
        return OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Max or fixed
    public static String formatBaseSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                        Offer<?, ?> offer,
                                                        boolean withCode) {
        return formatBaseSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatBaseSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market,
                                                        boolean withCode) {
        return OfferAmountUtil.findBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Range (Min - Max)
    public static String formatBaseSideRangeAmount(MarketPriceService marketPriceService,
                                                   Offer<?, ?> offer,
                                                   boolean withCode) {
        return formatBaseSideRangeAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatBaseSideRangeAmount(MarketPriceService marketPriceService,
                                                   AmountSpec amountSpec,
                                                   PriceSpec priceSpec,
                                                   Market market,
                                                   boolean withCode) {
        return formatBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market, false) + " - " +
                formatBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Either min-max or fixed
    public static String formatQuoteAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), offer.hasAmountRange(), true);
    }

    public static String formatQuoteAmount(MarketPriceService marketPriceService, Offer<?, ?> offer, boolean withCode) {
        return formatQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), offer.hasAmountRange(), withCode);
    }

    public static String formatQuoteAmount(MarketPriceService marketPriceService,
                                           AmountSpec amountSpec,
                                           PriceSpec priceSpec,
                                           Market market,
                                           boolean withCode) {
        return formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, amountSpec instanceof RangeAmountSpec, withCode);
    }

    public static String formatQuoteAmount(MarketPriceService marketPriceService,
                                           AmountSpec amountSpec,
                                           PriceSpec priceSpec,
                                           Market market,
                                           boolean hasAmountRange,
                                           boolean withCode) {
        if (hasAmountRange) {
            return formatQuoteSideRangeAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        } else {
            return formatQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
        }
    }

    // Fixed
    public static String formatQuoteSideFixedAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuoteSideFixedAmount(marketPriceService, offer, true);
    }

    public static String formatQuoteSideFixedAmount(MarketPriceService marketPriceService,
                                                    Offer<?, ?> offer,
                                                    boolean withCode) {
        return formatQuoteSideFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideFixedAmount(MarketPriceService marketPriceService,
                                                    AmountSpec amountSpec,
                                                    PriceSpec priceSpec,
                                                    Market market,
                                                    boolean withCode) {
        return OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Min
    public static String formatQuoteSideMinAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuoteSideMinAmount(marketPriceService, offer, true);
    }

    public static String formatQuoteSideMinAmount(MarketPriceService marketPriceService,
                                                  Offer<?, ?> offer,
                                                  boolean withCode) {
        return formatQuoteSideMinAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideMinAmount(MarketPriceService marketPriceService,
                                                  AmountSpec amountSpec,
                                                  PriceSpec priceSpec,
                                                  Market market,
                                                  boolean withCode) {
        return OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Min or fixed
    public static String formatQuoteSideMinOrFixedAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuoteSideMinOrFixedAmount(marketPriceService, offer, true);
    }

    public static String formatQuoteSideMinOrFixedAmount(MarketPriceService marketPriceService,
                                                         Offer<?, ?> offer,
                                                         boolean withCode) {
        return formatQuoteSideMinOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideMinOrFixedAmount(MarketPriceService marketPriceService,
                                                         AmountSpec amountSpec,
                                                         PriceSpec priceSpec,
                                                         Market market,
                                                         boolean withCode) {
        return OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Max
    public static String formatQuoteSideMaxAmount(MarketPriceService marketPriceService, Offer<?, ?> offer) {
        return formatQuoteSideMaxAmount(marketPriceService, offer, true);
    }

    public static String formatQuoteSideMaxAmount(MarketPriceService marketPriceService,
                                                  Offer<?, ?> offer,
                                                  boolean withCode) {
        return formatQuoteSideMaxAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideMaxAmount(MarketPriceService marketPriceService,
                                                  AmountSpec amountSpec,
                                                  PriceSpec priceSpec,
                                                  Market market,
                                                  boolean withCode) {
        return OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Max or fixed
    public static String formatQuoteSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                         Offer<?, ?> offer,
                                                         boolean withCode) {
        return formatQuoteSideMaxOrFixedAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideMaxOrFixedAmount(MarketPriceService marketPriceService,
                                                         AmountSpec amountSpec,
                                                         PriceSpec priceSpec,
                                                         Market market,
                                                         boolean withCode) {
        return OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, priceSpec, market).map(getFormatFunction(withCode)).orElse(Res.get("data.na"));
    }

    // Range (Min - Max)
    public static String formatQuoteSideRangeAmount(MarketPriceService marketPriceService,
                                                    Offer<?, ?> offer,
                                                    boolean withCode) {
        return formatQuoteSideRangeAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket(), withCode);
    }

    public static String formatQuoteSideRangeAmount(MarketPriceService marketPriceService,
                                                    AmountSpec amountSpec,
                                                    PriceSpec priceSpec,
                                                    Market market,
                                                    boolean withCode) {
        return formatQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market, false) + " - " +
                formatQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market, withCode);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static Function<Monetary, String> getFormatFunction(boolean withCode) {
        return withCode ? AmountFormatter::formatAmountWithCode : AmountFormatter::formatAmount;
    }
}
