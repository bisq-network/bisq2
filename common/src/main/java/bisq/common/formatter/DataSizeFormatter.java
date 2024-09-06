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

package bisq.common.formatter;

import bisq.common.data.ByteUnit;
import bisq.common.util.MathUtils;

public class DataSizeFormatter {

    public static String format(double sizeInBytes) {
        String asMB = "";
        double mb = ByteUnit.BYTE.toMB(sizeInBytes);
        if (mb >= 1) {
            return formatMB(sizeInBytes, 2) + "; ";
        }
        return formatKB(sizeInBytes, 2);
    }

    public static String formatMB(double sizeInBytes) {
        return formatMB(sizeInBytes, 2);
    }

    public static String formatMB(double sizeInBytes, int precision) {
        return MathUtils.roundDouble(ByteUnit.BYTE.toMB(sizeInBytes), precision) + " MB";
    }

    public static String formatKB(double sizeInBytes) {
        return formatKB(sizeInBytes, 2);
    }

    public static String formatKB(double sizeInBytes, int precision) {
        return MathUtils.roundDouble(ByteUnit.BYTE.toKB(sizeInBytes), precision) + " KB";
    }
}