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

package bisq.common.locale;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class CountryRepository {
    @Getter
    private static Country defaultCountry;
    private static final List<Country> ALL_COUNTRIES;
    private static final List<String> ALL_COUNTRY_CODES;

    public static void setDefaultCountry(Country defaultCountry) {
        CountryRepository.defaultCountry = defaultCountry;
    }

    public static void applyDefaultLocale(Locale defaultLocale) {
        CountryRepository.defaultCountry = findCountry(defaultLocale).orElseGet(() -> findCountry(Locale.US).orElseThrow());
    }

    public static Optional<Country> findCountry(Locale locale) {
        return ALL_COUNTRIES.stream().filter(c -> c.getCode().equals(locale.getCountry())).findAny();
    }

    public static List<Country> getAllCountries() {
        return ALL_COUNTRIES;
    }

    public static List<String> getAllCountyCodes() {
        return ALL_COUNTRY_CODES;
    }

    public static String getNameByCode(String countryCode) {
        if (countryCode.equals("XK")) {
            return "Republic of Kosovo";
        } else {
            return getLocalizedCountryDisplayString(countryCode);
        }
    }

    public static String getLocalizedCountryDisplayString(String countryCode) {
        return new Locale.Builder()
                .setLanguage(LanguageRepository.getDefaultLanguage())
                .setRegion(countryCode)
                .build()
                .getDisplayCountry();
    }

    public static Country getCountry(String countryCode) {
        String regionCode = RegionRepository.getRegionCode(countryCode);
        Region region = new Region(regionCode, RegionRepository.getRegionName(regionCode));

        Country country = new Country(countryCode, getLocalizedCountryDisplayString(countryCode), region);
        if (countryCode.equals("XK")) {
            country = new Country(countryCode, CountryRepository.getNameByCode(countryCode), region);
        }
        return country;
    }

    public static List<Country> getCountriesFromCodes(List<String> countryCodes) {
        return countryCodes.stream()
                .map(CountryRepository::getCountry)
                .collect(Collectors.toList());
    }

    static {
        ALL_COUNTRIES = LocaleRepository.LOCALES.stream()
                .map(locale -> {
                    String countryCode = locale.getCountry();
                    Region region = RegionRepository.getRegion(locale);
                    Country country = new Country(countryCode, locale.getDisplayCountry(), region);
                    if (locale.getCountry().equals("XK")) {
                        country = new Country(locale.getCountry(), "Republic of Kosovo", region);
                    }
                    return country;
                })
                .sorted(Comparator.comparing(Country::getName))
                .collect(Collectors.toUnmodifiableList());

        ALL_COUNTRY_CODES = ALL_COUNTRIES.stream()
                .map(Country::getCode)
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }


    public static boolean matchesAllCountries(List<String> countryCodes) {
        if (countryCodes.size() < ALL_COUNTRY_CODES.size()) {
            return false;
        }
        // CountryCodes might be unmodifiable list, thus we create an ArrayList
        List<String> sortedCountryCodes = new ArrayList<>(countryCodes);
        Collections.sort(sortedCountryCodes);
        return sortedCountryCodes.equals(ALL_COUNTRY_CODES);
    }
}
