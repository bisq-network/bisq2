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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class Res {
    // We use non-printing characters as separator. See: https://en.wikipedia.org/wiki/Delimiter#ASCII_delimited_text
    private static final char ARGS_SEPARATOR = 0x1f;
    private static final char PARAM_SEPARATOR = 0x1e;

    // We include also webcam to get support from the AI translation pipeline.
    // It would cause extra effort to apply the automated translations to those projects.
    public static final List<String> DEFAULT_BUNDLE_NAMES = List.of(
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
            "account",
            "mu_sig",
            "webcam",
            "bi2p"
    );

    private static volatile List<ResourceBundle> bundles = List.of();
    private static volatile List<String> bundleNames = DEFAULT_BUNDLE_NAMES;
    private static volatile Locale locale = Locale.US;


    /* --------------------------------------------------------------------- */
    // Config
    /* --------------------------------------------------------------------- */

    public static void setBundleNames(List<String> bundleNames) {
        Res.bundleNames = bundleNames;
    }

    public static void setAndApplyBundleNames(List<String> bundleNames) {
        setBundleNames(bundleNames);
        updateBundles();
    }

    public static void setLanguageTag(String languageTag) {
        // We use Locale.ROOT so that the i18n file without language suffix is used. in our case that's english.
        locale = "en".equalsIgnoreCase(languageTag) ? Locale.ROOT : Locale.forLanguageTag(languageTag);
    }

    public static void setAndApplyLanguageTag(String languageTag) {
        setLanguageTag(languageTag);
        updateBundles();
    }

    private static final ResourceBundle.Control BCP47_CONTROL = new Bcp47ResourceBundleControl();

    public static void updateBundles() {
        // Use collectors to avoid Samsung devices crashes (not fully supporting Java 16+ APIs)
        var newBundles = new ArrayList<ResourceBundle>(bundleNames.size());
        for (String bundleName : bundleNames) {
            try {
                newBundles.add(ResourceBundle.getBundle(bundleName, locale, BCP47_CONTROL));
            } catch (MissingResourceException e) {
                log.warn("Resource bundle '{}' not found for locale {}. Skipping.", bundleName, locale);
            }
        }
        bundles = List.copyOf(newBundles);
    }

    /**
     * Custom ResourceBundle.Control that handles BCP 47 language tags with script subtags.
     *
     * <h2>Problem</h2>
     * Java's default ResourceBundle.Control converts locales like {@code zh-Hans} to
     * {@code zh__#Hans} format for property filenames (using {@link Locale#toString()}),
     * but Transifex and our file naming convention use {@code zh-Hans} (hyphenated BCP 47
     * format).
     *
     * <h2>Solution</h2>
     * This control overrides {@link #toBundleName(String, Locale)} and
     * {@link #getCandidateLocales(String, Locale)} to preserve the BCP 47 format
     * ({@link Locale#toLanguageTag()}) for script-based locales, while using the
     * default behavior for standard language-country locales.
     *
     * <h2>Affected Locales</h2>
     * <ul>
     *   <li>{@code zh-Hans} (Chinese Simplified) → {@code default_zh-Hans.properties}</li>
     *   <li>{@code zh-Hant} (Chinese Traditional) → {@code default_zh-Hant.properties}</li>
     *   <li>Future script-based locales (e.g., {@code sr-Latn}, {@code sr-Cyrl})</li>
     * </ul>
     *
     * <h2>Thread Safety</h2>
     * This class is thread-safe. It contains no mutable state and all methods are pure
     * functions operating only on their parameters.
     *
     * <h2>Performance</h2>
     * This instance is used as a singleton (stored in a static final field) to
     * avoid unnecessary object allocation during bundle loading.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/bcp/bcp47.txt">BCP 47 - Tags for Identifying Languages</a>
     * @see <a href="https://www.unicode.org/reports/tr35/#Locale_Extension_Key_and_Type_Data">LDML - Unicode Locale Data Markup Language</a>
     */
    private static class Bcp47ResourceBundleControl extends ResourceBundle.Control {
        /**
         * Generates the candidate locales for bundle lookup, preserving BCP 47 format throughout the fallback chain.
         *
         * <p>For script-based locales, this ensures the fallback chain maintains hyphenated format:
         * <ul>
         *   <li>{@code zh-Hant-HK} → {@code zh-Hant} → {@code zh} → root</li>
         *   <li>{@code zh-Hans-CN} → {@code zh-Hans} → {@code zh} → root</li>
         * </ul>
         *
         * <p>For standard locales, uses Java's default fallback:
         * <ul>
         *   <li>{@code pt-BR} → {@code pt} → root</li>
         *   <li>{@code en-US} → {@code en} → root</li>
         * </ul>
         *
         * @param baseName the base bundle name
         * @param locale the locale for which a resource bundle should be loaded
         * @return a list of candidate locales for bundle lookup
         */
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (locale.equals(Locale.ROOT)) {
                return List.of(Locale.ROOT);
            }

            String language = locale.getLanguage();
            String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();

            // For script-based locales, generate proper BCP 47 fallback chain
            if (!script.isEmpty()) {
                List<Locale> candidates = new ArrayList<>(4);

                // Full locale (e.g., zh-Hant-HK)
                candidates.add(locale);

                // Language + Script (e.g., zh-Hant from zh-Hant-HK)
                if (!country.isEmpty() || !variant.isEmpty()) {
                    candidates.add(new Locale.Builder()
                            .setLanguage(language)
                            .setScript(script)
                            .build());
                }

                // Language only (e.g., zh)
                candidates.add(Locale.of(language));

                // Root (default)
                candidates.add(Locale.ROOT);

                return candidates;
            }

            // For standard locales, use default behavior
            return super.getCandidateLocales(baseName, locale);
        }

        /**
         * Converts a base name and locale to a bundle name following BCP 47 conventions.
         *
         * <p>For script-based locales (locales with {@link Locale#getScript()} non-empty),
         * this returns {@code baseName + "_" + locale.toLanguageTag()}, preserving the
         * BCP 47 hyphenated format (e.g., {@code zh-Hans}, {@code zh-Hant}).
         *
         * <p>For standard locales, delegates to the superclass implementation which uses
         * {@link Locale#toString()} format (e.g., {@code pt_BR}, {@code en_US}).
         *
         * @param baseName the base bundle name (e.g., "default", "chat")
         * @param locale the locale for which a resource bundle should be loaded
         * @return the bundle name for the given base name and locale
         */
        @Override
        public String toBundleName(String baseName, Locale locale) {
            if (locale == Locale.ROOT) {
                return baseName;
            }

            // BCP 47 locales with script subtags (with or without region)
            // Examples: zh-Hans, zh-Hant, zh-Hans-CN, sr-Latn, sr-Cyrl
            if (!locale.getScript().isEmpty()) {
                return baseName + "_" + locale.toLanguageTag();
            }

            // Standard language-country locales (pt-BR, af-ZA, en-US, etc.)
            // Uses Java's default naming: language_COUNTRY
            return super.toBundleName(baseName, locale);
        }
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

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

