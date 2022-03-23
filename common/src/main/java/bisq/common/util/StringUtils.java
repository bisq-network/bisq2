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

import java.text.DecimalFormat;
import java.util.UUID;

public class StringUtils {
    public static String truncate(String value) {
        return truncate(value, 32);
    }

    public static String truncate(String value, int maxLength) {
        return value.substring(0, Math.min(value.length(), maxLength)) + "...";
    }

    public static String createUid() {
        return UUID.randomUUID().toString();
    }

    public static String createShortUid() {
        return createUid(8);
    }

    public static String createUid(int maxLength) {
        return UUID.randomUUID().toString().substring(0, maxLength);
    }

    public static String fromBytes(long size) {
        if (size <= 0) return "0";
        String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.###").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String trimWhitespace(String value) {
        return value.replaceAll("\\s+", "");
    }

    public static String trimTrailingLinebreak(String value) {
        return value.substring(0, value.length() - 1);
    }

    public static String abbreviate(String message, int maxChar) {
        if (message.length() <= maxChar) {
            return message;
        }

        return message.substring(0, maxChar - 3) + "...";
    }

    public static boolean containsIgnoreCase(String string, String searchString) {
        if (string == null || searchString == null) {
            return false;
        }
        return string.toLowerCase().contains(searchString.toLowerCase());
    }
}
