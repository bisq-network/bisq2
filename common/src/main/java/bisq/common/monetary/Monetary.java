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

import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@ToString
@Slf4j
public abstract class Monetary implements Comparable<Monetary>, Serializable {
    public static long doubleValueToLong(double value, int precision) {
        double max = BigDecimal.valueOf(Long.MAX_VALUE).movePointLeft(precision).doubleValue();
        if (value > max) {
            throw new ArithmeticException("Provided value would lead to an overflow");
        }
        return BigDecimal.valueOf(value).movePointRight(precision).longValue();
    }

    public static Monetary from(Monetary monetary, long newValue) {
        if (monetary instanceof Fiat) {
            return Fiat.of(newValue, monetary.getCode(), monetary.getPrecision());
        } else {
            return Coin.of(newValue, monetary.getCode(), monetary.getPrecision());
        }
    }

    public static Monetary from(long amount, String code) {
        if (TradeCurrency.isFiat(code)) {
            return Fiat.of(amount, code);
        } else {
            return Coin.of(amount, code);
        }
    }

    /**
     * Unique ID in case an altcoin uses a code used by a fiat currency (happened in the past)
     */
    protected final String id;
    protected final long value;
    protected final String code;
    protected final int precision;
    protected final int minPrecision;

    protected Monetary(String id, long value, String code, int precision, int minPrecision) {
        this.id = id;
        this.value = value;
        this.code = code;
        this.precision = precision;
        this.minPrecision = minPrecision;
    }

    protected Monetary(String id, double value, String code, int precision, int minPrecision) {
        this(id, doubleValueToLong(value, precision), code, precision, minPrecision);
    }

    abstract public double toDouble(long value);

    public double asDouble() {
        return toDouble(value);
    }

    @Override
    public int compareTo(Monetary other) {
        return Long.compare(value, other.getValue());
    }
}