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

package network.misq.common.locale;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CountryRepository {
    public static final List<Country> COUNTRIES;

    static {
        COUNTRIES = LocaleRepository.LOCALES.stream()
                .map(locale -> {
                    String countryCode = locale.getCountry();
                    String regionCode = Region.getRegionCode(countryCode);
                    Region region = new Region(regionCode, Region.getRegionName(regionCode));
                    Country country = new Country(countryCode, locale.getDisplayCountry(), region);
                    if (locale.getCountry().equals("XK")) {
                        country = new Country(locale.getCountry(), "Republic of Kosovo", region);
                    }
                    return country;
                })
                .collect(Collectors.toList());
        COUNTRIES.add(new Country("GE", "Georgia", new Region("AS", Region.getRegionName("AS"))));
        COUNTRIES.add(new Country("BW", "Botswana", new Region("AF", Region.getRegionName("AF"))));
        COUNTRIES.add(new Country("IR", "Iran", new Region("AS", Region.getRegionName("AS"))));
        COUNTRIES.sort(Comparator.comparing(Country::name));
    }
}
