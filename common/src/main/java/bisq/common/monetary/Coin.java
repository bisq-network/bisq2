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
public final class Coin extends Monetary {

    public static Coin parse(String string, String code) {
        return parse(string, code, derivePrecision(code));
    }

    public static Coin parse(String string, String code, int precision) {
        return Coin.fromValue(new BigDecimal(string).movePointRight(precision).longValue(),
                code,
                precision);
    }

    public static Coin parseBtc(String string) {
        return Coin.fromValue(new BigDecimal(string).movePointRight(8).longValue(),
                "BTC",
                8);
    }

    public static Coin parseXmr(String string) {
        return Coin.fromValue(new BigDecimal(string).movePointRight(12).longValue(),
                "XMR",
                12);
    }

    public static Coin parseBsq(String string) {
        return Coin.fromValue(new BigDecimal(string).movePointRight(12).longValue(),
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
                int precision = derivePrecision(code);
                return parse(tokens[0], code, precision);
            }
        }
        throw new IllegalArgumentException("input could not be parsed. Expected: number value + space + currency code (e.g. 234.12 USD)");
    }

    /**
     * @param value Value as smallest unit the Coin object can represent.
     */
    public static Coin asBtcFromValue(long value) {
        return new Coin(value, "BTC", 8);
    }

    /**
     * @param faceValue Coin value as face value. E.g. 1.12345678 BTC
     */
    public static Coin asBtcFromFaceValue(double faceValue) {
        return new Coin(faceValue, "BTC", 8);
    }

    /**
     * @param value Value as smallest unit the Coin object can represent.
     */
    public static Coin asBsqFromValue(long value) {
        return new Coin(value, "BSQ", 2);
    }

    /**
     * @param faceValue Coin value as face value. E.g. 1.123456789012 XMR
     */
    public static Coin asBsqFromFaceValue(double faceValue) {
        return new Coin(faceValue, "BSQ", 2);
    }


    /**
     * @param value Value as smallest unit the Coin object can represent.
     */
    public static Coin asXmrFromValue(long value) {
        return new Coin(value, "XMR", 12);
    }

    /**
     * @param faceValue Coin value as face value. E.g. 1.123456789012 XMR
     */
    public static Coin asXmrFromFaceValue(double faceValue) {
        return new Coin(faceValue, "XMR", 12);
    }

    /**
     * @param value Value as smallest unit the Coin object can represent.
     */
    public static Coin fromValue(long value, String code) {
        return new Coin(value, code, derivePrecision(code));
    }

    /**
     * @param faceValue Coin value as face value. E.g. 1.12345678 BTC
     */
    public static Coin fromFaceValue(double faceValue, String code) {
        int precision = derivePrecision(code);
        return new Coin(faceValue, code, precision);
    }

    /**
     * @param value Value as smallest unit the Coin object can represent.
     */
    public static Coin fromValue(long value, String code, int precision) {
        return new Coin(value, code, precision);
    }

    Coin(long value, String code, int precision) {
        // We add a `c` as prefix for cryptocurrencies to avoid that we get a collusion with the code. 
        // It happened in the past that altcoins used a fiat code.

        super(code + " [crypto]", value, code, precision, code.equals("BSQ") ? 2 : 4);
    }

    /**
     * @param faceValue Coin value as face value. E.g. 1.12345678 BTC
     */
    private Coin(double faceValue, String code, int precision) {
        super(code + " [crypto]", faceValue, code, precision, code.equals("BSQ") ? 2 : 4);
    }

    private Coin(String id, long value, String code, int precision, int lowPrecision) {
        super(id, value, code, precision, lowPrecision);
    }

    @Override
    public bisq.common.protobuf.Monetary toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.Monetary.Builder getBuilder(boolean serializeForHash) {
        return getMonetaryBuilder().setCoin(bisq.common.protobuf.Coin.newBuilder());
    }

    public static Coin fromProto(bisq.common.protobuf.Monetary baseProto) {
        return new Coin(baseProto.getId(),
                baseProto.getValue(),
                baseProto.getCode(),
                baseProto.getPrecision(),
                baseProto.getLowPrecision());
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

    private static int derivePrecision(String code) {
        if (code.equals("XMR")) return 12;
        if (code.equals("BSQ")) return 2;
        return 8;
    }

    public Coin round(int roundPrecision) {
        //todo (low prio) add tests
        double rounded = MathUtils.roundDouble(toDouble(value), roundPrecision);
        long shifted = BigDecimal.valueOf(rounded).movePointRight(precision).longValue();
        return Coin.fromValue(shifted, code, precision);
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