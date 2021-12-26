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

package network.misq.application.options;

import network.misq.common.options.PropertiesReader;

import java.util.Locale;
import java.util.Properties;

public record ApplicationOptions(String appDir, String appName) {
    // To ensure the locale is set initially we should write it to property file instead of persisting it in 
    // preferences which might be read out to a later moment.
    public Locale getLocale() {
        Properties properties = PropertiesReader.getProperties("misq.properties");
        if (properties == null) {
            return Locale.getDefault();
        }
        String language = properties.getProperty("language");
        String country = properties.getProperty("country");
        if (language == null || country == null) {
            return Locale.getDefault();
        }

        return new Locale(language, country);
    }
}