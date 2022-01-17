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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@ToString
@Slf4j
public abstract class Monetary implements Comparable<Monetary> {
    public static long doubleValueToLong(double value, int smallestUnitExponent) {
        double max = BigDecimal.valueOf(Long.MAX_VALUE).movePointLeft(smallestUnitExponent).doubleValue();
        if (value > max) {
            throw new ArithmeticException("Provided value would lead to an overflow");
        }
        return BigDecimal.valueOf(value).movePointRight(smallestUnitExponent).longValue();
    }

    public static Monetary from(Monetary monetary, long newValue) {
        if (monetary instanceof Fiat) {
            return Fiat.of(newValue, monetary.getCode(), monetary.getSmallestUnitExponent());
        } else {
            return Coin.of(newValue, monetary.getCode(), monetary.getSmallestUnitExponent());
        }
    }

    public static Monetary from(long amount, String code) {
        if (BisqCurrency.isFiat(code)) {
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
    protected final int smallestUnitExponent;

    protected Monetary(String id, long value, String code, int smallestUnitExponent) {
        this.id = id;
        this.value = value;
        this.code = code;
        this.smallestUnitExponent = smallestUnitExponent;
    }

    protected Monetary(String id, double value, String code, int smallestUnitExponent) {
        this(id, doubleValueToLong(value, smallestUnitExponent), code, smallestUnitExponent);
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