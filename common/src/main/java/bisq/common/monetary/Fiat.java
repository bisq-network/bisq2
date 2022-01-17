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

import bisq.common.currency.BisqCurrency;
import bisq.common.util.MathUtils;
import com.google.common.math.LongMath;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkArgument;

@EqualsAndHashCode(callSuper = true)
public class Fiat extends Monetary {

    public static Fiat parse(String string, String code) {
        return Fiat.of(new BigDecimal(string).movePointRight(4).longValue(),
                code,
                4);
    }

    public static Fiat parse(String string, String code, int smallestUnitExponent) {
        return Fiat.of(new BigDecimal(string).movePointRight(smallestUnitExponent).longValue(),
                code,
                smallestUnitExponent);
    }

    public static Fiat parse(String string) {
        String[] tokens = string.split(" ");
        if (tokens.length == 2) {
            String code = tokens[1];
            if (BisqCurrency.isFiat(code)) {
                return parse(tokens[0], code);
            }
        }
        throw new IllegalArgumentException("input could not be parsed. Expected: number value + space + currency code (e.g. 234.12 USD)");
    }

    public static Fiat of(long value, String code) {
        return new Fiat(value, code, 4);
    }

    public static Fiat of(double value, String code) {
        return new Fiat(value, code, 4);
    }

    public static Fiat of(long value, String code, int smallestUnitExponent) {
        return new Fiat(value, code, smallestUnitExponent);
    }

    Fiat(long value, String code, int smallestUnitExponent) {
        super(code, value, code, smallestUnitExponent);
    }

    private Fiat(double value, String code, int smallestUnitExponent) {
        super(code, value, code, smallestUnitExponent);
    }

    public Fiat add(Fiat value) {
        checkArgument(value.code.equals(this.code));
        return new Fiat(LongMath.checkedAdd(this.value, value.value), this.code, this.smallestUnitExponent);
    }

    public Fiat subtract(Fiat value) {
        checkArgument(value.code.equals(this.code));
        return new Fiat(LongMath.checkedSubtract(this.value, value.value), this.code, this.smallestUnitExponent);
    }

    public Fiat multiply(long factor) {
        return new Fiat(LongMath.checkedMultiply(this.value, factor), this.code, this.smallestUnitExponent);
    }

    public Fiat divide(long divisor) {
        return new Fiat(this.value / divisor, this.code, this.smallestUnitExponent);
    }

    @Override
    public double toDouble(long value) {
        return MathUtils.roundDouble(BigDecimal.valueOf(value).movePointLeft(smallestUnitExponent).doubleValue(), smallestUnitExponent);
    }

    @Override
    public String toString() {
        return "Fiat{" +
                "\r\n} " + super.toString();
    }
}