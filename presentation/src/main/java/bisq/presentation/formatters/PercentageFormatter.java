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

package bisq.presentation.formatters;

import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

@Slf4j
public class PercentageFormatter {
    public static final DecimalFormat DEFAULT_FORMAT = (DecimalFormat) DecimalFormat.getInstance(Locale.US);

    static {
        DEFAULT_FORMAT.setDecimalFormatSymbols(DefaultNumberFormatter.DEFAULT_SEPARATORS);
        DEFAULT_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
        DEFAULT_FORMAT.setMinimumFractionDigits(2);
        DEFAULT_FORMAT.setMaximumFractionDigits(2);
        DEFAULT_FORMAT.applyPattern("0.00");
    }

    public static String formatToPercentWithSymbol(double value) {
        return formatToPercent(value) + "%";
    }

    /**
     * @param value to be represented as percentage. 1 = 100 %. We show 2 fraction digits and use RoundingMode.HALF_UP
     * @return The formatted percentage value without the '%' sign
     */
    public static String formatToPercent(double value) {
        return formatToPercent(value, DEFAULT_FORMAT);
    }

    public static String formatToPercent(double value, DecimalFormat defaultNumberFormat) {
        return defaultNumberFormat.format(MathUtils.roundDouble(value * 100.0, 2));
    }
}
