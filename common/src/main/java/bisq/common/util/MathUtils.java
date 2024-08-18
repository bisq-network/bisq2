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

package bisq.common.util;

import com.google.common.math.DoubleMath;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class MathUtils {
    public static final double LOG2 = 0.6931471805599453; // Math.log(2)

    public static double roundDouble(double value, int precision) {
        return roundDouble(value, precision, RoundingMode.HALF_UP);
    }

    public static double roundDouble(double value, int precision, RoundingMode roundingMode) {
        if (precision < 0) {
            throw new IllegalArgumentException("precision must not be negative");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Expected a finite double, but found " + value);
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(precision, roundingMode);
        return bd.doubleValue();
    }

    public static long roundDoubleToLong(double value) {
        return roundDoubleToLong(value, RoundingMode.HALF_UP);
    }

    @SuppressWarnings("SameParameterValue")
    public static long roundDoubleToLong(double value, RoundingMode roundingMode) {
        return DoubleMath.roundToLong(value, roundingMode);
    }

    public static int roundDoubleToInt(double value) {
        return roundDoubleToInt(value, RoundingMode.HALF_UP);
    }

    @SuppressWarnings("SameParameterValue")
    public static int roundDoubleToInt(double value, RoundingMode roundingMode) {
        return DoubleMath.roundToInt(value, roundingMode);
    }

    public static double scaleUpByPowerOf10(double value, int exponent) {
        double factor = Math.pow(10, exponent);
        return value * factor;
    }

    public static double scaleUpByPowerOf10(long value, int exponent) {
        double factor = Math.pow(10, exponent);
        return ((double) value) * factor;
    }

    public static double scaleDownByPowerOf10(double value, int exponent) {
        double factor = Math.pow(10, exponent);
        return value / factor;
    }

    public static double scaleDownByPowerOf10(long value, int exponent) {
        double factor = Math.pow(10, exponent);
        return ((double) value) / factor;
    }

    public static double exactMultiply(double value1, double value2) {
        return BigDecimal.valueOf(value1).multiply(BigDecimal.valueOf(value2)).doubleValue();
    }

    public static long doubleToLong(double value) {
        return Double.valueOf(value).longValue();
    }

    public static double bounded(double lowerBound, double upperBound, double value) {
        checkArgument(lowerBound <= upperBound,
                "lowerBound must not be larger than upperBound");
        return Math.min(Math.max(value, lowerBound), upperBound);
    }

    public static int bounded(int lowerBound, int upperBound, int value) {
        checkArgument(lowerBound <= upperBound,
                "lowerBound must not be larger than upperBound");
        return Math.min(Math.max(value, lowerBound), upperBound);
    }

    public static long bounded(long lowerBound, long upperBound, long value) {
        checkArgument(lowerBound <= upperBound,
                "lowerBound must not be larger than upperBound");
        return Math.min(Math.max(value, lowerBound), upperBound);
    }

    public static double getLog2(long value) {
        return Math.log(value) / Math.log(2);
    }

    public static double parseToDouble(String value) {
        String cleaned = StringUtils.removeAllWhitespaces(value).replace(",", ".");
        return Double.parseDouble(cleaned);
    }

    public static boolean isValidDouble(String value) {
        try {
            parseToDouble(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
