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

package network.misq.presentation.formatters;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.locale.LocaleRepository;
import network.misq.common.monetary.Fiat;
import network.misq.common.monetary.Monetary;
import network.misq.common.util.DecimalFormatters;

import java.util.Locale;
import java.util.Optional;

@Slf4j
public class AmountFormatter {
    public static String formatAmountWithMinAmount(Monetary amount, Optional<Long> optionalMinAmount) {
        return AmountFormatter.formatMinAmount(optionalMinAmount, amount) +
                AmountFormatter.formatAmount(amount);
    }

    public static String formatAmount1(long amount, String currencyCode) {
        return "TODO";//  Monetary.from(amount, currencyCode).formatWithCode();
    }

    public static String formatAmountWithCode(Monetary amount) {
        return formatAmountWithCode(amount, LocaleRepository.getDefaultLocale());
    }

    public static String formatAmountWithCode(Monetary amount, Locale locale) {
        return formatAmount(amount, locale) + " " + amount.getCurrencyCode();
    }

    public static String formatAmount(Monetary amount) {
        return formatAmount(amount, LocaleRepository.getDefaultLocale());
    }

    public static String formatAmount(Monetary amount, Locale locale) {
        return getDecimalFormat(amount, locale).format(amount.asDouble());
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount, Monetary amount) {
        return formatMinAmount(optionalMinAmount, amount, LocaleRepository.getDefaultLocale());
    }

    public static String formatMinAmount(Optional<Long> optionalMinAmount, Monetary amount, Locale locale) {
        return optionalMinAmount
                .map(minAmount -> getDecimalFormat(amount, locale).format(amount.toDouble(minAmount)) + " - ")
                .orElse("");
    }

    private static DecimalFormatters.Format getDecimalFormat(Monetary amount, Locale locale) {
        int precision = amount instanceof Fiat ? 0 : 4;
        return DecimalFormatters.getDecimalFormat(locale, precision);
    }
}
