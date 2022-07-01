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

package bisq.application;

import bisq.common.locale.LocaleRepository;
import bisq.common.options.PropertiesReader;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Locale;
import java.util.Properties;

@Getter
@ToString
@EqualsAndHashCode
public final class ApplicationConfig {
    private final String baseDir;
    private final String appName;
    private final String version;
    private final boolean devMode;
    private final boolean isBitcoindRegtest;
    private final boolean isElementsdRegtest;

    public ApplicationConfig(String baseDir,
                             String appName,
                             String version,
                             boolean devMode,
                             boolean isBitcoindRegtest,
                             boolean isElementsdRegtest) {
        this.baseDir = baseDir;
        this.appName = appName;
        this.version = version;
        this.devMode = devMode;
        this.isBitcoindRegtest = isBitcoindRegtest;
        this.isElementsdRegtest = isElementsdRegtest;
    }

    // To ensure the locale is set initially we should write it to property file instead of persisting it in
    // preferences which might be read out to a later moment.
    public Locale getLocale() {
        Properties properties = PropertiesReader.getProperties("bisq.properties");
        if (properties == null) {
            return LocaleRepository.getDefaultLocale();
        }
        Locale locale = new Locale(properties.getProperty("language"), properties.getProperty("country"));
        if (LocaleRepository.isLocaleInvalid(locale)) {
            return Locale.US;
        } else {
            return locale;
        }
    }
}