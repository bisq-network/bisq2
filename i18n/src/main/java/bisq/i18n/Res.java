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

import bisq.common.application.DevMode;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
public class Res {
    private static ResourceBundle defaultBundle;

    public static void initialize(Locale locale) {
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            locale = Locale.ROOT;
        }
        defaultBundle = ResourceBundle.getBundle("default", locale, new UTF8Control());
    }

    public static String get(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }

    public static String get(String key) {
        try {
            if (defaultBundle.containsKey(key)) {
                return defaultBundle.getString(key);
            } else if (DevMode.isDevMode()) {
                return "MISSING: " + key;
            } else {
                return "[" + key + "]";
            }
        } catch (MissingResourceException e) {
            log.warn("Missing resource for key: " + key, e);
            return key;
        }
    }

    public static boolean has(String key) {
        return defaultBundle.containsKey(key);
    }
}

