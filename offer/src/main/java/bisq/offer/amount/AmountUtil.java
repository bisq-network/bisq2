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
import bisq.offer.price.PriceSpec;
import bisq.offer.price.PriceUtil;
import bisq.oracle.marketprice.MarketPriceService;

import java.util.Optional;

public class AmountUtil {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount: The quote side amount gets calculated by using the given priceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixBaseAmount(MarketPriceService marketPriceService,
                                                       AmountSpec amountSpec,
                                                       PriceSpec priceSpec,
                                                       Market market) {
        return findAmountFromFixBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> findAmountFromFixQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
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
        return findMinAmountFromMinMaxBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> findMinAmountFromMinMaxQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
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
        return findMaxAmountFromMinMaxBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> findMaxAmountFromMinMaxQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }

    public static Optional<Monetary> findAmountOrMaxBaseAmount(MarketPriceService marketPriceService, Offer offer) {
        return findAmountOrMaxBaseAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findAmountOrMaxBaseAmount(MarketPriceService marketPriceService,
                                                               AmountSpec amountSpec,
                                                               PriceSpec priceSpec,
                                                               Market market) {
        return findAmountOrMaxAmountFromBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                .or(() -> findAmountOrMaxAmountFromQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                        .flatMap(quoteAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toBaseMonetary(quoteAmount))
                        ));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount: The base side amount gets calculated by using the given priceSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findFixQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findFixQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return findAmountFromFixQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> findAmountFromFixBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }


    public static Optional<Monetary> findMinQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMinQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMinQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return findMinAmountFromMinMaxQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> findMinAmountFromMinMaxBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findMaxQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findMaxQuoteAmount(MarketPriceService marketPriceService,
                                                        AmountSpec amountSpec,
                                                        PriceSpec priceSpec,
                                                        Market market) {
        return findMaxAmountFromMinMaxQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> findMaxAmountFromMinMaxBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }

    public static Optional<Monetary> findAmountOrMaxQuoteAmount(MarketPriceService marketPriceService, Offer offer) {
        return findAmountOrMaxQuoteAmount(marketPriceService, offer.getAmountSpec(), offer.getPriceSpec(), offer.getMarket());
    }

    public static Optional<Monetary> findAmountOrMaxQuoteAmount(MarketPriceService marketPriceService,
                                                                AmountSpec amountSpec,
                                                                PriceSpec priceSpec,
                                                                Market market) {
        return findAmountOrMaxAmountFromQuoteAmountSpec(amountSpec, market.getQuoteCurrencyCode())
                .or(() -> findAmountOrMaxAmountFromBaseAmountSpec(amountSpec, market.getBaseCurrencyCode())
                        .flatMap(baseAmount -> PriceUtil.findQuote(marketPriceService, priceSpec, market)
                                .map(quote -> quote.toQuoteMonetary(baseAmount))
                        ));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private utils for BaseAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static Optional<Monetary> findAmountFromFixBaseAmountSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findFixBaseAmountSpec(amountSpec)
                .map(FixBaseAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    private static Optional<Monetary> findMinAmountFromMinMaxBaseAmountSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findMinMaxBaseAmountSpec(amountSpec)
                .map(MinMaxBaseAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    private static Optional<Monetary> findMaxAmountFromMinMaxBaseAmountSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findMinMaxBaseAmountSpec(amountSpec)
                .map(MinMaxBaseAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    private static Optional<Monetary> findAmountOrMaxAmountFromBaseAmountSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findAmountFromFixBaseAmountSpec(amountSpec, baseCurrencyCode)
                .or(() -> findMaxAmountFromMinMaxBaseAmountSpec(amountSpec, baseCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private utils for QuoteAmount
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static Optional<Monetary> findAmountFromFixQuoteAmountSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findFixQuoteAmountSpec(amountSpec)
                .map(FixQuoteAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    private static Optional<Monetary> findMinAmountFromMinMaxQuoteAmountSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findMinMaxQuoteAmountSpec(amountSpec)
                .map(MinMaxQuoteAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    private static Optional<Monetary> findMaxAmountFromMinMaxQuoteAmountSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findMinMaxQuoteAmountSpec(amountSpec)
                .map(MinMaxQuoteAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    private static Optional<Monetary> findAmountOrMaxAmountFromQuoteAmountSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findAmountFromFixQuoteAmountSpec(amountSpec, quoteCurrencyCode)
                .or(() -> findMaxAmountFromMinMaxQuoteAmountSpec(amountSpec, quoteCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utils for AmountSpec
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static Optional<FixBaseAmountSpec> findFixBaseAmountSpec(AmountSpec amountSpec) {
        if (amountSpec instanceof FixBaseAmountSpec) {
            return Optional.of((FixBaseAmountSpec) amountSpec);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<MinMaxBaseAmountSpec> findMinMaxBaseAmountSpec(AmountSpec amountSpec) {
        if (amountSpec instanceof MinMaxBaseAmountSpec) {
            return Optional.of(((MinMaxBaseAmountSpec) amountSpec));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<FixQuoteAmountSpec> findFixQuoteAmountSpec(AmountSpec amountSpec) {
        if (amountSpec instanceof FixQuoteAmountSpec) {
            return Optional.of((FixQuoteAmountSpec) amountSpec);
        } else {
            return Optional.empty();
        }
    }

    private static Optional<MinMaxQuoteAmountSpec> findMinMaxQuoteAmountSpec(AmountSpec amountSpec) {
        if (amountSpec instanceof MinMaxQuoteAmountSpec) {
            return Optional.of(((MinMaxQuoteAmountSpec) amountSpec));
        } else {
            return Optional.empty();
        }
    }
}