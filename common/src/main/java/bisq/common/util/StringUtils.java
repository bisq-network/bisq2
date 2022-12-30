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

import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.UUID;

@Slf4j
public class StringUtils {
    public static String truncate(String value) {
        return truncate(value, 32);
    }

    public static String truncate(String value, int maxLength) {
        if (maxLength < value.length()) {
            return value.substring(0, maxLength) + "...";
        } else {
            return value;
        }
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

    public static String removeAllWhitespaces(String value) {
        return value.replaceAll("\\s+", "");
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

    /*
     * Method used in chat message to check if we should show mention user/channel popup
     */
    public static String deriveWordStartingWith(String text, char indicatorSign) {
        int index = text.lastIndexOf(indicatorSign);
        if (index < 0 || (index > 1 && text.charAt(index - 1) != ' ')) return null;

        String result = text.substring(index + 1);
        if (result.matches("[a-zA-Z\\d]*$")) {
            return result;
        }
        return null;
    }

    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        } else if (value.length() == 1) {
            return value.toUpperCase();
        } else {
            return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
        }
    }
}
