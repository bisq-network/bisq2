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

package bisq.common.asset;

import bisq.common.locale.CountryRepository;
import bisq.common.locale.LocaleRepository;
import bisq.common.util.LocaleFactory;
import bisq.common.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
        checkNotNull(locale, "locale must not be null at setLocale");
        currencyByCode = CountryRepository.getAllCountries().stream()
                .filter(Objects::nonNull)
                .map(country -> getCurrencyByCountryCode(country.getCode(), locale))
                .distinct()
                .collect(Collectors.toMap(FiatCurrency::getCode, Function.identity(), (x, y) -> x, HashMap::new));

        defaultCurrency = getCurrencyByCountryCode(locale.getCountry(), locale);

        majorCurrencies = initMajorCurrencies();
        majorCurrencies.remove(defaultCurrency);

        minorCurrencies = new ArrayList<>(currencyByCode.values());
        minorCurrencies.remove(defaultCurrency);
        minorCurrencies.removeAll(majorCurrencies);
        minorCurrencies.sort(Comparator.comparing(Asset::getDisplayNameAndCode));

        allCurrencies = new ArrayList<>();
        allCurrencies.add(defaultCurrency);
        allCurrencies.addAll(majorCurrencies);
        allCurrencies.addAll(minorCurrencies);
    }

    private static List<FiatCurrency> initMajorCurrencies() {
        List<String> mainCodes = new ArrayList<>(List.of("USD", "EUR", "GBP", "CAD", "AUD", "RUB", "CNY", "INR", "NGN"));
        return mainCodes.stream()
                .map(code -> currencyByCode.get(code))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode) {
        return getCurrencyByCountryCode(countryCode, LocaleRepository.getDefaultLocale());
    }

    public static FiatCurrency getCurrencyByCountryCode(String countryCode, Locale locale) {
        Locale countryLocale;
        String localeLanguage;
        try {
            checkArgument(!StringUtils.isEmpty(countryCode), "countryCode must not be null or empty");

            if (countryCode.equals("XK")) {
                return new FiatCurrency("EUR");
            }

            checkNotNull(locale, "locale must not be null");
            localeLanguage = locale.getLanguage();
            checkNotNull(localeLanguage, "localeLanguage must not be null");
            countryLocale = LocaleFactory.from(localeLanguage, countryCode);

            // The variant component of the locale at Currency.getInstance are ignored.
            Currency currency = Currency.getInstance(countryLocale);
            checkNotNull(currency, "currency must not be null");

            return new FiatCurrency(currency);
        } catch (Exception e) {
            log.error("Cannot derive currency from countryCode {} and locale {}. We fall back to USD.", countryCode, locale, e);
            return new FiatCurrency("USD");
        }
    }

    public static Map<String, FiatCurrency> getCurrencyByCodeMap() {
        return currencyByCode;
    }

    public static FiatCurrency getCurrencyByCode(String code) {
        return currencyByCode.get(code);
    }

    public static List<FiatCurrency> getCurrencyByCodes(List<String> codes) {
        return codes.stream().map(e -> currencyByCode.get(e)).collect(Collectors.toList());
    }

    public static Optional<String> findName(String code) {
        return Optional.ofNullable(currencyByCode.get(code)).map(Asset::getName);
    }

    public static Optional<String> findDisplayName(String code) {
        return Optional.ofNullable(currencyByCode.get(code)).map(Asset::getDisplayName);
    }

    public static List<String> getAllFiatCurrencyCodes() {
        return getAllCurrencies().stream()
                .map(e -> e.getCurrency().getCurrencyCode())
                .collect(Collectors.toList());
    }

    public static String getDisplayNameAndCode(String currencyCode) {
        return FiatCurrencyRepository.getCurrencyByCode(currencyCode).getDisplayNameAndCode();
    }

    public static String getCodeAndDisplayName(String currencyCode) {
        return FiatCurrencyRepository.getCurrencyByCode(currencyCode).getCodeAndDisplayName();
    }

    public static String getDisplayNameAndCodes(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getDisplayNameAndCode)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public static String getCodeAndDisplayNames(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCodeAndDisplayName)
                .sorted()
                .collect(Collectors.joining(", "));
    }


    public static String getSymbol(String code) {
        return Currency.getInstance(code.toUpperCase(Locale.ROOT)).getSymbol();
    }
}
