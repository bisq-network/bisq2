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

package bisq.common.currency;

import bisq.common.locale.CountryRepository;
import bisq.common.locale.LocaleRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class FiatCurrencyRepository {
    private static Map<String, FiatCurrency> currencyByCode;
    @Getter
    private static List<FiatCurrency> allCurrencies;
    @Getter
    private static List<FiatCurrency> majorCurrencies;
    @Getter
    private static List<FiatCurrency> minorCurrencies;
    @Getter
    private static FiatCurrency defaultCurrency;

    static {
        setLocale(LocaleRepository.getDefaultLocale());
    }

    // Need to be called at application setup with user locale
    public static void setLocale(Locale locale) {
        currencyByCode = CountryRepository.getCountries().stream()
                .map(country -> getCurrencyByCountryCode(country.getCode(), locale))
                .distinct()
                .collect(Collectors.toMap(FiatCurrency::getCode, Function.identity(), (x, y) -> x, HashMap::new));

        defaultCurrency = getCurrencyByCountryCode(locale.getCountry(), locale);

        majorCurrencies = initMajorCurrencies();
        majorCurrencies.remove(defaultCurrency);

        minorCurrencies = new ArrayList<>(currencyByCode.values());
        minorCurrencies.remove(defaultCurrency);
        minorCurrencies.removeAll(majorCurrencies);
        minorCurrencies.sort(Comparator.comparing(TradeCurrency::getDisplayNameAndCode));

        allCurrencies = new ArrayList<>();
        allCurrencies.add(defaultCurrency);
        allCurrencies.addAll(majorCurrencies);
        allCurrencies.addAll(minorCurrencies);
    }

    private static List<FiatCurrency> initMajorCurrencies() {
        List<String> mainCodes = new ArrayList<>(List.of("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN"));
        return mainCodes.stream()
                .map(code -> currencyByCode.get(code))
                .distinct()
                .collect(Collectors.toList());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode) {
        return getCurrencyByCountryCode(countryCode, LocaleRepository.getDefaultLocale());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode, Locale locale) {
        if (countryCode.equals("XK")) {
            return new FiatCurrency("EUR");
        }

        // The language and variant components of the locale at Currency.getInstance are ignored.
        Locale countryLocale = new Locale(locale.getLanguage(), countryCode);
        Currency currency = Currency.getInstance(countryLocale);
        return new FiatCurrency(currency);
    }

    public static Map<String, FiatCurrency> getCurrencyByCodeMap() {
        return currencyByCode;
    }

    public static FiatCurrency getCurrencyByCode(String code) {
        return currencyByCode.get(code);
    }

    public static Optional<String> getName(String code) {
        return Optional.ofNullable(currencyByCode.get(code)).map(TradeCurrency::getName);
    }

    public static Optional<String> getDisplayName(String code) {
        return Optional.ofNullable(currencyByCode.get(code)).map(TradeCurrency::getDisplayName);
    }

    public static List<String> getAllFiatCurrencyCodes() {
        return getAllCurrencies().stream().map(e -> e.getCurrency().getCurrencyCode()).collect(Collectors.toList());
    }


    public static String getSymbol(String code) {
        return Currency.getInstance(code.toUpperCase()).getSymbol();
    }
}
