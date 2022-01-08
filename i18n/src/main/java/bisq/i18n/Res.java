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

package bisq.i18n;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
public class Res {
    public static Res common, network, offerbook;
    private final String resourceName;

    public static void initialize(Locale locale) {
        common = new Res(locale, "default");
        network = new Res(locale, "network");
        offerbook = new Res(locale, "offerbook");
    }

    private final ResourceBundle resourceBundle;

    private Res(Locale locale, String resourceName) {
        this.resourceName = resourceName;
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            locale = Locale.ROOT;
        }
        resourceBundle = ResourceBundle.getBundle(resourceName, locale, new UTF8Control());
    }

    public String get(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }

    public String get(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing resource for key: " + resourceName + "." + key, e);
            return key;
        }
    }
}

