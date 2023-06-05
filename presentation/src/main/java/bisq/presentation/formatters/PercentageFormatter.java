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

import java.text.DecimalFormat;

public class PercentageFormatter {
    public static String formatToPercentWithSymbol(double value) {
        return formatToPercent(value) + "%";
    }

    public static String formatToRoundedPercentWithSymbol(double value) {
        return formatToPercent(value, new DecimalFormat("#")) + "%";
    }

    public static String formatToPercent(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setMaximumFractionDigits(2);
        return formatToPercent(value, decimalFormat);
    }

    public static String formatToPercent(double value, DecimalFormat decimalFormat) {
        return decimalFormat.format(MathUtils.roundDouble(value * 100.0, 2)).replace(",", ".");
    }
}