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
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@ToString
@Slf4j
public abstract class Monetary implements Comparable<Monetary>, PersistableProto {
    public static long faceValueToLong(double faceValue, int precision) {
        double max = BigDecimal.valueOf(Long.MAX_VALUE).movePointLeft(precision).doubleValue();
        if (faceValue > max) {
            throw new ArithmeticException("Provided value would lead to an overflow");
        }
        return BigDecimal.valueOf(faceValue).movePointRight(precision).longValue();
    }

    public static double toFaceValue(Monetary monetary, int precision) {
        double fullPrecision = MathUtils.scaleDownByPowerOf10(monetary.getValue(), monetary.getPrecision());
        return MathUtils.roundDouble(fullPrecision, precision);
    }

    public static Monetary clone(Monetary monetary) {
        return from(monetary, monetary.getValue());
    }

    public static Monetary from(Monetary monetary, long newValue) {
        if (monetary instanceof Fiat) {
            return Fiat.fromValue(newValue, monetary.getCode(), monetary.getPrecision());
        } else {
            return Coin.fromValue(newValue, monetary.getCode(), monetary.getPrecision());
        }
    }

    public static Monetary from(long amount, String code) {
        if (TradeCurrency.isFiat(code)) {
            return Fiat.fromValue(amount, code);
        } else {
            return Coin.fromValue(amount, code);
        }
    }

    /**
     * Unique ID in case an altcoin uses a code used by a fiat currency (happened in the past)
     */
    protected final String id;
    protected final long value;
    protected final String code;
    protected final int precision;
    protected final int lowPrecision;

    protected Monetary(String id, long value, String code, int precision, int lowPrecision) {
        this.id = id;
        this.value = value;
        this.code = code;
        this.precision = precision;
        this.lowPrecision = lowPrecision;

        NetworkDataValidation.validateText(id, 20);
        NetworkDataValidation.validateCode(code);
    }

    /**
     * @param faceValue Monetary value as face value. E.g. 123.45 USD or 1.12345678 BTC
     */
    protected Monetary(String id, double faceValue, String code, int precision, int lowPrecision) {
        this(id, faceValueToLong(faceValue, precision), code, precision, lowPrecision);
    }

    public abstract bisq.common.protobuf.Monetary toProto(boolean serializeForHash);

    public abstract bisq.common.protobuf.Monetary.Builder getBuilder(boolean serializeForHash);

    protected bisq.common.protobuf.Monetary.Builder getMonetaryBuilder() {
        return bisq.common.protobuf.Monetary.newBuilder()
                .setId(id)
                .setValue(value)
                .setCode(code)
                .setPrecision(precision)
                .setLowPrecision(lowPrecision);
    }

    public static Monetary fromProto(bisq.common.protobuf.Monetary proto) {
        return switch (proto.getMessageCase()) {
            case COIN -> Coin.fromProto(proto);
            case FIAT -> Fiat.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    public abstract double toDouble(long value);

    public double asDouble() {
        return toDouble(value);
    }

    @Override
    public int compareTo(Monetary other) {
        return Long.compare(value, other.getValue());
    }

    public abstract String getName();

    public abstract Monetary round(int roundPrecision);

    public long getRoundedValueForLowPrecision() {
        return getRoundedValueForPrecision(lowPrecision);
    }

    public long getRoundedValueForPrecision(int precision) {
        int scale = this.precision - precision;
        double scaledDown = MathUtils.scaleDownByPowerOf10(value, scale);
        double rounded = MathUtils.roundDouble(scaledDown, 0);
        double scaledUp = MathUtils.scaleUpByPowerOf10(rounded, scale);
        return MathUtils.roundDoubleToLong(scaledUp);
    }

    public boolean isLessThan(Monetary other) {
        return isLessThan(other, precision);
    }

    public boolean isLessThan(Monetary other, int precision) {
        return compare(other, precision, ComparisonOperator.IS_LESS_THAN);
    }

    public boolean isLessThanOrEqual(Monetary other) {
        return isLessThanOrEqual(other, precision);
    }

    public boolean isLessThanOrEqual(Monetary other, int precision) {
        return compare(other, precision, ComparisonOperator.IS_LESS_THAN_OR_EQUAL);
    }

    public boolean isGreaterThan(Monetary other) {
        return isGreaterThan(other, precision);
    }

    public boolean isGreaterThan(Monetary other, int precision) {
        return compare(other, precision, ComparisonOperator.IS_GREATER_THAN);
    }

    public boolean isGreaterThanOrEqual(Monetary other) {
        return isGreaterThanOrEqual(other, precision);
    }

    public boolean isGreaterThanOrEqual(Monetary other, int precision) {
        return compare(other, precision, ComparisonOperator.IS_GREATER_THAN_OR_EQUAL);
    }

    public boolean isEqual(Monetary other) {
        return isEqual(other, precision);
    }

    public boolean isEqual(Monetary other, int precision) {
        return compare(other, precision, ComparisonOperator.IS_EQUAL);
    }

    private boolean compare(Monetary other, int precision, ComparisonOperator comparisonOperator) {
        long valueForPrecision = getRoundedValueForPrecision(precision);
        long otherValueForPrecision = other.getRoundedValueForPrecision(precision);
        return switch (comparisonOperator) {
            case IS_LESS_THAN -> valueForPrecision < otherValueForPrecision;
            case IS_LESS_THAN_OR_EQUAL -> valueForPrecision <= otherValueForPrecision;
            case IS_GREATER_THAN -> valueForPrecision > otherValueForPrecision;
            case IS_GREATER_THAN_OR_EQUAL -> valueForPrecision >= otherValueForPrecision;
            case IS_EQUAL -> valueForPrecision == otherValueForPrecision;
        };
    }

    private enum ComparisonOperator {
        IS_LESS_THAN,
        IS_LESS_THAN_OR_EQUAL,
        IS_GREATER_THAN,
        IS_GREATER_THAN_OR_EQUAL,
        IS_EQUAL
    }
}