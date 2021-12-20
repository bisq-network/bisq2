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

package network.misq.common.monetary;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.currency.MisqCurrency;
import network.misq.common.util.MathUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A price quote is using the concept of base currency and quote currency. Base currency is left and quote currency
 * right. Often separated by '/' or '-'. The intuitive interpretation of division due the usage of '/' is misleading.
 * The price or quote is the amount of quote currency one gets for 1 unit of the base currency. E.g. a BTC/USD price
 * of 50 000 BTC/USD means you get 50 000 USD for 1 BTC.
 * <p>
 * For the smallestUnitExponent of the quote we use the smallestUnitExponent of the quote currency.
 */
@EqualsAndHashCode
@Getter
@Slf4j
public class Quote implements Comparable<Quote> {
    @Setter
    private String QUOTE_SEPARATOR = "/";
    private final long value;
    private final Monetary baseMonetary;
    private final Monetary quoteMonetary;
    private final int smallestUnitExponent;

    private Quote(long value, Monetary baseMonetary, Monetary quoteMonetary) {
        this.value = value;
        this.baseMonetary = baseMonetary;
        this.quoteMonetary = quoteMonetary;
        this.smallestUnitExponent = quoteMonetary.smallestUnitExponent;
    }

    /**
     * @param price            Price of a BTC-Fiat quote (e.g. BTC/USD). Bitcoin is base currency
     * @param fiatCurrencyCode Currency code of the fiat (quote) side
     * @return A quote object using 1 BTC as base coin.
     */
    public static Quote fromFiatPrice(double price, String fiatCurrencyCode) {
        return Quote.of(Coin.asBtc(1.0), Fiat.of(price, fiatCurrencyCode));
    }

    /**
     * @param price       Price of an Altcoin-BTC quote (e.g. XMR/BTC). Altcoin is base currency
     * @param altCoinCode Currency code of the altcoin (base) side
     * @return A quote object using 1 unit of the altcoin as base coin.
     */
    public static Quote fromAltCoinPrice(double price, String altCoinCode) {
        return Quote.of(Coin.of(1.0, altCoinCode), Coin.asBtc(price));
    }

    /**
     * @param price             Price (e.g. EUR/USD). Anything can be base currency or quote currency.
     * @param baseCurrencyCode  Base currency code
     * @param quoteCurrencyCode Quote currency code
     * @return A quote object using 1 unit of the base asset.
     */
    public static Quote fromPrice(double price, String baseCurrencyCode, String quoteCurrencyCode) {
        Monetary baseMonetary = MisqCurrency.isFiat(baseCurrencyCode) ?
                Fiat.of(1d, baseCurrencyCode) :
                Coin.of(1d, baseCurrencyCode);
        Monetary quoteMonetary = MisqCurrency.isFiat(quoteCurrencyCode) ?
                Fiat.of(price, quoteCurrencyCode) :
                Coin.of(price, quoteCurrencyCode);

        return Quote.of(baseMonetary, quoteMonetary);
    }

    /**
     * Calculates the quote from the given monetary objects
     *
     * @param baseMonetary  The base monetary
     * @param quoteMonetary The quote monetary
     * @return The quote
     */
    public static Quote of(Monetary baseMonetary, Monetary quoteMonetary) {
        checkArgument(baseMonetary.value != 0, "baseMonetary.value must not be 0");
        long value = BigDecimal.valueOf(quoteMonetary.value)
                .movePointRight(baseMonetary.smallestUnitExponent)
                .divide(BigDecimal.valueOf(baseMonetary.value), RoundingMode.HALF_UP)
                .longValue();
        return new Quote(value, baseMonetary, quoteMonetary);
    }

    /**
     * A quote created from a market price quote and a percentage offset
     *
     * @param marketPrice Current market price
     * @param offset      Offset from market price in percent (values: -1 to +1).
     * @return The quote representing the offset from market price
     */
    public static Quote fromMarketPriceOffset(Quote marketPrice, double offset) {
        checkArgument(offset >= -1 && offset <= 1, "Offset must be in range -1 to +1");
        double price = marketPrice.asDouble() * (1 + offset);
        return Quote.fromPrice(price, marketPrice.baseMonetary.currencyCode, marketPrice.quoteMonetary.currencyCode);
    }

    /**
     * @param marketQuote The quote representing the market price
     * @param offerQuote  The quote we want to compare to the market price
     * @return The percentage offset from the market price. Positive value means that offerQuote is above market price.
     * Result is rounded to precision 4 (2 decimal places at percentage representation)
     */
    public static double offsetOf(Quote marketQuote, Quote offerQuote) {
        checkArgument(marketQuote.value > 0, "marketQuote must be positive");
        return MathUtils.roundDouble(offerQuote.value / (double) marketQuote.value - 1, 4);
    }

    /**
     * Create a quote monetary from a given base monetary and a quote
     *
     * @param baseMonetary The base monetary
     * @param quote        The quote
     * @return The quote monetary
     */
    public static Monetary toQuoteMonetary(Monetary baseMonetary, Quote quote) {
        checkArgument(baseMonetary.getClass() == quote.baseMonetary.getClass(),
                "baseMonetary must be the same type as the quote.baseMonetary");
        Monetary quoteMonetary = quote.quoteMonetary;
        long value = BigDecimal.valueOf(baseMonetary.value).multiply(BigDecimal.valueOf(quote.value))
                .movePointLeft(baseMonetary.smallestUnitExponent)
                .longValue();
        if (quoteMonetary instanceof Fiat) {
            return new Fiat(value,
                    quoteMonetary.currencyCode,
                    quoteMonetary.smallestUnitExponent);
        } else {
            return new Coin(value,
                    quoteMonetary.currencyCode,
                    quoteMonetary.smallestUnitExponent);
        }
    }

    public double asDouble() {
        return asDouble(smallestUnitExponent);
    }

    public double asDouble(int precision) {
        return MathUtils.roundDouble(BigDecimal.valueOf(value).movePointLeft(smallestUnitExponent).doubleValue(), precision);
    }

    public String getQuoteCode() {
        return baseMonetary.currencyCode + QUOTE_SEPARATOR + quoteMonetary.currencyCode;
    }

    @Override
    public int compareTo(Quote o) {
        return Long.compare(value, o.getValue());
    }

    @Override
    public String toString() {
        return "Quote{" +
                "\r\n     value=" + value +
                ",\r\n     baseMonetary=" + baseMonetary +
                ",\r\n     quoteMonetary=" + quoteMonetary +
                ",\r\n     smallestUnitExponent=" + smallestUnitExponent +
                "\r\n}";
    }
}