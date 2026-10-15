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
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Optional;

@Slf4j
public class AmountFormatter {
    // Generic amount
    public static String formatAmountWithCode(Monetary amount, boolean useLowPrecision) {
        return formatAmountWithCode(amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatAmount(Monetary amount, boolean useLowPrecision) {
        return formatAmount(amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatAmount(Monetary amount, int precision) {
        return formatAmount(amount, LocaleRepository.getDefaultLocale(), precision);
    }

    public static String formatAmount(Monetary amount, Locale locale, boolean useLowPrecision) {
        return getDecimalFormat(amount, locale, useLowPrecision).format(amount.asDouble());
    }

    public static String formatAmount(Monetary amount, Locale locale, int precision) {
        return getDecimalFormat(locale, precision).format(amount.asDouble());
    }

    public static String formatAmountWithCode(Monetary amount, Locale locale, boolean useLowPrecision) {
        return formatAmount(amount, locale, useLowPrecision) + " " + amount.getCode();
    }

    public static String formatAmountWithMinAmount(Monetary amount,
                                                   Optional<Long> optionalMinAmount,
                                                   boolean useLowPrecision) {
        return AmountFormatter.formatMinAmount(optionalMinAmount, amount, useLowPrecision) +
                AmountFormatter.formatAmount(amount, useLowPrecision);
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount, Monetary amount, boolean useLowPrecision) {
        return formatMinAmount(optionalMinAmount, amount, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount,
                                         Monetary amount,
                                         Locale locale,
                                         boolean useLowPrecision) {
        return optionalMinAmount
                .map(minAmount -> getDecimalFormat(amount, locale, useLowPrecision).format(amount.toDouble(minAmount)) + " - ")
                .orElse("");
    }

    // Base amount
    public static String formatBaseAmount(Monetary amount) {
        return formatAmountByMonetaryType(amount);
    }

    public static String formatBaseAmountWithCode(Monetary amount) {
        return formatAmountWithCodeByMonetaryType(amount);
    }

    // Quote amount
    public static String formatQuoteAmount(Monetary amount) {
        return formatAmountByMonetaryType(amount);
    }

    public static String formatQuoteAmountWithCode(Monetary amount) {
        return formatAmountWithCodeByMonetaryType(amount);
    }

    public static String formatAmountByMonetaryType(Monetary amount) {
        return formatAmount(amount, amount instanceof Fiat);
    }

    public static String formatAmountWithCodeByMonetaryType(Monetary amount) {
        return formatAmountWithCode(amount, LocaleRepository.getDefaultLocale(), amount instanceof Fiat);
    }

    public static DecimalFormatter.Format getDecimalFormat(Monetary amount, Locale locale, boolean useLowPrecision) {
        return useLowPrecision ?
                getDecimalFormat(locale, amount.getLowPrecision()) :
                getDecimalFormat(locale, amount.getPrecision());
    }

    public static DecimalFormatter.Format getDecimalFormat(Locale locale, int precision) {
        return DecimalFormatter.getDecimalFormat(locale, precision);
    }
}
