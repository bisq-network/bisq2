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

import bisq.common.data.Pair;
import bisq.common.platform.OS;
import bisq.common.platform.PlatformUtils;
import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class StringUtils {
    public static String truncate(Object value) {
        return truncate(value.toString());
    }

    public static String truncate(String value) {
        return truncate(value, 32);
    }

    public static String truncate(Object value, int maxLength) {
        return truncate(value.toString(), maxLength);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            log.warn("value at truncate is null");
            return "";
        }
        if (maxLength > 3 && value.length() > maxLength) {
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
        return createUid().substring(0, maxLength);
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
            return value.substring(0, 1).toUpperCase() + value.substring(1);
        }
    }

    public static String capitalizeAll(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        } else {
            return value.toUpperCase();
        }
    }

    // Replaces the content inside the brackets marked with HYPERLINK with the hyperlink and the number of the hyperlink
    // and add the hyperlink to the hyperlinks list.
    // E.g. ...some text: [HYPERLINK:https://bisq.community] .... -> ...some text: https://bisq.community [1] ...
    public static String extractHyperlinks(String message, List<String> hyperlinks) {
        Pattern pattern = Pattern.compile("\\[HYPERLINK:(.*?)]");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String link = matcher.group(1);
            hyperlinks.add(link);
            message = message.replaceFirst(pattern.toString(), String.format("'%s' [%d]", link, hyperlinks.size()));
        }
        return message;
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Nullable
    public static String toNullIfEmpty(String value) {
        return isEmpty(value) ? null : value;
    }

    public static Optional<String> toOptional(String value) {
        return Optional.ofNullable(toNullIfEmpty(value));
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

    public static List<Pair<String, List<String>>> getTextStylePairs(String input) {
        List<Pair<String, List<String>>> result = new ArrayList<>();
        if (input == null) {
            return result;
        }
        if (input.isEmpty()) {
            result.add(new Pair<>("", List.of()));
            return result;
        }
        input = input.replace("/", "|");
        Pattern pattern = Pattern.compile("<([^<>/]+)\\s+style=([^<>/]+)\\s*>");

        Matcher matcher = pattern.matcher(input);
        int prevEnd = 0;
        while (matcher.find()) {
            String text = input.substring(prevEnd, matcher.start());
            if (!text.isEmpty()) {
                result.add(new Pair<>(text, null));
            }
            text = matcher.group(1);

            text = text.replace("|", "/");
            String style = matcher.group(2);

            if (!style.contains(",")) {
                Pair<String, List<String>> pair = new Pair<>(text, List.of(style));
                result.add(pair);
            } else {
                List<String> styles = List.of(style.replace(", ", ",").split(","));
                Pair<String, List<String>> pair = new Pair<>(text, styles);
                result.add(pair);
            }

            prevEnd = matcher.end();
        }

        if (prevEnd < input.length()) {
            String remainingText = input.substring(prevEnd);
            Pair<String, List<String>> remainingPair = new Pair<>(remainingText, null);
            result.add(remainingPair);
        }

        return result;
    }

    public static String formatBytes(long size) {
        //todo Use ByteUnit instead
        if (size <= 0) return "0 B";
        String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.###").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String formatTime(long time) {
        double sec = MathUtils.roundDouble(time / 1000d, 2);
        long min = (int) sec / 60;
        sec = sec % 60;
        long hours = min / 60;
        min = min % 60;
        long days = hours / 24;
        hours = hours % 24;
        if (days == 0) {
            return String.format("%02d:%02d:%02.2f", hours, min, sec);
        } else if (days == 1) {
            return String.format("1 day, %02d:%02d:%02.2f", hours, min, sec);
        } else {
            return String.format("%02d days, %02d:%02d:%02.2f", days, hours, min, sec);
        }
    }

    public static String maskHomeDirectory(String string) {
        // TODO: check out if we can reliably mask it on Android
        if (OS.isAndroid()) {
            return string;
        }
        return string.replace(PlatformUtils.getHomeDirectory(), "<HOME_DIR>");
    }
}
