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

package bisq.common.validation;

import bisq.common.locale.LocaleRepository;
import bisq.common.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class NumberValidation {
    private static final Map<String, Pattern> CACHE = new HashMap<>();

    /**
     * Validates a numeric input token using the default locale's decimal separator.
     * Only digits and a single optional decimal separator are allowed.
     * Leading sign characters such as '-' or '+' are not allowed.
     */
    public static boolean isValidNumberInputToken(String value) {
        Locale locale = LocaleRepository.getDefaultLocale();
        return isValidNumberInputToken(value, locale);
    }

    /**
     * Validates a numeric input token using the locale's decimal separator.
     * Only digits and a single optional decimal separator are allowed.
     * Leading sign characters such as '-' or '+' are not allowed.
     */
    public static boolean isValidNumberInputToken(String value, Locale locale) {
        char decimalSeparator = StringUtils.getDecimalSeparator(locale);
        return isValidNumberInputToken(value, decimalSeparator);
    }

    /**
     * Validates a numeric input token using the provided decimal separator.
     * Only digits and a single optional decimal separator are allowed.
     * Leading sign characters such as '-' or '+' are not allowed.
     */
    public static boolean isValidNumberInputToken(String value, char decimalSeparator) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        Pattern regex = getRegex(decimalSeparator);
        return regex.matcher(value).matches();
    }

    private static Pattern getRegex(char decimalSeparator) {
        String key = String.valueOf(decimalSeparator);
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        String sep = Pattern.quote(key);
        Pattern pattern = Pattern.compile("\\d*(" + sep + "\\d*)?");
        CACHE.put(key, pattern);
        return pattern;
    }
}
