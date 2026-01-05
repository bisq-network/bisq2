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

import bisq.common.util.LocaleFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

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
        return ALL_COUNTRIES.stream()
                .filter(country -> country.getCode().equals(locale.getCountry()))
                .findAny();
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
        return LocaleFactory.from("", countryCode).getDisplayCountry(LocaleRepository.getDefaultLocale());
    }

    public static Country getCountry(String countryCode) {
        checkNotNull(countryCode, "countryCode must not be null");
        String regionCode = RegionRepository.getRegionCode(countryCode);
        checkNotNull(regionCode, "regionCode must not be null");
        Region region = new Region(regionCode, RegionRepository.getRegionName(regionCode));

        if (countryCode.equals("XK")) {
            return new Country(countryCode, CountryRepository.getNameByCode(countryCode), region);
        } else {
            return new Country(countryCode, getLocalizedCountryDisplayString(countryCode), region);
        }
    }

    public static List<Country> getCountriesFromCodes(List<String> countryCodes) {
        return countryCodes.stream()
                .map(CountryRepository::getCountry)
                .collect(Collectors.toList());
    }

    static {
        ALL_COUNTRIES = LocaleRepository.LOCALES.stream()
                .filter(Objects::nonNull)
                .map(locale -> {
                    try {
                        String countryCode = locale.getCountry();
                        checkNotNull(countryCode, "countryCode must not be null");
                        Region region = RegionRepository.getRegion(locale);
                        checkNotNull(region, "region must not be null for locale %s", locale);
                        if (countryCode.equals("XK")) {
                            return new Country(countryCode, CountryRepository.getNameByCode(countryCode), region);
                        } else {
                            return new Country(countryCode, locale.getDisplayCountry(), region);
                        }
                    } catch (Exception e) {
                        log.error("Could not create country for locale {}", locale, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Country::getName))
                .toList();

        ALL_COUNTRY_CODES = ALL_COUNTRIES.stream()
                .map(Country::getCode)
                .sorted()
                .toList();
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
