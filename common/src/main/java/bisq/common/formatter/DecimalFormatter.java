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

package bisq.common.formatter;

import bisq.common.data.Pair;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class DecimalFormatter {
    @EqualsAndHashCode
    @ToString
    public static final class Format {
        private final DecimalFormat decimalFormat;

        /**
         * Wrapper to make DecimalFormat immutable and expose only what we use.
         */
        public Format(DecimalFormat decimalFormat) {
            this.decimalFormat = decimalFormat;
        }

        public String format(long number) {
            return decimalFormat.format(number);
        }

        public String format(double number) {
            return decimalFormat.format(number);
        }
    }

    /**
     * Caches formatters which have the same parameters. We use formatters as stateless immutable objects.
     */
    private static final LoadingCache<Pair<Locale, Integer>, Format> decimalFormatCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(DecimalFormatter::getDecimalFormat));

    /**
     * @param locale    The locale to be used
     * @param precision The precision
     * @return Returns cached DecimalFormat object.
     */
    public static Format getDecimalFormat(Locale locale, int precision) {
        return decimalFormatCache.getUnchecked(new Pair<>(locale, precision));
    }

    private static Format getDecimalFormat(Pair<Locale, Integer> pair) {
        Locale locale = pair.getFirst();
        int precision = pair.getSecond();
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        if (precision > 0) {
            decimalFormat.applyPattern(getPattern(precision));
        } else {
            decimalFormat.applyPattern("0");
        }

        return new Format(decimalFormat);
    }

    private static String getPattern(int precision) {
        return "0." + "0".repeat(Math.max(0, precision));
    }
}