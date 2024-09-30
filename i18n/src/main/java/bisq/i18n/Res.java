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
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class Res {
    // We use non-printing characters as separator. See: https://en.wikipedia.org/wiki/Delimiter#ASCII_delimited_text
    private static final char ARGS_SEPARATOR = 0x1f;
    private static final char PARAM_SEPARATOR = 0x1e;

    private static final List<String> BUNDLE_NAMES = List.of(
            "default",
            "application",
            "bisq_easy",
            "reputation",
            "trade_apps",
            "academy",
            "chat",
            "support",
            "user",
            "network",
            "settings",
            "wallet",
            "authorized_role",
            "payment_method"
    );

    private static final List<ResourceBundle> bundles = new ArrayList<>();

    public static void setLanguage(String languageCode) {
        Locale locale = "en".equalsIgnoreCase(languageCode) ? new Locale("") : Locale.forLanguageTag(languageCode);

        bundles.clear();

        bundles.addAll(
                BUNDLE_NAMES.stream()
                        .map(bundleName -> ResourceBundle.getBundle(bundleName, locale))
                        .toList()
        );
    }

    public static String get(String key, Object... arguments) {
        return MessageFormat.format(get(key), arguments);
    }

    public static String get(String key) {
        checkArgument(!bundles.isEmpty(), "Res.get cannot be called as bundles is still empty. key=" + key);
        try {
            return bundles.stream()
                    .filter(bundle -> bundle.containsKey(key))
                    .map(bundle -> bundle.getString(key))
                    .findFirst()
                    .orElseGet(() -> {
                        log.error("Missing resource for key: {}", key);
                        if (DevMode.isDevMode()) {
                            return "MISSING: " + key;
                        } else {
                            return "[" + key + "!]";
                        }
                    });
        } catch (MissingResourceException e) {
            log.warn("Missing resource for key: {}", key, e);
            return "[" + key + "!!!]";
        }
    }

    /**
     * Expecting to have i18n keys with '.1' and '.*' postfix for singular and plural handling.
     * Additionally, a '.0' postfix handles 0 values.
     */
    public static String getPluralization(String key, double number) {
        String pluralKey;
        if (number == 1) {
            pluralKey = key + ".1";
        } else {
            if (number == 0 && has(key + ".0")) {
                pluralKey = key + ".0";
            } else {
                pluralKey = key + ".*";
            }
        }
        return get(pluralKey, number);
    }

    public static boolean has(String key) {
        return bundles.stream().anyMatch(bundle -> bundle.containsKey(key));
    }


    public static String encode(String key, Object... arguments) {
        if (arguments.length == 0) {
            return key;
        }

        String args = Joiner.on(ARGS_SEPARATOR).join(arguments);
        return key + PARAM_SEPARATOR + args;
    }

    public static String decode(String encoded) {
        String separator = String.valueOf(Res.PARAM_SEPARATOR);
        if (!encoded.contains(separator)) {
            if (Res.has(encoded)) {
                return Res.get(encoded);
            } else {
                // If we get a log message from an old node we get the resolved string
                return encoded;
            }
        }

        String[] tokens = encoded.split(separator);
        String key = tokens[0];
        if (tokens.length == 1) {
            return Res.get(key);
        }

        String argumentList = tokens[1];
        Object[] arguments = argumentList.split(String.valueOf(ARGS_SEPARATOR));
        return Res.get(key, arguments);
    }
}

