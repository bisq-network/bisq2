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

import bisq.common.locale.LocaleRepository;
import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.text.NumberFormat;

@Slf4j
public class PercentageFormatter {
    private static final NumberFormat DEFAULT_FORMAT = NumberFormat.getNumberInstance(LocaleRepository.getDefaultLocale());

    static {
        DEFAULT_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
        DEFAULT_FORMAT.setMinimumFractionDigits(2);
        DEFAULT_FORMAT.setMaximumFractionDigits(2);
        //Disable thousand separators if desired for percentages (e.g., avoid 1,234.56)
        DEFAULT_FORMAT.setGroupingUsed(false);
    }

    public static String formatToPercentWithSymbol(double value) {
        return formatToPercent(value) + "%";
    }

    public static String formatToPercentWithSignAndSymbol(double value) {
        return value > 0
                ? "+" + formatToPercentWithSymbol(value)
                : formatToPercentWithSymbol(value);
    }

    /**
     * Formats a value as a percentage string (without '%') using the current user locale.
     * @param value to be represented as percentage (e.g., 0.1 for 10%).
     *              Uses 2 fraction digits and RoundingMode.HALF_UP.
     * @return The formatted percentage value without the '%' sign, respecting the user's locale for decimal separators.
     */
    public static String formatToPercent(double value) {
        return formatToPercent(value, DEFAULT_FORMAT);
    }

    public static String formatToPercent(double value, NumberFormat defaultNumberFormat) {
        return defaultNumberFormat.format(MathUtils.roundDouble(value * 100.0, 2));
    }
}
