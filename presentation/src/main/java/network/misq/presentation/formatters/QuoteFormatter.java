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

import network.misq.common.locale.LocaleRepository;
import network.misq.common.monetary.Fiat;
import network.misq.common.monetary.Quote;
import network.misq.common.util.DecimalFormatters;
import network.misq.common.util.MathUtils;

import java.util.Locale;

public class QuoteFormatter {
    public static String formatWithQuoteCode(Quote quote) {
        return formatWithQuoteCode(quote, LocaleRepository.getDefaultLocale());
    }

    public static String formatMarketPriceOffset(double offset) {
        return MathUtils.roundDouble(offset * 100, 2) + "%";
    }

    public static String formatWithQuoteCode(Quote quote, Locale locale) {
        return format(quote, locale) + " " + quote.getQuoteCode();
    }

    public static String format(Quote quote) {
        return format(quote, LocaleRepository.getDefaultLocale());
    }

    public static String format(Quote quote, Locale locale) {
        return getDecimalFormat(quote, locale).format(quote.asDouble());
    }

    private static DecimalFormatters.Format getDecimalFormat(Quote quote, Locale locale) {
        int precision = quote.getQuoteMonetary() instanceof Fiat ? 4 : quote.getSmallestUnitExponent();
        return DecimalFormatters.getDecimalFormat(locale, precision);
    }


}
