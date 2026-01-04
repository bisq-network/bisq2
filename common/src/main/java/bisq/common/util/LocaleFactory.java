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

package bisq.common.util;

import java.util.Locale;

public class LocaleFactory {
    private LocaleFactory() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    @SuppressWarnings("deprecation")
    public static Locale from(String language, String country) {
        // We stick to the deprecated constructor as it is Android compatible
        return new Locale(language, country);
    }

    @SuppressWarnings("deprecation")
    public static Locale from(String language, String country, String variant) {
        // We stick to the deprecated constructor as it is Android compatible
        return new Locale(language, country, variant);
    }
}