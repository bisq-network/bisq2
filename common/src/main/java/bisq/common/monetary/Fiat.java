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

    public static Fiat parse(String string, String code, int precision) {
        return Fiat.of(new BigDecimal(string).movePointRight(precision).longValue(),
                code,
                precision);
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

    public static Fiat of(long value, String code, int precision) {
        return new Fiat(value, code, precision);
    }

    Fiat(long value, String code, int precision) {
        // For Fiat market price we show precision 2 but in trade context we show the highest precision (4 for Fiat)  
        super(code, value, code, precision, 2);
    }

    private Fiat(double value, String code, int precision) {
        super(code, value, code, precision, 2);
    }

    public Fiat add(Fiat value) {
        checkArgument(value.code.equals(this.code));
        return new Fiat(LongMath.checkedAdd(this.value, value.value), this.code, this.precision);
    }

    public Fiat subtract(Fiat value) {
        checkArgument(value.code.equals(this.code));
        return new Fiat(LongMath.checkedSubtract(this.value, value.value), this.code, this.precision);
    }

    public Fiat multiply(long factor) {
        return new Fiat(LongMath.checkedMultiply(this.value, factor), this.code, this.precision);
    }

    public Fiat divide(long divisor) {
        return new Fiat(this.value / divisor, this.code, this.precision);
    }

    @Override
    public double toDouble(long value) {
        return MathUtils.roundDouble(BigDecimal.valueOf(value).movePointLeft(precision).doubleValue(), precision);
    }

    @Override
    public String toString() {
        return "Fiat{" +
                "\r\n} " + super.toString();
    }
}