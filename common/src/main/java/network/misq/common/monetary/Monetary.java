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
import lombok.extern.slf4j.Slf4j;
import network.misq.common.currency.MisqCurrency;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@Slf4j
public abstract class Monetary implements Comparable<Monetary> {
    protected final long value;
    protected final String currencyCode;
    protected final int smallestUnitExponent;

    public static Monetary from(Monetary monetary, long newValue) {
        if (monetary instanceof Fiat) {
            return Fiat.of(newValue, monetary.getCurrencyCode(), monetary.getSmallestUnitExponent());
        } else {
            return Coin.of(newValue, monetary.getCurrencyCode(), monetary.getSmallestUnitExponent());
        }
    }

    public static Monetary from(long amount, String currencyCode) {
        if (MisqCurrency.isFiat(currencyCode)) {
            return Fiat.of(amount, currencyCode);
        } else {
            return Coin.of(amount, currencyCode);
        }
    }

    protected Monetary(long value, String currencyCode, int smallestUnitExponent) {
        this.value = value;
        this.smallestUnitExponent = smallestUnitExponent;
        this.currencyCode = currencyCode;
    }

    protected Monetary(double value, String currencyCode, int smallestUnitExponent) {
        double max = BigDecimal.valueOf(Long.MAX_VALUE).movePointLeft(smallestUnitExponent).doubleValue();
        if (value > max) {
            throw new ArithmeticException("Provided value would lead to an overflow");
        }
        this.value = BigDecimal.valueOf(value).movePointRight(smallestUnitExponent).longValue();
        this.smallestUnitExponent = smallestUnitExponent;
        this.currencyCode = currencyCode;
    }

    abstract public double toDouble(long value);

    public double asDouble() {
        return toDouble(value);
    }

    @Override
    public int compareTo(Monetary o) {
        return Long.compare(value, o.getValue());
    }

    @Override
    public String toString() {
        return "Monetary{" +
                "\r\n     value=" + value +
                ",\r\n     currencyCode='" + currencyCode + '\'' +
                ",\r\n     smallestUnitExponent=" + smallestUnitExponent +
                "\r\n}";
    }
}