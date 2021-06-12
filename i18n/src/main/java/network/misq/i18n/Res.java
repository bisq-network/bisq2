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

package network.misq.i18n;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;

@Slf4j
public class Res {
    // Convenience methods
    public static String get(String key, Object... arguments) {
        return Default.get(key, arguments);
    }

    public static String get(String key) {
        return Default.get(key);
    }

    public static void setLocale(Locale locale) {
        Default.setLocale(locale);
    }


    public static class Default extends Base {
        private static Default INSTANCE = new Default(Locale.ROOT);

        private Default(Locale locale) {
            super(locale);
        }

        public static void setLocale(Locale locale) {
            INSTANCE = new Default(locale);
        }

        public static String get(String key, Object... arguments) {
            return MessageFormat.format(INSTANCE.getValue(key), arguments);
        }

        public static String get(String key) {
            try {
                return INSTANCE.getValue(key);
            } catch (MissingResourceException e) {
                log.warn("Missing resource for key: {}", key);
                e.printStackTrace();
                return key;
            }
        }
    }

    public static class Listing extends Base {
        private static Listing INSTANCE = new Listing(Locale.ROOT);

        private Listing(Locale locale) {
            super(locale);
        }

        public static void setLocale(Locale locale) {
            INSTANCE = new Listing(locale);
        }

        public static String get(String key, Object... arguments) {
            return MessageFormat.format(INSTANCE.getValue(key), arguments);
        }

        public static String get(String key) {
            try {
                return INSTANCE.getValue(key);
            } catch (MissingResourceException e) {
                log.warn("Missing resource for key: {}", key);
                e.printStackTrace();
                return key;
            }
        }
    }
}

