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

import bisq.common.locale.LocaleRepository;
import bisq.common.monetary.Quote;
import bisq.common.util.DecimalFormatters;
import bisq.common.util.MathUtils;

import java.util.Locale;

public class QuoteFormatter {
    public static String formatWithQuoteCode(Quote quote) {
        return formatWithQuoteCode(quote, LocaleRepository.getDefaultLocale());
    }

    public static String formatMarketPriceOffset(double offset) {
        return MathUtils.roundDouble(offset * 100, 2) + "%";
    }

    public static String formatWithQuoteCode(Quote quote, Locale locale) {
        return format(quote, locale) + " " + quote.getQuoteCodePair();
    }

    public static String format(Quote quote) {
        return format(quote, LocaleRepository.getDefaultLocale());
    }

    public static String format(Quote quote, boolean useLowPrecision) {
        return format(quote, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String format(Quote quote, Locale locale) {
        return getDecimalFormat(quote, locale, false).format(quote.asDouble());
    }

    public static String format(Quote quote, Locale locale, boolean useLowPrecision) {
        return getDecimalFormat(quote, locale, useLowPrecision).format(quote.asDouble());
    }

    private static DecimalFormatters.Format getDecimalFormat(Quote quote, Locale locale, boolean useLowPrecision) {
        int exponent = useLowPrecision ? quote.getLowPrecision() : quote.getPrecision();
        return DecimalFormatters.getDecimalFormat(locale, exponent);
    }


}
