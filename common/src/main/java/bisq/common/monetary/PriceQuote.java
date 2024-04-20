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

package bisq.common.monetary;

import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.common.proto.PersistableProto;
import bisq.common.util.MathUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A price quote is using the concept of base currency and quote currency. Base currency is left and quote currency
 * right. Often separated by '/' or '-'. The intuitive interpretation of division due the usage of '/' is misleading.
 * The price or quote is the amount of quote currency one gets for 1 unit of the base currency. E.g. a BTC/USD price
 * of 50 000 BTC/USD means you get 50 000 USD for 1 BTC.
 * <p>
 * For the precision of the quote we use the precision of the quote currency.
 */
@EqualsAndHashCode
@Getter
@ToString
@Slf4j
public final class PriceQuote implements Comparable<PriceQuote>, PersistableProto {
    @Setter
    private static String QUOTE_SEPARATOR = "/";

    private final long value;
    private final Monetary baseSideMonetary;
    private final Monetary quoteSideMonetary;
    private final int precision;
    // For Fiat market price we show precision 2 but in trade context we show the highest precision (4 for Fiat)  
    private final int lowPrecision;
    private final Market market;

    private PriceQuote(long value, Monetary baseSideMonetary, Monetary quoteSideMonetary) {
        this.value = value;
        this.baseSideMonetary = baseSideMonetary;
        this.quoteSideMonetary = quoteSideMonetary;
        this.precision = quoteSideMonetary.precision;
        lowPrecision = quoteSideMonetary.lowPrecision;

        market = new Market(baseSideMonetary.getCode(), quoteSideMonetary.getCode(), baseSideMonetary.getName(), quoteSideMonetary.getName());
    }

    @Override
    public bisq.common.protobuf.PriceQuote toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.PriceQuote.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.PriceQuote.newBuilder().setValue(value)
                .setBaseSideMonetary(baseSideMonetary.toProto(serializeForHash))
                .setQuoteSideMonetary(quoteSideMonetary.toProto(serializeForHash));
    }

    public static PriceQuote fromProto(bisq.common.protobuf.PriceQuote proto) {
        return new PriceQuote(proto.getValue(),
                Monetary.fromProto(proto.getBaseSideMonetary()),
                Monetary.fromProto(proto.getQuoteSideMonetary()));
    }

    /**
     * @param priceValue       Price of a BTC-Fiat quote (e.g. BTC/USD). Bitcoin is base currency
     * @param fiatCurrencyCode Currency code of the fiat (quote) side
     * @return A PriceQuote object using 1 BTC as base coin.
     */
    public static PriceQuote fromFiatPrice(double priceValue, String fiatCurrencyCode) {
        return from(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(priceValue, fiatCurrencyCode));
    }

    /**
     * @param priceValue  Price of an Altcoin-BTC quote (e.g. XMR/BTC). Altcoin is base currency
     * @param altCoinCode Currency code of the altcoin (base) side
     * @return A PriceQuote object using 1 unit of the altcoin as base coin.
     */
    public static PriceQuote fromAltCoinPrice(double priceValue, String altCoinCode) {
        return from(Coin.fromFaceValue(1.0, altCoinCode), Coin.asBtcFromFaceValue(priceValue));
    }

    /**
     * @param priceValue        Price (e.g. EUR/USD). Anything can be base currency or quote currency.
     * @param baseCurrencyCode  Base currency code
     * @param quoteCurrencyCode Quote currency code
     * @return A PriceQuote object using 1 unit of the base asset.
     */
    public static PriceQuote fromPrice(double priceValue, String baseCurrencyCode, String quoteCurrencyCode) {
        Monetary baseSideMonetary = TradeCurrency.isFiat(baseCurrencyCode) ?
                Fiat.fromFaceValue(1d, baseCurrencyCode) :
                Coin.fromFaceValue(1d, baseCurrencyCode);
        Monetary quoteSideMonetary = TradeCurrency.isFiat(quoteCurrencyCode) ?
                Fiat.fromFaceValue(priceValue, quoteCurrencyCode) :
                Coin.fromFaceValue(priceValue, quoteCurrencyCode);

        return from(baseSideMonetary, quoteSideMonetary);
    }

    public static PriceQuote fromPrice(long priceValue, Market market) {
        return fromPrice(priceValue, market.getBaseCurrencyCode(), market.getQuoteCurrencyCode());
    }

    public static PriceQuote fromPrice(long priceValue, String baseCurrencyCode, String quoteCurrencyCode) {
        Monetary baseSideMonetary = TradeCurrency.isFiat(baseCurrencyCode) ?
                Fiat.fromFaceValue(1d, baseCurrencyCode) :
                Coin.fromFaceValue(1d, baseCurrencyCode);
        Monetary quoteSideMonetary = TradeCurrency.isFiat(quoteCurrencyCode) ?
                Fiat.fromValue(priceValue, quoteCurrencyCode) :
                Coin.fromValue(priceValue, quoteCurrencyCode);

        return from(baseSideMonetary, quoteSideMonetary);
    }

    /**
     * Calculates the quote from the given monetary objects
     *
     * @param baseSideMonetary  The base side monetary
     * @param quoteSideMonetary The quoteside  monetary
     * @return The PriceQuote
     */
    public static PriceQuote from(Monetary baseSideMonetary, Monetary quoteSideMonetary) {
        checkArgument(baseSideMonetary.value != 0, "baseSideMonetary.value must not be 0");
        long value = BigDecimal.valueOf(quoteSideMonetary.value)
                .movePointRight(baseSideMonetary.precision)
                .divide(BigDecimal.valueOf(baseSideMonetary.value), RoundingMode.HALF_UP)
                .longValue();
        return new PriceQuote(value, baseSideMonetary, quoteSideMonetary);
    }

    /**
     * Create a quote monetary from a given base monetary and a quote
     *
     * @param baseSideMonetary The base side monetary
     * @return The quote side monetary
     */
    public Monetary toQuoteSideMonetary(Monetary baseSideMonetary) {
        checkArgument(baseSideMonetary.getClass() == this.baseSideMonetary.getClass(),
                "baseSideMonetary must be the same type as the quote.baseSideMonetary");
        long value = BigDecimal.valueOf(baseSideMonetary.value).multiply(BigDecimal.valueOf(this.value))
                .movePointLeft(baseSideMonetary.precision)
                .longValue();
        if (quoteSideMonetary instanceof Fiat) {
            return new Fiat(value,
                    quoteSideMonetary.code,
                    quoteSideMonetary.precision);
        } else {
            return new Coin(value,
                    quoteSideMonetary.code,
                    quoteSideMonetary.precision);
        }
    }

    public Monetary toBaseSideMonetary(Monetary quoteSideMonetary) {
        checkArgument(quoteSideMonetary.getClass() == this.quoteSideMonetary.getClass(),
                "quoteSideMonetary must be the same type as the quote.quoteSideMonetary");
        long value = BigDecimal.valueOf(quoteSideMonetary.value)
                .movePointRight(baseSideMonetary.precision)
                .divide(BigDecimal.valueOf(this.value), RoundingMode.HALF_UP)
                .longValue();
        if (baseSideMonetary instanceof Fiat) {
            return new Fiat(value,
                    baseSideMonetary.code,
                    baseSideMonetary.precision);
        } else {
            return new Coin(value,
                    baseSideMonetary.code,
                    baseSideMonetary.precision);
        }
    }

    public double asDouble() {
        return asDouble(precision);
    }

    public double asDouble(int precision) {
        return MathUtils.roundDouble(BigDecimal.valueOf(value).movePointLeft(precision).doubleValue(), precision);
    }

    @Override
    public int compareTo(PriceQuote other) {
        return Long.compare(value, other.getValue());
    }
}