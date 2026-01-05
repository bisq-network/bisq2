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

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class LanguageRepository {
    // IETF BCP 47 language tag string.  E.g. pt-BR
    @Getter
    private static String defaultLanguageTag = "en";

    public static void setDefaultLanguageTag(String languageTag) {
        checkNotNull(languageTag, "languageTag must not be null at setDefaultLanguageTag");
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
    // Organized by strategic tiers based on: crypto adoption rates, internet user base,
    // GDP purchasing power, speaker population, and software localization market maturity
    public static final List<String> LANGUAGE_TAGS = List.of(
            // ============================================================================
            // TIER 1: Critical Markets
            // High crypto adoption, large internet user base, or established tech markets
            // Combined reach: ~3.5B speakers, dominant in crypto adoption rankings 2024
            // ============================================================================
            "en",      // English - Global lingua franca, 1.5B speakers, 25.9% internet users, dominant in crypto
            "zh-Hans", // Chinese (Simplified) - 1.2B speakers, 19% web content, China massive tech market
            "zh-Hant", // Chinese (Traditional) - Taiwan/Hong Kong, strong crypto adoption, 30%+ regional ownership
            "es",      // Spanish - 750M speakers, 3rd most used online, Latin America crypto growth leader
            "hi",      // Hindi - 609M speakers, India #1 in 2024 crypto adoption index (172% growth)
            "pt-BR",   // Portuguese (Brazil) - 230M speakers, Brazil top-10 crypto adoption, Latin America leader
            "ru",      // Russian - 258M speakers, strong Eastern Europe/Central Asia crypto presence
            "ja",      // Japanese - 125M speakers, mature tech market, high GDP per capita, institutional adoption
            "de",      // German - 134M speakers, strong EU economy, high purchasing power, established crypto market
            "fr",      // French - 280M speakers, EU + Africa presence, 29 countries official language
            "ko",      // Korean - 82M speakers, advanced tech market, high crypto trading volume
            "tr",      // Turkish - 88M speakers, Turkey 20%+ crypto ownership rate (2024)
            "id",      // Indonesian - 199M speakers, Indonesia #3 in 2024 global crypto adoption index
            "vi",      // Vietnamese - 85M speakers, Southeast Asia crypto growth, strong community
            "it",      // Italian - 85M speakers, EU economy, established software localization market
            "th",      // Thai - 69M speakers, Thailand strong Southeast Asia crypto hub
            "bn",      // Bengali - 265M speakers, Bangladesh/India, growing internet penetration
            "pl",      // Polish - 45M speakers, strong EU tech market, Central Europe gateway
            "nl",      // Dutch - 25M speakers, Netherlands crypto-friendly regulation, high GDP per capita

            // ============================================================================
            // TIER 2: Strategic Growth Markets
            // Moderate crypto adoption, regional importance, or significant diaspora
            // Target markets for expansion, established localization value
            // ============================================================================
            "pt-PT",   // Portuguese (Portugal) - 10M speakers, EU market, distinct from Brazilian Portuguese
            "tl",      // Tagalog - 45M+ speakers, Philippines remittance market, growing crypto use
            "ta",      // Tamil - 80M speakers, India/Sri Lanka/Singapore, strong diaspora, tech workforce
            "sv",      // Swedish - 13M speakers, Nordic region high GDP per capita, crypto-friendly
            "pcm",     // Nigerian Pidgin - 120M speakers, Nigeria #2 in 2024 crypto adoption, P2P leader
            "el",      // Greek - 13M speakers, EU member, historical crypto community presence
            "hu",      // Hungarian - 13M speakers, Central Europe, growing tech hub (Budapest)
            "ro",      // Romanian - 26M speakers, Eastern Europe tech outsourcing hub
            "no",      // Norwegian - 5.5M speakers, high GDP per capita, Nordic crypto-friendly regulations
            "fi",      // Finnish - 5.5M speakers, high GDP per capita, strong tech sector
            "cs",      // Czech - 13M speakers, Central Europe tech hub (Prague), established crypto community
            "af-ZA",   // Afrikaans - 7M speakers, South Africa crypto adoption
            "pa",      // Punjabi - 125M speakers, India/Pakistan diaspora, growing remittance use case
            "sw",      // Swahili - 200M speakers, East Africa (Kenya, Tanzania), mobile money ecosystem
            "ms",      // Malay - 290M speakers, Malaysia/Indonesia/Singapore, Southeast Asia regional

            // ============================================================================
            // TIER 3: Emerging & Specialized Markets
            // Smaller user bases but strategic regional coverage or community requests
            // Provides comprehensive global reach and serves diaspora communities
            // ============================================================================
            "bg",      // Bulgarian - 8M speakers, Eastern Europe EU member
            "da",      // Danish - 6M speakers, Nordic region completeness
            "sk",      // Slovak - 5.5M speakers, Central Europe EU member
            "hr",      // Croatian - 5M speakers, Balkans EU member
            "sl",      // Slovenian - 2.5M speakers, EU member, Alpine region coverage
            "et",      // Estonian - 1.1M speakers, Baltic EU member, digital society leader
            "lv",      // Latvian - 1.5M speakers, Baltic EU member
            "lt",      // Lithuanian - 2.8M speakers, Baltic EU member
            "is",      // Icelandic - 350K speakers, Nordic completeness, Bitcoin mining history
            "ga",      // Irish - 1.8M speakers, Ireland EU member, cultural preservation
            "sr",      // Serbian - 12M speakers, Balkans regional hub
            "be",      // Belarusian - 2.2M speakers, Eastern Europe coverage
            "sq",      // Albanian - 7.6M speakers, Balkans coverage (Albania/Kosovo)
            "ca",      // Catalan - 10M speakers, Spain regional language, Barcelona tech hub
            "mk",      // Macedonian - 2M speakers, Balkans regional completeness
            "kk",      // Kazakh - 13M speakers, Central Asia, Kazakhstan pro-crypto policies
            "km",      // Khmer - 16M speakers, Cambodia, Southeast Asia coverage
            "am",      // Amharic - 32M speakers, Ethiopia, East Africa coverage
            "ha",      // Hausa - 77M speakers, West Africa (Nigeria, Niger)
            "yo",      // Yoruba - 45M speakers, West Africa (Nigeria)
            "jv"       // Javanese - 82M speakers, Indonesia regional language
            /*
            Future RTL language candidates (not yet implemented due to technical requirements):
            | Code | Language        | Rationale                                                                              |
            | ---- | --------------- | -------------------------------------------------------------------------------------- |
            | ar   | Arabic          | 310M speakers, 20+ countries, UAE 30%+ crypto ownership, MENA critical market         |
            | fa   | Persian (Farsi) | 110M speakers, Iran/Afghanistan sanctions drive crypto adoption, remittance market     |
            | ur   | Urdu            | 160M speakers, Pakistan emerging crypto hub, remittance use case                       |
            */
    );

    public static boolean isDefaultLanguageRTL() {
        String languageCode = Locale.forLanguageTag(defaultLanguageTag).getLanguage();
        return RTL_LANGUAGES_CODES.contains(languageCode);
    }

    public static final List<String> RTL_LANGUAGES_CODES = List.of(
            "fa", // Persian
            "ar", // Arabic
            "he" // Hebrew
    );
}
