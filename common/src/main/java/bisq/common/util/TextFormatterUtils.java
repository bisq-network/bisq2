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

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TextFormatterUtils {

    private static final Pattern UNSAFE_CHARS = Pattern.compile(".*[<>\"&'].*");

    public static boolean isValidLength(String text, int maxLength) {
        return StringUtils.isNotEmpty(text) && text.length() <= maxLength;
    }

    public static boolean isSafe(String text) {
        return StringUtils.isNotEmpty(text) && !UNSAFE_CHARS.matcher(text).matches();
    }

    public static Predicate<String> lengthLimit(int maxLength) {
        return text -> isValidLength(text, maxLength);
    }

    public static Predicate<String> safe() {
        return TextFormatterUtils::isSafe;
    }

    @SafeVarargs
    public static Predicate<String> allOf(Predicate<String>... conditions) {
        return text -> {
            for (Predicate<String> condition : conditions) {
                if (!condition.test(text)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static Predicate<String> safeWithLength(int maxLength) {
        return allOf(lengthLimit(maxLength), safe());
    }

    public static String formatIban(String iban) {
        return StringUtils.toOptional(iban)
                .filter(StringUtils::isNotEmpty)
                .map(value -> {
                    String cleanIban = value.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < cleanIban.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ");
                        }
                        formatted.append(cleanIban.charAt(i));
                    }
                    return formatted.toString();
                })
                .orElse("");
    }
}