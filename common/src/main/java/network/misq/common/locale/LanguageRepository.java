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

package network.misq.common.locale;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
public class LanguageRepository {
    private static final List<String> I18N_CODES = List.of(
            "en", // English
            "de", // German
            "es", // Spanish
            "pt", // Portuguese
            "pt-BR", // Portuguese (Brazil)
            "zh-Hans", // Chinese [Han Simplified]
            "zh-Hant", // Chinese [Han Traditional]
            "ru", // Russian
            "fr", // French
            "vi", // Vietnamese
            "th", // Thai
            "ja", // Japanese
            "fa", // Persian
            "it", // Italian
            "cs" // Czech
            /*
            // not translated yet
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

    public static final List<String> RTL_LANGUAGES_CODES = List.of(
            "fa", // Persian
            "ar", // Arabic
            "iw" // Hebrew
    );

    public static final List<String> CODES = LocaleRepository.LOCALES.stream()
            .filter(locale -> !locale.getLanguage().isEmpty() &&
                    !locale.getDisplayLanguage().isEmpty())
            .distinct()
            .map(Locale::getLanguage)
            .sorted(Comparator.comparing(LanguageRepository::getDisplayName))
            .collect(Collectors.toList());

    public static String getDisplayName(String code) {
        Locale locale = Locale.forLanguageTag(code);
        return locale.getDisplayName(locale);
    }

    public static String getEnglishCode() {
        return new Locale(Locale.ENGLISH.getLanguage()).getLanguage();
    }
}
