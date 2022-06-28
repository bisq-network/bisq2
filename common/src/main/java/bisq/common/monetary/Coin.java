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

import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.util.MathUtils;
import com.google.common.math.LongMath;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode(callSuper = true)
public class Coin extends Monetary {

    public static Coin parse(String string, String code) {
        return parse(string, code, deriveExponent(code));
    }

    public static Coin parse(String string, String code, int precision) {
        return Coin.of(new BigDecimal(string).movePointRight(precision).longValue(),
                code,
                precision);
    }

    public static Coin parseBtc(String string) {
        return Coin.of(new BigDecimal(string).movePointRight(8).longValue(),
                "BTC",
                8);
    }

    public static Coin parseXmr(String string) {
        return Coin.of(new BigDecimal(string).movePointRight(12).longValue(),
                "XMR",
                12);
    }

    public static Coin parseBsq(String string) {
        return Coin.of(new BigDecimal(string).movePointRight(12).longValue(),
                "BSQ",
                2);
    }

    // Unsafe method. We do not know the currency and the precision, just that it is not a fiat currency.
    // We exclude XMR
    public static Coin parse(String string) {
        String[] tokens = string.split(" ");
        if (tokens.length == 2) {
            String code = tokens[1];
            if (TradeCurrency.isMaybeCrypto(code)) {
                int exponent = deriveExponent(code);
                return parse(tokens[0], code, exponent);
            }
        }
        throw new IllegalArgumentException("input could not be parsed. Expected: number value + space + currency code (e.g. 234.12 USD)");
    }

    public static Coin asBtc(long value) {
        return new Coin(value, "BTC", 8);
    }

    public static Coin asBtc(double value) {
        return new Coin(value, "BTC", 8);
    }

    public static Coin asXmr(long value) {
        return new Coin(value, "XMR", 12);
    }

    public static Coin asXmr(double value) {
        return new Coin(value, "XMR", 12);
    }

    public static Coin of(long value, String code) {
        return new Coin(value, code, deriveExponent(code));
    }

    public static Coin of(double value, String code) {
        int exponent = deriveExponent(code);
        return new Coin(value, code, exponent);
    }


    public static Coin of(long value, String code, int precision) {
        return new Coin(value, code, precision);
    }

    Coin(long value, String code, int precision) {
        // We add a `c` as prefix for cryptocurrencies to avoid that we get a collusion with the code. 
        // It happened in the past that altcoins used a fiat code.

        super(code + " [crypto]", value, code, precision, code.equals("BSQ") ? 2 : 4);
    }

    private Coin(double value, String code, int precision) {
        super(code + " [crypto]", value, code, precision, precision);
    }

    private Coin(String id, long value, String code, int precision, int minPrecision) {
        super(id, value, code, precision, minPrecision);
    }

    public bisq.common.protobuf.Monetary toProto() {
        return getMonetaryBuilder().setCoin(bisq.common.protobuf.Coin.newBuilder()).build();
    }

    public static Coin fromProto(bisq.common.protobuf.Monetary baseProto) {
        return new Coin(baseProto.getId(),
                baseProto.getValue(),
                baseProto.getCode(),
                baseProto.getPrecision(),
                baseProto.getMinPrecision());
    }

    public Coin add(Coin value) {
        checkArgument(value.code.equals(this.code));
        return new Coin(LongMath.checkedAdd(this.value, value.value), this.code, this.precision);
    }

    public Coin subtract(Coin value) {
        checkArgument(value.code.equals(this.code));
        return new Coin(LongMath.checkedSubtract(this.value, value.value), this.code, this.precision);
    }

    public Coin multiply(long factor) {
        return new Coin(LongMath.checkedMultiply(this.value, factor), this.code, this.precision);
    }

    public Coin divide(long divisor) {
        return new Coin(this.value / divisor, this.code, this.precision);
    }

    @Override
    public double toDouble(long value) {
        return MathUtils.roundDouble(BigDecimal.valueOf(value).movePointLeft(precision).doubleValue(), precision);
    }

    private static int deriveExponent(String code) {
        if (code.equals("XMR")) return 12;
        if (code.equals("BSQ")) return 2;
        return 8;
    }

    public Coin round(int roundPrecision) {
        //todo not tested
        double rounded = MathUtils.roundDouble(toDouble(value), roundPrecision);
        long shifted = BigDecimal.valueOf(rounded).movePointRight(precision).longValue();
        return Coin.of(shifted, code, precision);
    }

    @Override
    public String getName() {
        return CryptoCurrencyRepository.getName(code).orElse(code);
    }

    @Override
    public String toString() {
        return "Coin{" +
                "\r\n} " + super.toString();
    }
}