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
        // languageInLocale with country code would return: Portuguese (Brazil). We remove the brackets to get
        // Portuguese - Brazil, so that full string is: "Portuguese (Portuguese - Brazil)"
        String languageInLocale = getDisplayLanguageInLocale(locale).replace("(", " - ").replace(")", "");
        return getDisplayLanguage(locale) + " (" + languageInLocale + ")";
    }

    public static String getDisplayLanguage(String code) {
        return getDisplayLanguage(Locale.forLanguageTag(code));
    }

    // Returns language in defaut locale language (e.g. Spanish if "en" is default)
    public static String getDisplayLanguage(Locale locale) {
        return locale.getDisplayLanguage(Locale.forLanguageTag(defaultLanguageTag));
    }

    // Returns language in users OS default locale's language
    public static String getDisplayLanguageInLocale(Locale locale) {
        return locale.getDisplayName(Locale.getDefault());
    }

    // IETF BCP 47 language tag string.  E.g. pt-BR
    public static final List<String> LANGUAGE_TAGS = List.of(
            "en", // English
            "de", // German
            "es", // Spanish
            "it", // Italian
            "pt-BR", // Portuguese (Brazil)
            "cs", // Czech
            "pcm", // Nigerian Pidgin
            "ru", // Russian
            "af-ZA", // Afrikaans
            "pt-PT", // Portuguese
            "fr", // French
            "tl", // Tagalog
            "bn", // Bengali
            "ta", // Tamil
            "pl", // Polish
            "hi", // Hindi
            "zh-Hans", // Chinese (Simplified)
            "zh-Hant", // Chinese (Traditional)
            "sv", // Swedish
            "th", // Thai
            "tr", // Turkish
            "ja", // Japanese
            "ko", // Korean
            "vi", // Vietnamese
            "id", // Indonesian
            "nl", // Dutch
            "el", // Greek
            "hu", // Hungarian
            "ro", // Romanian
            "no", // Norwegian
            "fi", // Finnish
            "bg", // Bulgarian
            "da", // Danish
            "sk", // Slovak
            "hr", // Croatian
            "sl", // Slovenian
            "et", // Estonian
            "lv", // Latvian
            "lt", // Lithuanian
            "is", // Icelandic
            "ga" // Irish
            /*
            // not translated yet
            "fa", // Persian
            "sr", // Serbian
            "he", // Hebrew
            "be", // Belarusian
            "sq", // Albanian
            "ca", // Catalan
            "mk", // Macedonian
            "kk", // Kazakh (Kazakhstan and parts of China (Xinjiang), Mongolia, Uzbekistan, Russia, and Kyrgyzstan)
            "km", // Khmer (Cambodia, Thailand and Vietnam)
            "sw", // Swahili (Tanzania, Kenya, Uganda, Democratic Republic of the Congo, Mozambique, and parts of Rwanda, Burundi, and Comoros)
            "ms" // Malay (Malaysia , Brunei, Singapore, and Indonesia)

            Missing languages:
            | Code | Language        | Countries / Regions                                                                    | Estimated Speakers                                                                                     | Direction |
            | ---- | --------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | --------- |
            | pa   | Punjabi         | India (Punjab), Pakistan (Punjab region)                                               | ~90-125 million+ (depending on dialect) – general figure from world-language listings                  | LTR       |
            | am   | Amharic         | Ethiopia (official)                                                                    | ~57 million+ speakers                                                                                  | LTR       |
            | ha   | Hausa           | Nigeria (north), Niger, and wider West Africa                                          | ~72 million native + many L2 (100+m total)                                                             | LTR       |
            | yo   | Yoruba          | Nigeria, Benin, Togo                                                                   | ~45-50 million+ (various sources) – ranked in world-language listings                                  | LTR       |
            | jv   | Javanese        | Indonesia (Java)                                                                       | ~68-80 million+ speakers                                                                               | LTR       |

            Missing RTL languages:
            | Code | Language        | Countries / Regions                                                                    | Estimated Speakers                                                                                     | Direction |
            | ---- | --------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | --------- |
            | ar   | Arabic          | 20+ countries in Middle East & North Africa (e.g., Egypt, Saudi Arabia, Iraq, Algeria) | ~310 million native + many L2.                                                                         | RTL       |
            | fa   | Persian (Farsi) | Iran (official), Afghanistan (Dari), Tajikistan (Tajik)                                | ~70-110 million total speakers.                                                                        | RTL       |
            | ur   | Urdu            | Pakistan (official national), India (recognized), diaspora worldwide                   | ~100-160 million speakers (native + L2)                                                                | RTL       |
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
