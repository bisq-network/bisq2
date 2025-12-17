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
import bisq.common.util.LocaleUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

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
        LocaleRepository.defaultLocale = locale;
    }

    public static Locale ensureValidLocale(Locale locale) {
        if (locale.getCountry().isEmpty()) {
            log.warn("Locale has no country defined. locale={}", locale);
            Locale currentLocale = LocaleRepository.getDefaultLocale();

            if (!locale.getLanguage().isEmpty() && !currentLocale.getCountry().isEmpty()) {
                log.warn("Locale has no country defined. We apply the country from the current locale.");
                return LocaleUtils.of(locale.getLanguage(), currentLocale.getCountry());
            } else {
                log.warn("Could not set the new locale, we fall back to Locale.US");
                return Locale.US;
            }
        } else {
            return locale;
        }
    }

    // Data from https://restcountries.eu/rest/v2/all?fields=name;region;subregion;alpha2Code;languages
    public static final Set<Locale> LOCALES = Set.of(
            LocaleUtils.of("ps", "AF"), // Afghanistan / lang=Pashto
            LocaleUtils.of("sv", "AX"), // Åland Islands / lang=Swedish
            LocaleUtils.of("sq", "AL"), // Albania / lang=Albanian
            LocaleUtils.of("ar", "DZ"), // Algeria / lang=Arabic
            LocaleUtils.of("en", "AS"), // American Samoa / lang=English
            LocaleUtils.of("ca", "AD"), // Andorra / lang=Catalan
            LocaleUtils.of("pt", "AO"), // Angola / lang=Portuguese
            LocaleUtils.of("en", "AI"), // Anguilla / lang=English
            LocaleUtils.of("en", "AG"), // Antigua and Barbuda / lang=English
            LocaleUtils.of("es", "AR"), // Argentina / lang=Spanish
            LocaleUtils.of("hy", "AM"), // Armenia / lang=Armenian
            LocaleUtils.of("nl", "AW"), // Aruba / lang=Dutch
            LocaleUtils.of("en", "AU"), // Australia / lang=English
            LocaleUtils.of("de", "AT"), // Austria / lang=German
            LocaleUtils.of("az", "AZ"), // Azerbaijan / lang=Azerbaijani
            LocaleUtils.of("en", "BS"), // Bahamas / lang=English
            LocaleUtils.of("ar", "BH"), // Bahrain / lang=Arabic
            LocaleUtils.of("bn", "BD"), // Bangladesh / lang=Bengali
            LocaleUtils.of("en", "BB"), // Barbados / lang=English
            LocaleUtils.of("be", "BY"), // Belarus / lang=Belarusian
            LocaleUtils.of("nl", "BE"), // Belgium / lang=Dutch
            LocaleUtils.of("en", "BZ"), // Belize / lang=English
            LocaleUtils.of("fr", "BJ"), // Benin / lang=French
            LocaleUtils.of("en", "BM"), // Bermuda / lang=English
            LocaleUtils.of("dz", "BT"), // Bhutan / lang=Dzongkha
            LocaleUtils.of("es", "BO"), // Bolivia (Plurinational State of) / lang=Spanish
            LocaleUtils.of("nl", "BQ"), // Bonaire, Sint Eustatius and Saba / lang=Dutch
            LocaleUtils.of("bs", "BA"), // Bosnia and Herzegovina / lang=Bosnian
            LocaleUtils.of("en", "BW"), // Botswana / lang=English
            LocaleUtils.of("pt", "BR"), // Brazil / lang=Portuguese
            LocaleUtils.of("en", "IO"), // British Indian Ocean Territory / lang=English
            LocaleUtils.of("en", "UM"), // United States Minor Outlying Islands / lang=English
            LocaleUtils.of("en", "VG"), // Virgin Islands (British) / lang=English
            LocaleUtils.of("en", "VI"), // Virgin Islands (U.S.) / lang=English
            LocaleUtils.of("ms", "BN"), // Brunei Darussalam / lang=Malay
            LocaleUtils.of("bg", "BG"), // Bulgaria / lang=Bulgarian
            LocaleUtils.of("fr", "BF"), // Burkina Faso / lang=French
            LocaleUtils.of("fr", "BI"), // Burundi / lang=French
            LocaleUtils.of("km", "KH"), // Cambodia / lang=Khmer
            LocaleUtils.of("en", "CM"), // Cameroon / lang=English
            LocaleUtils.of("en", "CA"), // Canada / lang=English
            LocaleUtils.of("pt", "CV"), // Cabo Verde / lang=Portuguese
            LocaleUtils.of("en", "KY"), // Cayman Islands / lang=English
            LocaleUtils.of("fr", "CF"), // Central African Republic / lang=French
            LocaleUtils.of("fr", "TD"), // Chad / lang=French
            LocaleUtils.of("es", "CL"), // Chile / lang=Spanish
            LocaleUtils.of("zh", "CN"), // China / lang=Chinese
            LocaleUtils.of("en", "CX"), // Christmas Island / lang=English
            LocaleUtils.of("en", "CC"), // Cocos (Keeling) Islands / lang=English
            LocaleUtils.of("es", "CO"), // Colombia / lang=Spanish
            LocaleUtils.of("ar", "KM"), // Comoros / lang=Arabic
            LocaleUtils.of("fr", "CG"), // Congo / lang=French
            LocaleUtils.of("fr", "CD"), // Congo (Democratic Republic of the) / lang=French
            LocaleUtils.of("en", "CK"), // Cook Islands / lang=English
            LocaleUtils.of("es", "CR"), // Costa Rica / lang=Spanish
            LocaleUtils.of("hr", "HR"), // Croatia / lang=Croatian
            LocaleUtils.of("es", "CU"), // Cuba / lang=Spanish
            LocaleUtils.of("nl", "CW"), // Curaçao / lang=Dutch
            LocaleUtils.of("el", "CY"), // Cyprus / lang=Greek (modern)
            LocaleUtils.of("cs", "CZ"), // Czech Republic / lang=Czech
            LocaleUtils.of("da", "DK"), // Denmark / lang=Danish
            LocaleUtils.of("fr", "DJ"), // Djibouti / lang=French
            LocaleUtils.of("en", "DM"), // Dominica / lang=English
            LocaleUtils.of("es", "DO"), // Dominican Republic / lang=Spanish
            LocaleUtils.of("es", "EC"), // Ecuador / lang=Spanish
            LocaleUtils.of("ar", "EG"), // Egypt / lang=Arabic
            LocaleUtils.of("es", "SV"), // El Salvador / lang=Spanish
            LocaleUtils.of("es", "GQ"), // Equatorial Guinea / lang=Spanish
            LocaleUtils.of("ti", "ER"), // Eritrea / lang=Tigrinya
            LocaleUtils.of("et", "EE"), // Estonia / lang=Estonian
            LocaleUtils.of("am", "ET"), // Ethiopia / lang=Amharic
            LocaleUtils.of("en", "FK"), // Falkland Islands (Malvinas) / lang=English
            LocaleUtils.of("fo", "FO"), // Faroe Islands / lang=Faroese
            LocaleUtils.of("en", "FJ"), // Fiji / lang=English
            LocaleUtils.of("fi", "FI"), // Finland / lang=Finnish
            LocaleUtils.of("fr", "FR"), // France / lang=French
            LocaleUtils.of("fr", "GF"), // French Guiana / lang=French
            LocaleUtils.of("fr", "PF"), // French Polynesia / lang=French
            LocaleUtils.of("fr", "TF"), // French Southern Territories / lang=French
            LocaleUtils.of("fr", "GA"), // Gabon / lang=French
            LocaleUtils.of("en", "GM"), // Gambia / lang=English
            LocaleUtils.of("ka", "GE"), // Georgia / lang=Georgian
            LocaleUtils.of("de", "DE"), // Germany / lang=German
            LocaleUtils.of("en", "GH"), // Ghana / lang=English
            LocaleUtils.of("en", "GI"), // Gibraltar / lang=English
            LocaleUtils.of("el", "GR"), // Greece / lang=Greek (modern)
            LocaleUtils.of("kl", "GL"), // Greenland / lang=Kalaallisut
            LocaleUtils.of("en", "GD"), // Grenada / lang=English
            LocaleUtils.of("fr", "GP"), // Guadeloupe / lang=French
            LocaleUtils.of("en", "GU"), // Guam / lang=English
            LocaleUtils.of("es", "GT"), // Guatemala / lang=Spanish
            LocaleUtils.of("en", "GG"), // Guernsey / lang=English
            LocaleUtils.of("fr", "GN"), // Guinea / lang=French
            LocaleUtils.of("pt", "GW"), // Guinea-Bissau / lang=Portuguese
            LocaleUtils.of("en", "GY"), // Guyana / lang=English
            LocaleUtils.of("fr", "HT"), // Haiti / lang=French
            LocaleUtils.of("la", "VA"), // Holy See / lang=Latin
            LocaleUtils.of("es", "HN"), // Honduras / lang=Spanish
            LocaleUtils.of("en", "HK"), // Hong Kong / lang=English
            LocaleUtils.of("hu", "HU"), // Hungary / lang=Hungarian
            LocaleUtils.of("is", "IS"), // Iceland / lang=Icelandic
            LocaleUtils.of("hi", "IN"), // India / lang=Hindi
            LocaleUtils.of("id", "ID"), // Indonesia / lang=Indonesian
            LocaleUtils.of("fr", "CI"), // Côte d'Ivoire / lang=French
            LocaleUtils.of("fa", "IR"), // Iran (Islamic Republic of) / lang=Persian (Farsi)
            LocaleUtils.of("ar", "IQ"), // Iraq / lang=Arabic
            LocaleUtils.of("ga", "IE"), // Ireland / lang=Irish
            LocaleUtils.of("en", "IM"), // Isle of Man / lang=English
            LocaleUtils.of("he", "IL"), // Israel / lang=Hebrew (modern)
            LocaleUtils.of("it", "IT"), // Italy / lang=Italian
            LocaleUtils.of("en", "JM"), // Jamaica / lang=English
            LocaleUtils.of("ja", "JP"), // Japan / lang=Japanese
            LocaleUtils.of("en", "JE"), // Jersey / lang=English
            LocaleUtils.of("ar", "JO"), // Jordan / lang=Arabic
            LocaleUtils.of("kk", "KZ"), // Kazakhstan / lang=Kazakh
            LocaleUtils.of("en", "KE"), // Kenya / lang=English
            LocaleUtils.of("en", "KI"), // Kiribati / lang=English
            LocaleUtils.of("ar", "KW"), // Kuwait / lang=Arabic
            LocaleUtils.of("ky", "KG"), // Kyrgyzstan / lang=Kyrgyz
            LocaleUtils.of("lo", "LA"), // Lao People's Democratic Republic / lang=Lao
            LocaleUtils.of("lv", "LV"), // Latvia / lang=Latvian
            LocaleUtils.of("ar", "LB"), // Lebanon / lang=Arabic
            LocaleUtils.of("en", "LS"), // Lesotho / lang=English
            LocaleUtils.of("en", "LR"), // Liberia / lang=English
            LocaleUtils.of("ar", "LY"), // Libya / lang=Arabic
            LocaleUtils.of("de", "LI"), // Liechtenstein / lang=German
            LocaleUtils.of("lt", "LT"), // Lithuania / lang=Lithuanian
            LocaleUtils.of("fr", "LU"), // Luxembourg / lang=French
            LocaleUtils.of("zh", "MO"), // Macao / lang=Chinese
            LocaleUtils.of("mk", "MK"), // Macedonia (the former Yugoslav Republic of) / lang=Macedonian
            LocaleUtils.of("fr", "MG"), // Madagascar / lang=French
            LocaleUtils.of("en", "MW"), // Malawi / lang=English
            LocaleUtils.of("en", "MY"), // Malaysia / lang=Malaysian
            LocaleUtils.of("dv", "MV"), // Maldives / lang=Divehi
            LocaleUtils.of("fr", "ML"), // Mali / lang=French
            LocaleUtils.of("mt", "MT"), // Malta / lang=Maltese
            LocaleUtils.of("en", "MH"), // Marshall Islands / lang=English
            LocaleUtils.of("fr", "MQ"), // Martinique / lang=French
            LocaleUtils.of("ar", "MR"), // Mauritania / lang=Arabic
            LocaleUtils.of("en", "MU"), // Mauritius / lang=English
            LocaleUtils.of("fr", "YT"), // Mayotte / lang=French
            LocaleUtils.of("es", "MX"), // Mexico / lang=Spanish
            LocaleUtils.of("en", "FM"), // Micronesia (Federated States of) / lang=English
            LocaleUtils.of("ro", "MD"), // Moldova (Republic of) / lang=Romanian
            LocaleUtils.of("fr", "MC"), // Monaco / lang=French
            LocaleUtils.of("mn", "MN"), // Mongolia / lang=Mongolian
            LocaleUtils.of("sr", "ME"), // Montenegro / lang=Serbian
            LocaleUtils.of("en", "MS"), // Montserrat / lang=English
            LocaleUtils.of("ar", "MA"), // Morocco / lang=Arabic
            LocaleUtils.of("pt", "MZ"), // Mozambique / lang=Portuguese
            LocaleUtils.of("my", "MM"), // Myanmar / lang=Burmese
            LocaleUtils.of("en", "NA"), // Namibia / lang=English
            LocaleUtils.of("en", "NR"), // Nauru / lang=English
            LocaleUtils.of("ne", "NP"), // Nepal / lang=Nepali
            LocaleUtils.of("nl", "NL"), // Netherlands / lang=Dutch
            LocaleUtils.of("fr", "NC"), // New Caledonia / lang=French
            LocaleUtils.of("en", "NZ"), // New Zealand / lang=English
            LocaleUtils.of("es", "NI"), // Nicaragua / lang=Spanish
            LocaleUtils.of("fr", "NE"), // Niger / lang=French
            LocaleUtils.of("en", "NG"), // Nigeria / lang=English
            LocaleUtils.of("en", "NU"), // Niue / lang=English
            LocaleUtils.of("en", "NF"), // Norfolk Island / lang=English
            LocaleUtils.of("ko", "KP"), // Korea (Democratic People's Republic of) / lang=Korean
            LocaleUtils.of("en", "MP"), // Northern Mariana Islands / lang=English
            LocaleUtils.of("no", "NO"), // Norway / lang=Norwegian
            LocaleUtils.of("ar", "OM"), // Oman / lang=Arabic
            LocaleUtils.of("en", "PK"), // Pakistan / lang=English
            LocaleUtils.of("en", "PW"), // Palau / lang=English
            LocaleUtils.of("ar", "PS"), // Palestine, State of / lang=Arabic
            LocaleUtils.of("es", "PA"), // Panama / lang=Spanish
            LocaleUtils.of("en", "PG"), // Papua New Guinea / lang=English
            LocaleUtils.of("es", "PY"), // Paraguay / lang=Spanish
            LocaleUtils.of("es", "PE"), // Peru / lang=Spanish
            LocaleUtils.of("en", "PH"), // Philippines / lang=English
            LocaleUtils.of("en", "PN"), // Pitcairn / lang=English
            LocaleUtils.of("pl", "PL"), // Poland / lang=Polish
            LocaleUtils.of("pt", "PT"), // Portugal / lang=Portuguese
            LocaleUtils.of("es", "PR"), // Puerto Rico / lang=Spanish
            LocaleUtils.of("ar", "QA"), // Qatar / lang=Arabic
            LocaleUtils.of("sq", "XK"), // Republic of Kosovo / lang=Albanian
            LocaleUtils.of("fr", "RE"), // Réunion / lang=French
            LocaleUtils.of("ro", "RO"), // Romania / lang=Romanian
            LocaleUtils.of("ru", "RU"), // Russian Federation / lang=Russian
            LocaleUtils.of("rw", "RW"), // Rwanda / lang=Kinyarwanda
            LocaleUtils.of("fr", "BL"), // Saint Barthélemy / lang=French
            LocaleUtils.of("en", "SH"), // Saint Helena, Ascension and Tristan da Cunha / lang=English
            LocaleUtils.of("en", "KN"), // Saint Kitts and Nevis / lang=English
            LocaleUtils.of("en", "LC"), // Saint Lucia / lang=English
            LocaleUtils.of("en", "MF"), // Saint Martin (French part) / lang=English
            LocaleUtils.of("fr", "PM"), // Saint Pierre and Miquelon / lang=French
            LocaleUtils.of("en", "VC"), // Saint Vincent and the Grenadines / lang=English
            LocaleUtils.of("sm", "WS"), // Samoa / lang=Samoan
            LocaleUtils.of("it", "SM"), // San Marino / lang=Italian
            LocaleUtils.of("pt", "ST"), // Sao Tome and Principe / lang=Portuguese
            LocaleUtils.of("ar", "SA"), // Saudi Arabia / lang=Arabic
            LocaleUtils.of("fr", "SN"), // Senegal / lang=French
            LocaleUtils.of("sr", "RS"), // Serbia / lang=Serbian
            LocaleUtils.of("fr", "SC"), // Seychelles / lang=French
            LocaleUtils.of("en", "SL"), // Sierra Leone / lang=English
            LocaleUtils.of("en", "SG"), // Singapore / lang=English
            LocaleUtils.of("nl", "SX"), // Sint Maarten (Dutch part) / lang=Dutch
            LocaleUtils.of("sk", "SK"), // Slovakia / lang=Slovak
            LocaleUtils.of("sl", "SI"), // Slovenia / lang=Slovene
            LocaleUtils.of("en", "SB"), // Solomon Islands / lang=English
            LocaleUtils.of("so", "SO"), // Somalia / lang=Somali
            LocaleUtils.of("af", "ZA"), // South Africa / lang=Afrikaans
            LocaleUtils.of("en", "GS"), // South Georgia and the South Sandwich Islands / lang=English
            LocaleUtils.of("ko", "KR"), // Korea (Republic of) / lang=Korean
            LocaleUtils.of("en", "SS"), // South Sudan / lang=English
            LocaleUtils.of("es", "ES"), // Spain / lang=Spanish
            LocaleUtils.of("si", "LK"), // Sri Lanka / lang=Sinhalese
            LocaleUtils.of("ar", "SD"), // Sudan / lang=Arabic
            LocaleUtils.of("nl", "SR"), // Suriname / lang=Dutch
            LocaleUtils.of("no", "SJ"), // Svalbard and Jan Mayen / lang=Norwegian
            LocaleUtils.of("en", "SZ"), // Swaziland / lang=English
            LocaleUtils.of("sv", "SE"), // Sweden / lang=Swedish
            LocaleUtils.of("de", "CH"), // Switzerland / lang=German
            LocaleUtils.of("ar", "SY"), // Syrian Arab Republic / lang=Arabic
            LocaleUtils.of("zh", "TW"), // Taiwan / lang=Chinese
            LocaleUtils.of("tg", "TJ"), // Tajikistan / lang=Tajik
            LocaleUtils.of("sw", "TZ"), // Tanzania, United Republic of / lang=Swahili
            LocaleUtils.of("th", "TH"), // Thailand / lang=Thai
            LocaleUtils.of("pt", "TL"), // Timor-Leste / lang=Portuguese
            LocaleUtils.of("fr", "TG"), // Togo / lang=French
            LocaleUtils.of("en", "TK"), // Tokelau / lang=English
            LocaleUtils.of("en", "TO"), // Tonga / lang=English
            LocaleUtils.of("en", "TT"), // Trinidad and Tobago / lang=English
            LocaleUtils.of("ar", "TN"), // Tunisia / lang=Arabic
            LocaleUtils.of("tr", "TR"), // Turkey / lang=Turkish
            LocaleUtils.of("tk", "TM"), // Turkmenistan / lang=Turkmen
            LocaleUtils.of("en", "TC"), // Turks and Caicos Islands / lang=English
            LocaleUtils.of("en", "TV"), // Tuvalu / lang=English
            LocaleUtils.of("en", "UG"), // Uganda / lang=English
            LocaleUtils.of("uk", "UA"), // Ukraine / lang=Ukrainian
            LocaleUtils.of("ar", "AE"), // United Arab Emirates / lang=Arabic
            LocaleUtils.of("en", "GB"), // United Kingdom of Great Britain and Northern Ireland / lang=English
            LocaleUtils.of("en", "US"), // United States of America / lang=English
            LocaleUtils.of("es", "UY"), // Uruguay / lang=Spanish
            LocaleUtils.of("uz", "UZ"), // Uzbekistan / lang=Uzbek
            LocaleUtils.of("bi", "VU"), // Vanuatu / lang=Bislama
            LocaleUtils.of("es", "VE"), // Venezuela (Bolivarian Republic of) / lang=Spanish
            LocaleUtils.of("vi", "VN"), // Vietnam / lang=Vietnamese
            LocaleUtils.of("fr", "WF"), // Wallis and Futuna / lang=French
            LocaleUtils.of("es", "EH"), // Western Sahara / lang=Spanish
            LocaleUtils.of("ar", "YE"), // Yemen / lang=Arabic
            LocaleUtils.of("en", "ZM"), // Zambia / lang=English
            LocaleUtils.of("en", "ZW")  // Zimbabwe / lang=English
    );
}
