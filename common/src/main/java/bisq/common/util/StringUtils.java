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

import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class StringUtils {
    public static String truncate(String value) {
        return truncate(value, 32);
    }

    public static String truncate(String value, int maxLength) {
        checkArgument(maxLength > 3, "maxLength must be > 3");
        if (value.length() > maxLength) {
            return value.substring(0, maxLength - 3) + "...";
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

    // Replaces the content inside the brackets marked with HYPERLINK with the number of the hyperlink
    // and add the hyperlink to the hyperlinks list.
    // E.g. ...some text [HYPERLINK:https://bisq.community] .... -> ...some text [1] ...
    public static String extractHyperlinks(String message, List<String> hyperlinks) {
        Pattern pattern = Pattern.compile("\\[HYPERLINK:(.*?)]");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {  // extract hyperlinks & store in array
            hyperlinks.add(matcher.group(1));
            // replace hyperlink in message with [n] reference
            message = message.replaceFirst(pattern.toString(), String.format("[%d]", hyperlinks.size()));
        }
        return message;
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static String snakeCaseToCamelCase(String value) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, value.toLowerCase());
    }

    public static String kebapCaseToCamelCase(String value) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, value.toLowerCase());
    }

    public static String camelCaseToSnakeCase(String value) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, value);
    }

    public static String camelCaseToKebapCase(String value) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, value);
    }

    public static String snakeCaseToKebapCase(String value) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, value.toLowerCase());
    }
}
