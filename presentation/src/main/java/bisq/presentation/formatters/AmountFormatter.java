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

import bisq.common.formatter.DecimalFormatter;
import bisq.common.locale.LocaleRepository;
import bisq.common.monetary.Monetary;
import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public class AmountFormatter {
    public static String formatAmountWithMinAmount(Monetary amount, Optional<Long> optionalMinAmount, boolean useLowPrecision) {
        return AmountFormatter.formatMinAmount(optionalMinAmount, amount, useLowPrecision) +
                AmountFormatter.formatAmount(amount, useLowPrecision);
    }

    public static String formatAmountWithCode(Monetary amount) {
        return formatAmountWithCode(amount, LocaleRepository.getDefaultLocale(), true);
    }

    public static String formatAmountWithCode(Monetary amount, boolean useLowPrecision) {
        return formatAmountWithCode(amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatAmountWithCode(Monetary amount, Locale locale, boolean useLowPrecision) {
        return formatAmount(amount, locale, useLowPrecision) + " " + amount.getCode();
    }

    public static String formatAmount(Monetary amount) {
        return formatAmount(amount, LocaleRepository.getDefaultLocale(), true);
    }

    public static String formatAmount(Monetary amount, boolean useLowPrecision) {
        return formatAmount(amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatAmount(Monetary amount, Locale locale) {
        return getDecimalFormat(amount, locale, true).format(amount.asDouble());
    }

    public static String formatAmount(Monetary amount, Locale locale, boolean useLowPrecision) {
        return getDecimalFormat(amount, locale, useLowPrecision).format(amount.asDouble());
    }

    public static String formatWithDecimalGroups(Monetary amount, Locale locale, boolean useLowPrecision) {
        String formatted = formatAmount(amount, locale, useLowPrecision);
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        char decimalSeparator = symbols.getDecimalSeparator();
        String regex = decimalSeparator == '.' ? "\\." : String.valueOf(decimalSeparator);
        String[] parts = formatted.split(regex);
        if (parts.length == 2 && parts[1].length() == 8) {
            String part1 = parts[0];
            String part2 = parts[1].substring(0, 4);
            String part3 = parts[1].substring(4);
            return part1 + decimalSeparator + part2 + " " + part3;
        } else {
            return formatted;
        }
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount, Monetary amount, boolean useLowPrecision) {
        return formatMinAmount(optionalMinAmount, amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount, Monetary amount, Locale locale, boolean useLowPrecision) {
        return optionalMinAmount
                .map(minAmount -> getDecimalFormat(amount, locale, useLowPrecision).format(amount.toDouble(minAmount)) + " - ")
                .orElse("");
    }

    private static DecimalFormatter.Format getDecimalFormat(Monetary amount, Locale locale, boolean useLowPrecision) {
        return useLowPrecision ?
                DecimalFormatter.getDecimalFormat(locale, amount.getLowPrecision()) :
                DecimalFormatter.getDecimalFormat(locale, amount.getPrecision());
    }
}
