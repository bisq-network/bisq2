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

package bisq.common.locale;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class LanguageRepository {
    @Getter
    @Setter
    private static String defaultLanguage = "en";

    public static final List<String> CODES = LocaleRepository.LOCALES.stream()
            .filter(locale -> !locale.getLanguage().isEmpty() &&
                    !locale.getDisplayLanguage().isEmpty())
            .map(Locale::getLanguage)
            .filter(Objects::nonNull)
            .distinct()
            .sorted(Comparator.comparing(LanguageRepository::getDisplayString))
            .collect(Collectors.toList());

    public static String getDisplayString(String code) {
        Locale locale = Locale.forLanguageTag(code);
        return getDisplayLanguage(locale) + " (" + getDisplayLanguageInLocale(locale) + ")";
    }

    public static String getDisplayLanguage(String code) {
        return getDisplayLanguage(Locale.forLanguageTag(code));
    }

    // Returns language in defaut locale language (e.g. Spanish if "en" is default)
    public static String getDisplayLanguage(Locale locale) {
        return locale.getDisplayLanguage(Locale.forLanguageTag(defaultLanguage));
    }

    // Returns language in locale's language (e.g. español)
    public static String getDisplayLanguageInLocale(Locale locale) {
        return locale.getDisplayName(locale);
    }

    public static final List<String> I18N_CODES = List.of(
            "en", // English
            "de", // German
            "es", // Spanish
            "it", // Italian
            "pt-BR", // Portuguese (Brazil)
            "cs", // Czech
            "pcm" // Nigerian Pidgin
            /*
            // not translated yet
            "pt", // Portuguese
            "zh-Hans", // Chinese [Han Simplified]
            "zh-Hant", // Chinese [Han Traditional]
            "ru", // Russian
            "fr", // French
            "vi", // Vietnamese
            "th", // Thai
            "ja", // Japanese
            "fa", // Persian
            "el", // Greek
            "sr-Latn-RS", // Serbian [Latin] (Serbia)
            "hu", // Hungarian
            "ro", // Romanian
            "tr" // Turkish
            "iw", // Hebrew
            "hi", // Hindi
            "ko", // Korean
            "pl", // Polish
            "sv", // Swedish
            "no", // Norwegian
            "nl", // Dutch
            "be", // Belarusian
            "fi", // Finnish
            "bg", // Bulgarian
            "lt", // Lithuanian
            "lv", // Latvian
            "hr", // Croatian
            "uk", // Ukrainian
            "sk", // Slovak
            "sl", // Slovenian
            "ga", // Irish
            "sq", // Albanian
            "ca", // Catalan
            "mk", // Macedonian
            "kk", // Kazakh
            "km", // Khmer
            "sw", // Swahili
            "in", // Indonesian
            "ms", // Malay
            "is", // Icelandic
            "et", // Estonian
            "ar", // Arabic
            "vi", // Vietnamese
            "th", // Thai
            "da", // Danish
            "mt"  // Maltese
            */
    );

    public static boolean isDefaultLanguageRTL() {
        return RTL_LANGUAGES_CODES.contains(defaultLanguage);
    }

    public static final List<String> RTL_LANGUAGES_CODES = List.of(
            "fa", // Persian
            "ar", // Arabic
            "iw" // Hebrew
    );
}
