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
import bisq.common.monetary.PriceQuote;

import java.util.Locale;

public class PriceFormatter {
    public static String formatWithCode(PriceQuote priceQuote) {
        return formatWithCode(priceQuote, LocaleRepository.getDefaultLocale(), true);
    }

    public static String formatWithCode(PriceQuote priceQuote, boolean useLowPrecision) {
        return formatWithCode(priceQuote, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String formatWithCode(PriceQuote priceQuote, Locale locale, boolean useLowPrecision) {
        return format(priceQuote, locale, useLowPrecision) + " " + priceQuote.getMarket().getMarketCodes();
    }

    public static String format(PriceQuote priceQuote) {
        return format(priceQuote, LocaleRepository.getDefaultLocale());
    }

    public static String format(PriceQuote priceQuote, boolean useLowPrecision) {
        return format(priceQuote, LocaleRepository.getDefaultLocale(), useLowPrecision);
    }

    public static String format(PriceQuote priceQuote, Locale locale) {
        return getDecimalFormat(priceQuote, locale, true).format(priceQuote.asDouble());
    }

    public static String format(PriceQuote priceQuote, Locale locale, boolean useLowPrecision) {
        return getDecimalFormat(priceQuote, locale, useLowPrecision).format(priceQuote.asDouble());
    }

    private static DecimalFormatter.Format getDecimalFormat(PriceQuote priceQuote,
                                                            Locale locale,
                                                            boolean useLowPrecision) {
        int precision = useLowPrecision ? priceQuote.getLowPrecision() : priceQuote.getPrecision();
        return DecimalFormatter.getDecimalFormat(locale, precision);
    }
}
