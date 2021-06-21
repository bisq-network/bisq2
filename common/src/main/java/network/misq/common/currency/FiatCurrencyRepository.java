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

package network.misq.common.currency;

import network.misq.common.locale.CountryRepository;
import network.misq.common.locale.LocaleRepository;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiatCurrencyRepository {
    private static Map<String, FiatCurrency> fiatCurrencyByCode;

    static {
        applyLocale(LocaleRepository.getDefaultLocale());
    }

    // Need to be called at application setup with user locale
    public static void applyLocale(Locale locale) {
        fiatCurrencyByCode = CountryRepository.COUNTRIES.stream()
                .map(country -> getFiatCurrencyByCountryCode(country.code(), locale))
                .distinct()
                .collect(Collectors.toMap(FiatCurrency::getCode, Function.identity(), (x, y) -> x, HashMap::new));
    }

    public static FiatCurrency getFiatCurrencyByCountryCode(String countryCode, Locale locale) {
        if (countryCode.equals("XK")) {
            return new FiatCurrency("EUR", locale);
        }

        Currency currency = Currency.getInstance(new Locale(locale.getLanguage(), countryCode));
        return new FiatCurrency(currency.getCurrencyCode(), locale);
    }

    public static Map<String, FiatCurrency> getFiatCurrencyByCode() {
        checkNotNull(fiatCurrencyByCode, "applyLocale need to be called before accessing getFiatCurrencyByCode");
        return fiatCurrencyByCode;
    }
}
