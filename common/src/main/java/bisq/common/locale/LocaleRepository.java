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

import bisq.common.file.PropertiesReader;
import bisq.common.util.StringUtils;
import bisq.common.util.LocaleFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class LocaleRepository {
    @Getter
    private static Locale defaultLocale = Locale.US;

    static {
        String propertyFileName = "bisq.properties";
        Locale locale = Locale.getDefault();
        try {
            Properties properties = PropertiesReader.getProperties(propertyFileName);
            String language = properties.getProperty("language"); // e.g., "en"
            String country = properties.getProperty("country");   // e.g., "US"
            String countryTag = StringUtils.isNotEmpty(country) ? "-" + country : "";
            String tag = language + countryTag;
            locale = Locale.forLanguageTag(tag);
        } catch (FileNotFoundException ignore) {
            log.debug("No {} property file found. This is expected", propertyFileName);
        } catch (IOException e) {
            log.warn("Could not load properties from {}", propertyFileName);
        } finally {
            setDefaultLocale(locale);
        }
    }

    public static void setDefaultLocale(Locale locale) {
        checkNotNull(locale, "locale must not be null at setDefaultLocale");
        LocaleRepository.defaultLocale = locale;
    }

    /**
     * Ensures a locale is suitable for use in Bisq's locale-dependent systems.
     *
     * <p>For historical reasons, Bisq requires most locales to have a country code
     * to ensure proper currency and number formatting. Script-based locales (e.g.,
     * {@code zh-Hans}, {@code zh-Hant}) are exempt from this requirement as they are
     * valid BCP 47 language tags without countries.
     *
     * <p>If a locale has neither country nor script, this method attempts to add
     * a country code from the current locale, or falls back to {@code Locale.US}.
     *
     * <h2>Examples</h2>
     * <ul>
     *   <li>{@code zh-Hans} → returned as-is (has script)</li>
     *   <li>{@code pt} (no country, no script) → {@code pt-BR} if current locale is Brazil</li>
     *   <li>{@code en} (no country, no script) → {@code en-US} fallback</li>
     * </ul>
     *
     * @param locale the locale to validate
     * @return a locale with country code, or the original if it has a script
     * @see <a href="https://www.rfc-editor.org/rfc/bcp/bcp47.txt">BCP 47 - Tags for Identifying Languages</a>
     */
    public static Locale ensureValidLocale(Locale locale) {
        checkNotNull(locale, "locale must not be null at ensureValidLocale");
        // Script-based locales (e.g., zh-Hans, zh-Hant) don't have a country code
        // but are still valid BCP 47 language tags. Only require country for
        // locales that don't have a script subtag.
        if (locale.getCountry().isEmpty() && locale.getScript().isEmpty()) {
            log.info("Locale has no country defined. locale={}", locale);
            Locale currentLocale = LocaleRepository.getDefaultLocale();

            if (!locale.getLanguage().isEmpty() && !currentLocale.getCountry().isEmpty()) {
                log.warn("Locale has no country defined. We apply the country from the current locale.");
                return LocaleFactory.from(locale.getLanguage(), currentLocale.getCountry());
            } else {
                log.info("Could not set the new locale, we fall back to Locale.US");
                return Locale.US;
            }
        } else {
            return locale;
        }
    }

    // Data from https://restcountries.eu/rest/v2/all?fields=name;region;subregion;alpha2Code;languages
    public static final Set<Locale> LOCALES = Set.of(
            LocaleFactory.from("ps", "AF"), // Afghanistan / lang=Pashto
            LocaleFactory.from("sv", "AX"), // Åland Islands / lang=Swedish
            LocaleFactory.from("sq", "AL"), // Albania / lang=Albanian
            LocaleFactory.from("ar", "DZ"), // Algeria / lang=Arabic
            LocaleFactory.from("en", "AS"), // American Samoa / lang=English
            LocaleFactory.from("ca", "AD"), // Andorra / lang=Catalan
            LocaleFactory.from("pt", "AO"), // Angola / lang=Portuguese
            LocaleFactory.from("en", "AI"), // Anguilla / lang=English
            LocaleFactory.from("en", "AG"), // Antigua and Barbuda / lang=English
            LocaleFactory.from("es", "AR"), // Argentina / lang=Spanish
            LocaleFactory.from("hy", "AM"), // Armenia / lang=Armenian
            LocaleFactory.from("nl", "AW"), // Aruba / lang=Dutch
            LocaleFactory.from("en", "AU"), // Australia / lang=English
            LocaleFactory.from("de", "AT"), // Austria / lang=German
            LocaleFactory.from("az", "AZ"), // Azerbaijan / lang=Azerbaijani
            LocaleFactory.from("en", "BS"), // Bahamas / lang=English
            LocaleFactory.from("ar", "BH"), // Bahrain / lang=Arabic
            LocaleFactory.from("bn", "BD"), // Bangladesh / lang=Bengali
            LocaleFactory.from("en", "BB"), // Barbados / lang=English
            LocaleFactory.from("be", "BY"), // Belarus / lang=Belarusian
            LocaleFactory.from("nl", "BE"), // Belgium / lang=Dutch
            LocaleFactory.from("en", "BZ"), // Belize / lang=English
            LocaleFactory.from("fr", "BJ"), // Benin / lang=French
            LocaleFactory.from("en", "BM"), // Bermuda / lang=English
            LocaleFactory.from("dz", "BT"), // Bhutan / lang=Dzongkha
            LocaleFactory.from("es", "BO"), // Bolivia (Plurinational State of) / lang=Spanish
            LocaleFactory.from("nl", "BQ"), // Bonaire, Sint Eustatius and Saba / lang=Dutch
            LocaleFactory.from("bs", "BA"), // Bosnia and Herzegovina / lang=Bosnian
            LocaleFactory.from("en", "BW"), // Botswana / lang=English
            LocaleFactory.from("pt", "BR"), // Brazil / lang=Portuguese
            LocaleFactory.from("en", "IO"), // British Indian Ocean Territory / lang=English
            LocaleFactory.from("en", "UM"), // United States Minor Outlying Islands / lang=English
            LocaleFactory.from("en", "VG"), // Virgin Islands (British) / lang=English
            LocaleFactory.from("en", "VI"), // Virgin Islands (U.S.) / lang=English
            LocaleFactory.from("ms", "BN"), // Brunei Darussalam / lang=Malay
            LocaleFactory.from("bg", "BG"), // Bulgaria / lang=Bulgarian
            LocaleFactory.from("fr", "BF"), // Burkina Faso / lang=French
            LocaleFactory.from("fr", "BI"), // Burundi / lang=French
            LocaleFactory.from("km", "KH"), // Cambodia / lang=Khmer
            LocaleFactory.from("en", "CM"), // Cameroon / lang=English
            LocaleFactory.from("en", "CA"), // Canada / lang=English
            LocaleFactory.from("pt", "CV"), // Cabo Verde / lang=Portuguese
            LocaleFactory.from("en", "KY"), // Cayman Islands / lang=English
            LocaleFactory.from("fr", "CF"), // Central African Republic / lang=French
            LocaleFactory.from("fr", "TD"), // Chad / lang=French
            LocaleFactory.from("es", "CL"), // Chile / lang=Spanish
            LocaleFactory.from("zh", "CN"), // China / lang=Chinese
            LocaleFactory.from("en", "CX"), // Christmas Island / lang=English
            LocaleFactory.from("en", "CC"), // Cocos (Keeling) Islands / lang=English
            LocaleFactory.from("es", "CO"), // Colombia / lang=Spanish
            LocaleFactory.from("ar", "KM"), // Comoros / lang=Arabic
            LocaleFactory.from("fr", "CG"), // Congo / lang=French
            LocaleFactory.from("fr", "CD"), // Congo (Democratic Republic of the) / lang=French
            LocaleFactory.from("en", "CK"), // Cook Islands / lang=English
            LocaleFactory.from("es", "CR"), // Costa Rica / lang=Spanish
            LocaleFactory.from("hr", "HR"), // Croatia / lang=Croatian
            LocaleFactory.from("es", "CU"), // Cuba / lang=Spanish
            LocaleFactory.from("nl", "CW"), // Curaçao / lang=Dutch
            LocaleFactory.from("el", "CY"), // Cyprus / lang=Greek (modern)
            LocaleFactory.from("cs", "CZ"), // Czech Republic / lang=Czech
            LocaleFactory.from("da", "DK"), // Denmark / lang=Danish
            LocaleFactory.from("fr", "DJ"), // Djibouti / lang=French
            LocaleFactory.from("en", "DM"), // Dominica / lang=English
            LocaleFactory.from("es", "DO"), // Dominican Republic / lang=Spanish
            LocaleFactory.from("es", "EC"), // Ecuador / lang=Spanish
            LocaleFactory.from("ar", "EG"), // Egypt / lang=Arabic
            LocaleFactory.from("es", "SV"), // El Salvador / lang=Spanish
            LocaleFactory.from("es", "GQ"), // Equatorial Guinea / lang=Spanish
            LocaleFactory.from("ti", "ER"), // Eritrea / lang=Tigrinya
            LocaleFactory.from("et", "EE"), // Estonia / lang=Estonian
            LocaleFactory.from("am", "ET"), // Ethiopia / lang=Amharic
            LocaleFactory.from("en", "FK"), // Falkland Islands (Malvinas) / lang=English
            LocaleFactory.from("fo", "FO"), // Faroe Islands / lang=Faroese
            LocaleFactory.from("en", "FJ"), // Fiji / lang=English
            LocaleFactory.from("fi", "FI"), // Finland / lang=Finnish
            LocaleFactory.from("fr", "FR"), // France / lang=French
            LocaleFactory.from("fr", "GF"), // French Guiana / lang=French
            LocaleFactory.from("fr", "PF"), // French Polynesia / lang=French
            LocaleFactory.from("fr", "TF"), // French Southern Territories / lang=French
            LocaleFactory.from("fr", "GA"), // Gabon / lang=French
            LocaleFactory.from("en", "GM"), // Gambia / lang=English
            LocaleFactory.from("ka", "GE"), // Georgia / lang=Georgian
            LocaleFactory.from("de", "DE"), // Germany / lang=German
            LocaleFactory.from("en", "GH"), // Ghana / lang=English
            LocaleFactory.from("en", "GI"), // Gibraltar / lang=English
            LocaleFactory.from("el", "GR"), // Greece / lang=Greek (modern)
            LocaleFactory.from("kl", "GL"), // Greenland / lang=Kalaallisut
            LocaleFactory.from("en", "GD"), // Grenada / lang=English
            LocaleFactory.from("fr", "GP"), // Guadeloupe / lang=French
            LocaleFactory.from("en", "GU"), // Guam / lang=English
            LocaleFactory.from("es", "GT"), // Guatemala / lang=Spanish
            LocaleFactory.from("en", "GG"), // Guernsey / lang=English
            LocaleFactory.from("fr", "GN"), // Guinea / lang=French
            LocaleFactory.from("pt", "GW"), // Guinea-Bissau / lang=Portuguese
            LocaleFactory.from("en", "GY"), // Guyana / lang=English
            LocaleFactory.from("fr", "HT"), // Haiti / lang=French
            LocaleFactory.from("la", "VA"), // Holy See / lang=Latin
            LocaleFactory.from("es", "HN"), // Honduras / lang=Spanish
            LocaleFactory.from("en", "HK"), // Hong Kong / lang=English
            LocaleFactory.from("hu", "HU"), // Hungary / lang=Hungarian
            LocaleFactory.from("is", "IS"), // Iceland / lang=Icelandic
            LocaleFactory.from("hi", "IN"), // India / lang=Hindi
            LocaleFactory.from("id", "ID"), // Indonesia / lang=Indonesian
            LocaleFactory.from("fr", "CI"), // Côte d'Ivoire / lang=French
            LocaleFactory.from("fa", "IR"), // Iran (Islamic Republic of) / lang=Persian (Farsi)
            LocaleFactory.from("ar", "IQ"), // Iraq / lang=Arabic
            LocaleFactory.from("ga", "IE"), // Ireland / lang=Irish
            LocaleFactory.from("en", "IM"), // Isle of Man / lang=English
            LocaleFactory.from("he", "IL"), // Israel / lang=Hebrew (modern)
            LocaleFactory.from("it", "IT"), // Italy / lang=Italian
            LocaleFactory.from("en", "JM"), // Jamaica / lang=English
            LocaleFactory.from("ja", "JP"), // Japan / lang=Japanese
            LocaleFactory.from("en", "JE"), // Jersey / lang=English
            LocaleFactory.from("ar", "JO"), // Jordan / lang=Arabic
            LocaleFactory.from("kk", "KZ"), // Kazakhstan / lang=Kazakh
            LocaleFactory.from("en", "KE"), // Kenya / lang=English
            LocaleFactory.from("en", "KI"), // Kiribati / lang=English
            LocaleFactory.from("ar", "KW"), // Kuwait / lang=Arabic
            LocaleFactory.from("ky", "KG"), // Kyrgyzstan / lang=Kyrgyz
            LocaleFactory.from("lo", "LA"), // Lao People's Democratic Republic / lang=Lao
            LocaleFactory.from("lv", "LV"), // Latvia / lang=Latvian
            LocaleFactory.from("ar", "LB"), // Lebanon / lang=Arabic
            LocaleFactory.from("en", "LS"), // Lesotho / lang=English
            LocaleFactory.from("en", "LR"), // Liberia / lang=English
            LocaleFactory.from("ar", "LY"), // Libya / lang=Arabic
            LocaleFactory.from("de", "LI"), // Liechtenstein / lang=German
            LocaleFactory.from("lt", "LT"), // Lithuania / lang=Lithuanian
            LocaleFactory.from("fr", "LU"), // Luxembourg / lang=French
            LocaleFactory.from("zh", "MO"), // Macao / lang=Chinese
            LocaleFactory.from("mk", "MK"), // Macedonia (the former Yugoslav Republic of) / lang=Macedonian
            LocaleFactory.from("fr", "MG"), // Madagascar / lang=French
            LocaleFactory.from("en", "MW"), // Malawi / lang=English
            LocaleFactory.from("en", "MY"), // Malaysia / lang=Malaysian
            LocaleFactory.from("dv", "MV"), // Maldives / lang=Divehi
            LocaleFactory.from("fr", "ML"), // Mali / lang=French
            LocaleFactory.from("mt", "MT"), // Malta / lang=Maltese
            LocaleFactory.from("en", "MH"), // Marshall Islands / lang=English
            LocaleFactory.from("fr", "MQ"), // Martinique / lang=French
            LocaleFactory.from("ar", "MR"), // Mauritania / lang=Arabic
            LocaleFactory.from("en", "MU"), // Mauritius / lang=English
            LocaleFactory.from("fr", "YT"), // Mayotte / lang=French
            LocaleFactory.from("es", "MX"), // Mexico / lang=Spanish
            LocaleFactory.from("en", "FM"), // Micronesia (Federated States of) / lang=English
            LocaleFactory.from("ro", "MD"), // Moldova (Republic of) / lang=Romanian
            LocaleFactory.from("fr", "MC"), // Monaco / lang=French
            LocaleFactory.from("mn", "MN"), // Mongolia / lang=Mongolian
            LocaleFactory.from("sr", "ME"), // Montenegro / lang=Serbian
            LocaleFactory.from("en", "MS"), // Montserrat / lang=English
            LocaleFactory.from("ar", "MA"), // Morocco / lang=Arabic
            LocaleFactory.from("pt", "MZ"), // Mozambique / lang=Portuguese
            LocaleFactory.from("my", "MM"), // Myanmar / lang=Burmese
            LocaleFactory.from("en", "NA"), // Namibia / lang=English
            LocaleFactory.from("en", "NR"), // Nauru / lang=English
            LocaleFactory.from("ne", "NP"), // Nepal / lang=Nepali
            LocaleFactory.from("nl", "NL"), // Netherlands / lang=Dutch
            LocaleFactory.from("fr", "NC"), // New Caledonia / lang=French
            LocaleFactory.from("en", "NZ"), // New Zealand / lang=English
            LocaleFactory.from("es", "NI"), // Nicaragua / lang=Spanish
            LocaleFactory.from("fr", "NE"), // Niger / lang=French
            LocaleFactory.from("en", "NG"), // Nigeria / lang=English
            LocaleFactory.from("en", "NU"), // Niue / lang=English
            LocaleFactory.from("en", "NF"), // Norfolk Island / lang=English
            LocaleFactory.from("ko", "KP"), // Korea (Democratic People's Republic of) / lang=Korean
            LocaleFactory.from("en", "MP"), // Northern Mariana Islands / lang=English
            LocaleFactory.from("no", "NO"), // Norway / lang=Norwegian
            LocaleFactory.from("ar", "OM"), // Oman / lang=Arabic
            LocaleFactory.from("en", "PK"), // Pakistan / lang=English
            LocaleFactory.from("en", "PW"), // Palau / lang=English
            LocaleFactory.from("ar", "PS"), // Palestine, State of / lang=Arabic
            LocaleFactory.from("es", "PA"), // Panama / lang=Spanish
            LocaleFactory.from("en", "PG"), // Papua New Guinea / lang=English
            LocaleFactory.from("es", "PY"), // Paraguay / lang=Spanish
            LocaleFactory.from("es", "PE"), // Peru / lang=Spanish
            LocaleFactory.from("en", "PH"), // Philippines / lang=English
            LocaleFactory.from("en", "PN"), // Pitcairn / lang=English
            LocaleFactory.from("pl", "PL"), // Poland / lang=Polish
            LocaleFactory.from("pt", "PT"), // Portugal / lang=Portuguese
            LocaleFactory.from("es", "PR"), // Puerto Rico / lang=Spanish
            LocaleFactory.from("ar", "QA"), // Qatar / lang=Arabic
            LocaleFactory.from("sq", "XK"), // Republic of Kosovo / lang=Albanian
            LocaleFactory.from("fr", "RE"), // Réunion / lang=French
            LocaleFactory.from("ro", "RO"), // Romania / lang=Romanian
            LocaleFactory.from("ru", "RU"), // Russian Federation / lang=Russian
            LocaleFactory.from("rw", "RW"), // Rwanda / lang=Kinyarwanda
            LocaleFactory.from("fr", "BL"), // Saint Barthélemy / lang=French
            LocaleFactory.from("en", "SH"), // Saint Helena, Ascension and Tristan da Cunha / lang=English
            LocaleFactory.from("en", "KN"), // Saint Kitts and Nevis / lang=English
            LocaleFactory.from("en", "LC"), // Saint Lucia / lang=English
            LocaleFactory.from("en", "MF"), // Saint Martin (French part) / lang=English
            LocaleFactory.from("fr", "PM"), // Saint Pierre and Miquelon / lang=French
            LocaleFactory.from("en", "VC"), // Saint Vincent and the Grenadines / lang=English
            LocaleFactory.from("sm", "WS"), // Samoa / lang=Samoan
            LocaleFactory.from("it", "SM"), // San Marino / lang=Italian
            LocaleFactory.from("pt", "ST"), // Sao Tome and Principe / lang=Portuguese
            LocaleFactory.from("ar", "SA"), // Saudi Arabia / lang=Arabic
            LocaleFactory.from("fr", "SN"), // Senegal / lang=French
            LocaleFactory.from("sr", "RS"), // Serbia / lang=Serbian
            LocaleFactory.from("fr", "SC"), // Seychelles / lang=French
            LocaleFactory.from("en", "SL"), // Sierra Leone / lang=English
            LocaleFactory.from("en", "SG"), // Singapore / lang=English
            LocaleFactory.from("nl", "SX"), // Sint Maarten (Dutch part) / lang=Dutch
            LocaleFactory.from("sk", "SK"), // Slovakia / lang=Slovak
            LocaleFactory.from("sl", "SI"), // Slovenia / lang=Slovene
            LocaleFactory.from("en", "SB"), // Solomon Islands / lang=English
            LocaleFactory.from("so", "SO"), // Somalia / lang=Somali
            LocaleFactory.from("af", "ZA"), // South Africa / lang=Afrikaans
            LocaleFactory.from("en", "GS"), // South Georgia and the South Sandwich Islands / lang=English
            LocaleFactory.from("ko", "KR"), // Korea (Republic of) / lang=Korean
            LocaleFactory.from("en", "SS"), // South Sudan / lang=English
            LocaleFactory.from("es", "ES"), // Spain / lang=Spanish
            LocaleFactory.from("si", "LK"), // Sri Lanka / lang=Sinhalese
            LocaleFactory.from("ar", "SD"), // Sudan / lang=Arabic
            LocaleFactory.from("nl", "SR"), // Suriname / lang=Dutch
            LocaleFactory.from("no", "SJ"), // Svalbard and Jan Mayen / lang=Norwegian
            LocaleFactory.from("en", "SZ"), // Swaziland / lang=English
            LocaleFactory.from("sv", "SE"), // Sweden / lang=Swedish
            LocaleFactory.from("de", "CH"), // Switzerland / lang=German
            LocaleFactory.from("ar", "SY"), // Syrian Arab Republic / lang=Arabic
            LocaleFactory.from("zh", "TW"), // Taiwan / lang=Chinese
            LocaleFactory.from("tg", "TJ"), // Tajikistan / lang=Tajik
            LocaleFactory.from("sw", "TZ"), // Tanzania, United Republic of / lang=Swahili
            LocaleFactory.from("th", "TH"), // Thailand / lang=Thai
            LocaleFactory.from("pt", "TL"), // Timor-Leste / lang=Portuguese
            LocaleFactory.from("fr", "TG"), // Togo / lang=French
            LocaleFactory.from("en", "TK"), // Tokelau / lang=English
            LocaleFactory.from("en", "TO"), // Tonga / lang=English
            LocaleFactory.from("en", "TT"), // Trinidad and Tobago / lang=English
            LocaleFactory.from("ar", "TN"), // Tunisia / lang=Arabic
            LocaleFactory.from("tr", "TR"), // Turkey / lang=Turkish
            LocaleFactory.from("tk", "TM"), // Turkmenistan / lang=Turkmen
            LocaleFactory.from("en", "TC"), // Turks and Caicos Islands / lang=English
            LocaleFactory.from("en", "TV"), // Tuvalu / lang=English
            LocaleFactory.from("en", "UG"), // Uganda / lang=English
            LocaleFactory.from("uk", "UA"), // Ukraine / lang=Ukrainian
            LocaleFactory.from("ar", "AE"), // United Arab Emirates / lang=Arabic
            LocaleFactory.from("en", "GB"), // United Kingdom of Great Britain and Northern Ireland / lang=English
            LocaleFactory.from("en", "US"), // United States of America / lang=English
            LocaleFactory.from("es", "UY"), // Uruguay / lang=Spanish
            LocaleFactory.from("uz", "UZ"), // Uzbekistan / lang=Uzbek
            LocaleFactory.from("bi", "VU"), // Vanuatu / lang=Bislama
            LocaleFactory.from("es", "VE"), // Venezuela (Bolivarian Republic of) / lang=Spanish
            LocaleFactory.from("vi", "VN"), // Vietnam / lang=Vietnamese
            LocaleFactory.from("fr", "WF"), // Wallis and Futuna / lang=French
            LocaleFactory.from("es", "EH"), // Western Sahara / lang=Spanish
            LocaleFactory.from("ar", "YE"), // Yemen / lang=Arabic
            LocaleFactory.from("en", "ZM"), // Zambia / lang=English
            LocaleFactory.from("en", "ZW")  // Zimbabwe / lang=English
    );
}
