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

import java.util.*;
import java.util.stream.Collectors;

public class CountryRepository {
    @Getter
    private static Country defaultCountry;

    public static void setDefaultCountry(Country defaultCountry) {
        CountryRepository.defaultCountry = defaultCountry;
    }

    public static void applyDefaultLocale(Locale defaultLocale) {
        CountryRepository.defaultCountry = findCountry(defaultLocale).orElse(findCountry(Locale.US).orElseThrow());
    }

    public static Optional<Country> findCountry(Locale locale) {
        return countries.stream().filter(c -> c.getCode().equals(locale.getCountry())).findAny();
    }

    public static List<Country> getCountries() {
        return countries;
    }

    private static final List<Country> countries;

    public static String getNameByCode(String countryCode) {
        if (countryCode.equals("XK"))
            return "Republic of Kosovo";
        else
            return new Locale(LanguageRepository.getDefaultLanguage(), countryCode).getDisplayCountry();
    }

    public static List<Country> getCountriesFromCodes(List<String> codes) {
        List<Country> list = new ArrayList<>();
        for (String code : codes) {
            Locale locale = new Locale(LanguageRepository.getDefaultLanguage(), code, "");
            String countryCode = locale.getCountry();
            String regionCode = RegionRepository.getRegionCode(countryCode);
            Region region = new Region(regionCode, RegionRepository.getRegionName(regionCode));
            Country country = new Country(countryCode, locale.getDisplayCountry(), region);
            if (countryCode.equals("XK")) {
                country = new Country(countryCode, CountryRepository.getNameByCode(countryCode), region);
            }
            list.add(country);
        }
        return list;
    }

    static {
        countries = LocaleRepository.LOCALES.stream()
                .map(locale -> {
                    String countryCode = locale.getCountry();
                    Region region = RegionRepository.getRegion(locale);
                    Country country = new Country(countryCode, locale.getDisplayCountry(), region);
                    if (locale.getCountry().equals("XK")) {
                        country = new Country(locale.getCountry(), "Republic of Kosovo", region);
                    }
                    return country;
                })
                .collect(Collectors.toList());
        countries.add(new Country("GE", "Georgia", new Region("AS", RegionRepository.getRegionName("AS"))));
        countries.add(new Country("BW", "Botswana", new Region("AF", RegionRepository.getRegionName("AF"))));
        countries.add(new Country("IR", "Iran", new Region("AS", RegionRepository.getRegionName("AS"))));
        countries.sort(Comparator.comparing(Country::getName));
    }


}
