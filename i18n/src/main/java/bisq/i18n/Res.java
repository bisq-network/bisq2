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
import java.util.*;

@Slf4j
public class Res {
    private static ResourceBundle defaultBundle;
    private static final List<ResourceBundle> bundles = new ArrayList<>();

    public static void setLocale(Locale locale) {
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            locale = Locale.ROOT;
        }
        defaultBundle = ResourceBundle.getBundle("default", locale);
        bundles.addAll(List.of(defaultBundle,
                ResourceBundle.getBundle("application", locale),
                ResourceBundle.getBundle("payment_method", locale),
                ResourceBundle.getBundle("wallet", locale),
                ResourceBundle.getBundle("chat", locale),
                ResourceBundle.getBundle("trade_apps", locale),
                ResourceBundle.getBundle("bisq_easy", locale),
                ResourceBundle.getBundle("academy", locale),
                ResourceBundle.getBundle("user", locale),
                ResourceBundle.getBundle("authorized_role", locale),
                ResourceBundle.getBundle("settings", locale)
        ));
    }

    public static String get(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }

    public static String get(String key) {
        try {
            return bundles.stream()
                    .filter(bundle -> bundle.containsKey(key))
                    .map(bundle -> bundle.getString(key))
                    .findFirst()
                    .orElseGet(() -> {
                        if (DevMode.isDevMode()) {
                            log.error("Missing resource for key: {}", key);
                            return "MISSING: " + key;
                        } else {
                            return "[" + key + "!]";
                        }
                    });
        } catch (MissingResourceException e) {
            log.warn("Missing resource for key: " + key, e);
            return "[" + key + "!!!]";
        }
    }

    public static boolean has(String key) {
        return bundles.stream().anyMatch(bundle -> bundle.containsKey(key));
    }
}

