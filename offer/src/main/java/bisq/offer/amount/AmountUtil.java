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
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.AmountSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.oracle.marketprice.MarketPriceService;

import java.util.Optional;

/**
 * Public APIs for getting different types of amounts:
 * - Base side / quote side
 * - fixPriceAmount, minAmount, maxAmount
 * - Combinations of fallbacks for fixPriceAmount, minAmount, maxAmount
 */
public class AmountUtil {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount: If no BaseAmountSpec we calculate it from the QuoteAmountSpec with the PriceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixBaseAmount(MarketPriceService marketPriceService,
                                                       AmountSpec amountSpec,
                                                       PriceSpec priceSpec,
                                                       Market market) {
        return AmountSpecUtil.findFixBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findFixQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    public static Optional<Monetary> findMinBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMinBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMinBaseAmount(MarketPriceService marketPriceService,
                                                       AmountSpec amountSpec,
                                                       PriceSpec priceSpec,
                                                       Market market) {
        return AmountSpecUtil.findMinBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findMinQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    public static Optional<Monetary> findMaxBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMaxBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMaxBaseAmount(MarketPriceService marketPriceService,
                                                       AmountSpec amountSpec,
                                                       PriceSpec priceSpec,
                                                       Market market) {
        return AmountSpecUtil.findMaxBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findMaxQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    public static Optional<Monetary> findFixOrMaxBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixOrMaxBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixOrMaxBaseAmount(MarketPriceService marketPriceService,
                                                            AmountSpec amountSpec,
                                                            PriceSpec priceSpec,
                                                            Market market) {
        return AmountSpecUtil.findFixOrMaxBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> AmountSpecUtil.findFixOrMaxQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount: If no QuoteAmountSpec we calculate it from the BaseAmountSpec with the PriceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return AmountSpecUtil.findFixQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findFixBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findMinOrFixQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMinQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket())
                .or(() -> findFixQuoteAmount(marketPriceService, offer));
    }

    public static Optional<Monetary> findMinQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMinQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMinQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return AmountSpecUtil.findMinQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findMinBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findMaxOrFixQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMaxQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket())
                .or(() -> AmountUtil.findFixQuoteAmount(marketPriceService, offer));
    }

    public static Optional<Monetary> findMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMaxQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMaxQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return AmountSpecUtil.findMaxQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findMaxBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findFixOrMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixOrMaxQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixOrMaxQuoteAmount(MarketPriceService marketPriceService,
                                                             AmountSpec amountSpec,
                                                             PriceSpec priceSpec,
                                                             Market market) {
        return AmountSpecUtil.findFixOrMaxQuoteAmountFromSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> AmountSpecUtil.findFixOrMaxBaseAmountFromSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }
}