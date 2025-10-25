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
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class LanguageRepository {
    public static void setDefaultLanguageTag(String languageTag) {
        if (LANGUAGE_TAGS.contains(languageTag)) {
            defaultLanguageTag = languageTag;
        } else {
            // If we don't have the language with specified region we fall back to the languageCode
            String languageCode = Locale.forLanguageTag(languageTag).getLanguage();
            // Find any supported tag matching this language code
            defaultLanguageTag = LANGUAGE_TAGS.stream()
                    .filter(tag -> Locale.forLanguageTag(tag).getLanguage().equals(languageCode))
                    .findFirst()
                    .orElse("en");
        }
    }

    // IETF BCP 47 language tag string.  E.g. pt-BR
    @Getter
    private static String defaultLanguageTag = "en";

    public static final List<String> LANGUAGE_CODES = LocaleRepository.LOCALES.stream()
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
        return locale.getDisplayLanguage(Locale.forLanguageTag(defaultLanguageTag));
    }

    // Returns language in locale's language (e.g. español)
    public static String getDisplayLanguageInLocale(Locale locale) {
        return locale.getDisplayName(locale);
    }

    // IETF BCP 47 language tag string.  E.g. pt-BR
    public static final List<String> LANGUAGE_TAGS = List.of(
            "en", // English
            "de", // German
            "es", // Spanish
            "it", // Italian
            "pt-BR", // Portuguese (Brazil)
            "cs", // Czech
            "pcm-NG", // Nigerian Pidgin
            "ru", // Russian
            "af-ZA" // Afrikaans
            /*
            // not translated yet
            "pt", // Portuguese
            "zh-Hans", // Chinese [Han Simplified] -> zh-Hans-CN to support country
            "zh-Hant", // Chinese [Han Traditional]-> zh-Hant-CN to support country
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
        String languageCode = Locale.forLanguageTag(defaultLanguageTag).getLanguage();
        return RTL_LANGUAGES_CODES.contains(languageCode);
    }

    public static final List<String> RTL_LANGUAGES_CODES = List.of(
            "fa", // Persian
            "ar", // Arabic
            "iw" // Hebrew
    );
}
