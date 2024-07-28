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

import bisq.common.options.PropertiesReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class LocaleRepository {
    @Getter
    private static Locale defaultLocale;

    static {
        Properties properties = PropertiesReader.getProperties("bisq.properties");
        if (properties != null) {
            Locale locale = Locale.of(properties.getProperty("language"), properties.getProperty("country"));
            setDefaultLocale(locale);
        } else {
            setDefaultLocale(Locale.getDefault());
        }
    }

    public static void setDefaultLocale(Locale defaultLocale) {
        if (isLocaleInvalid(defaultLocale)) {
            defaultLocale = Locale.US;
        }
        LocaleRepository.defaultLocale = defaultLocale;
    }

    public static boolean isLocaleInvalid(Locale locale) {
        // On some systems there is no country defined, in that case we use en_US
        boolean isInvalid = locale == null ||
                locale.getCountry() == null ||
                locale.getCountry().isEmpty() ||
                locale.getDisplayCountry() == null ||
                locale.getDisplayCountry().isEmpty() ||
                locale.getLanguage() == null ||
                locale.getLanguage().isEmpty() ||
                locale.getDisplayLanguage() == null ||
                locale.getDisplayLanguage().isEmpty();
        if (isInvalid) {
            log.warn("Provided locale is invalid. We use Locale.US instead. Provided locale={}", locale);

        }
        return isInvalid;
    }

    // Data from https://restcountries.eu/rest/v2/all?fields=name;region;subregion;alpha2Code;languages
    public static final Set<Locale> LOCALES = Set.of(
            Locale.of("ps", "AF"), // Afghanistan / lang=Pashto
            Locale.of("sv", "AX"), // Åland Islands / lang=Swedish
            Locale.of("sq", "AL"), // Albania / lang=Albanian
            Locale.of("ar", "DZ"), // Algeria / lang=Arabic
            Locale.of("en", "AS"), // American Samoa / lang=English
            Locale.of("ca", "AD"), // Andorra / lang=Catalan
            Locale.of("pt", "AO"), // Angola / lang=Portuguese
            Locale.of("en", "AI"), // Anguilla / lang=English
            Locale.of("en", "AG"), // Antigua and Barbuda / lang=English
            Locale.of("es", "AR"), // Argentina / lang=Spanish
            Locale.of("hy", "AM"), // Armenia / lang=Armenian
            Locale.of("nl", "AW"), // Aruba / lang=Dutch
            Locale.of("en", "AU"), // Australia / lang=English
            Locale.of("de", "AT"), // Austria / lang=German
            Locale.of("az", "AZ"), // Azerbaijan / lang=Azerbaijani
            Locale.of("en", "BS"), // Bahamas / lang=English
            Locale.of("ar", "BH"), // Bahrain / lang=Arabic
            Locale.of("bn", "BD"), // Bangladesh / lang=Bengali
            Locale.of("en", "BB"), // Barbados / lang=English
            Locale.of("be", "BY"), // Belarus / lang=Belarusian
            Locale.of("nl", "BE"), // Belgium / lang=Dutch
            Locale.of("en", "BZ"), // Belize / lang=English
            Locale.of("fr", "BJ"), // Benin / lang=French
            Locale.of("en", "BM"), // Bermuda / lang=English
            Locale.of("dz", "BT"), // Bhutan / lang=Dzongkha
            Locale.of("es", "BO"), // Bolivia (Plurinational State of) / lang=Spanish
            Locale.of("nl", "BQ"), // Bonaire, Sint Eustatius and Saba / lang=Dutch
            Locale.of("bs", "BA"), // Bosnia and Herzegovina / lang=Bosnian
            Locale.of("en", "BW"), // Botswana / lang=English
            Locale.of("pt", "BR"), // Brazil / lang=Portuguese
            Locale.of("en", "IO"), // British Indian Ocean Territory / lang=English
            Locale.of("en", "UM"), // United States Minor Outlying Islands / lang=English
            Locale.of("en", "VG"), // Virgin Islands (British) / lang=English
            Locale.of("en", "VI"), // Virgin Islands (U.S.) / lang=English
            Locale.of("ms", "BN"), // Brunei Darussalam / lang=Malay
            Locale.of("bg", "BG"), // Bulgaria / lang=Bulgarian
            Locale.of("fr", "BF"), // Burkina Faso / lang=French
            Locale.of("fr", "BI"), // Burundi / lang=French
            Locale.of("km", "KH"), // Cambodia / lang=Khmer
            Locale.of("en", "CM"), // Cameroon / lang=English
            Locale.of("en", "CA"), // Canada / lang=English
            Locale.of("pt", "CV"), // Cabo Verde / lang=Portuguese
            Locale.of("en", "KY"), // Cayman Islands / lang=English
            Locale.of("fr", "CF"), // Central African Republic / lang=French
            Locale.of("fr", "TD"), // Chad / lang=French
            Locale.of("es", "CL"), // Chile / lang=Spanish
            Locale.of("zh", "CN"), // China / lang=Chinese
            Locale.of("en", "CX"), // Christmas Island / lang=English
            Locale.of("en", "CC"), // Cocos (Keeling) Islands / lang=English
            Locale.of("es", "CO"), // Colombia / lang=Spanish
            Locale.of("ar", "KM"), // Comoros / lang=Arabic
            Locale.of("fr", "CG"), // Congo / lang=French
            Locale.of("fr", "CD"), // Congo (Democratic Republic of the) / lang=French
            Locale.of("en", "CK"), // Cook Islands / lang=English
            Locale.of("es", "CR"), // Costa Rica / lang=Spanish
            Locale.of("hr", "HR"), // Croatia / lang=Croatian
            Locale.of("es", "CU"), // Cuba / lang=Spanish
            Locale.of("nl", "CW"), // Curaçao / lang=Dutch
            Locale.of("el", "CY"), // Cyprus / lang=Greek (modern)
            Locale.of("cs", "CZ"), // Czech Republic / lang=Czech
            Locale.of("da", "DK"), // Denmark / lang=Danish
            Locale.of("fr", "DJ"), // Djibouti / lang=French
            Locale.of("en", "DM"), // Dominica / lang=English
            Locale.of("es", "DO"), // Dominican Republic / lang=Spanish
            Locale.of("es", "EC"), // Ecuador / lang=Spanish
            Locale.of("ar", "EG"), // Egypt / lang=Arabic
            Locale.of("es", "SV"), // El Salvador / lang=Spanish
            Locale.of("es", "GQ"), // Equatorial Guinea / lang=Spanish
            Locale.of("ti", "ER"), // Eritrea / lang=Tigrinya
            Locale.of("et", "EE"), // Estonia / lang=Estonian
            Locale.of("am", "ET"), // Ethiopia / lang=Amharic
            Locale.of("en", "FK"), // Falkland Islands (Malvinas) / lang=English
            Locale.of("fo", "FO"), // Faroe Islands / lang=Faroese
            Locale.of("en", "FJ"), // Fiji / lang=English
            Locale.of("fi", "FI"), // Finland / lang=Finnish
            Locale.of("fr", "FR"), // France / lang=French
            Locale.of("fr", "GF"), // French Guiana / lang=French
            Locale.of("fr", "PF"), // French Polynesia / lang=French
            Locale.of("fr", "TF"), // French Southern Territories / lang=French
            Locale.of("fr", "GA"), // Gabon / lang=French
            Locale.of("en", "GM"), // Gambia / lang=English
            Locale.of("ka", "GE"), // Georgia / lang=Georgian
            Locale.of("de", "DE"), // Germany / lang=German
            Locale.of("en", "GH"), // Ghana / lang=English
            Locale.of("en", "GI"), // Gibraltar / lang=English
            Locale.of("el", "GR"), // Greece / lang=Greek (modern)
            Locale.of("kl", "GL"), // Greenland / lang=Kalaallisut
            Locale.of("en", "GD"), // Grenada / lang=English
            Locale.of("fr", "GP"), // Guadeloupe / lang=French
            Locale.of("en", "GU"), // Guam / lang=English
            Locale.of("es", "GT"), // Guatemala / lang=Spanish
            Locale.of("en", "GG"), // Guernsey / lang=English
            Locale.of("fr", "GN"), // Guinea / lang=French
            Locale.of("pt", "GW"), // Guinea-Bissau / lang=Portuguese
            Locale.of("en", "GY"), // Guyana / lang=English
            Locale.of("fr", "HT"), // Haiti / lang=French
            Locale.of("la", "VA"), // Holy See / lang=Latin
            Locale.of("es", "HN"), // Honduras / lang=Spanish
            Locale.of("en", "HK"), // Hong Kong / lang=English
            Locale.of("hu", "HU"), // Hungary / lang=Hungarian
            Locale.of("is", "IS"), // Iceland / lang=Icelandic
            Locale.of("hi", "IN"), // India / lang=Hindi
            Locale.of("id", "ID"), // Indonesia / lang=Indonesian
            Locale.of("fr", "CI"), // Côte d'Ivoire / lang=French
            Locale.of("fa", "IR"), // Iran (Islamic Republic of) / lang=Persian (Farsi)
            Locale.of("ar", "IQ"), // Iraq / lang=Arabic
            Locale.of("ga", "IE"), // Ireland / lang=Irish
            Locale.of("en", "IM"), // Isle of Man / lang=English
            Locale.of("he", "IL"), // Israel / lang=Hebrew (modern)
            Locale.of("it", "IT"), // Italy / lang=Italian
            Locale.of("en", "JM"), // Jamaica / lang=English
            Locale.of("ja", "JP"), // Japan / lang=Japanese
            Locale.of("en", "JE"), // Jersey / lang=English
            Locale.of("ar", "JO"), // Jordan / lang=Arabic
            Locale.of("kk", "KZ"), // Kazakhstan / lang=Kazakh
            Locale.of("en", "KE"), // Kenya / lang=English
            Locale.of("en", "KI"), // Kiribati / lang=English
            Locale.of("ar", "KW"), // Kuwait / lang=Arabic
            Locale.of("ky", "KG"), // Kyrgyzstan / lang=Kyrgyz
            Locale.of("lo", "LA"), // Lao People's Democratic Republic / lang=Lao
            Locale.of("lv", "LV"), // Latvia / lang=Latvian
            Locale.of("ar", "LB"), // Lebanon / lang=Arabic
            Locale.of("en", "LS"), // Lesotho / lang=English
            Locale.of("en", "LR"), // Liberia / lang=English
            Locale.of("ar", "LY"), // Libya / lang=Arabic
            Locale.of("de", "LI"), // Liechtenstein / lang=German
            Locale.of("lt", "LT"), // Lithuania / lang=Lithuanian
            Locale.of("fr", "LU"), // Luxembourg / lang=French
            Locale.of("zh", "MO"), // Macao / lang=Chinese
            Locale.of("mk", "MK"), // Macedonia (the former Yugoslav Republic of) / lang=Macedonian
            Locale.of("fr", "MG"), // Madagascar / lang=French
            Locale.of("en", "MW"), // Malawi / lang=English
            Locale.of("en", "MY"), // Malaysia / lang=Malaysian
            Locale.of("dv", "MV"), // Maldives / lang=Divehi
            Locale.of("fr", "ML"), // Mali / lang=French
            Locale.of("mt", "MT"), // Malta / lang=Maltese
            Locale.of("en", "MH"), // Marshall Islands / lang=English
            Locale.of("fr", "MQ"), // Martinique / lang=French
            Locale.of("ar", "MR"), // Mauritania / lang=Arabic
            Locale.of("en", "MU"), // Mauritius / lang=English
            Locale.of("fr", "YT"), // Mayotte / lang=French
            Locale.of("es", "MX"), // Mexico / lang=Spanish
            Locale.of("en", "FM"), // Micronesia (Federated States of) / lang=English
            Locale.of("ro", "MD"), // Moldova (Republic of) / lang=Romanian
            Locale.of("fr", "MC"), // Monaco / lang=French
            Locale.of("mn", "MN"), // Mongolia / lang=Mongolian
            Locale.of("sr", "ME"), // Montenegro / lang=Serbian
            Locale.of("en", "MS"), // Montserrat / lang=English
            Locale.of("ar", "MA"), // Morocco / lang=Arabic
            Locale.of("pt", "MZ"), // Mozambique / lang=Portuguese
            Locale.of("my", "MM"), // Myanmar / lang=Burmese
            Locale.of("en", "NA"), // Namibia / lang=English
            Locale.of("en", "NR"), // Nauru / lang=English
            Locale.of("ne", "NP"), // Nepal / lang=Nepali
            Locale.of("nl", "NL"), // Netherlands / lang=Dutch
            Locale.of("fr", "NC"), // New Caledonia / lang=French
            Locale.of("en", "NZ"), // New Zealand / lang=English
            Locale.of("es", "NI"), // Nicaragua / lang=Spanish
            Locale.of("fr", "NE"), // Niger / lang=French
            Locale.of("en", "NG"), // Nigeria / lang=English
            Locale.of("en", "NU"), // Niue / lang=English
            Locale.of("en", "NF"), // Norfolk Island / lang=English
            Locale.of("ko", "KP"), // Korea (Democratic People's Republic of) / lang=Korean
            Locale.of("en", "MP"), // Northern Mariana Islands / lang=English
            Locale.of("no", "NO"), // Norway / lang=Norwegian
            Locale.of("ar", "OM"), // Oman / lang=Arabic
            Locale.of("en", "PK"), // Pakistan / lang=English
            Locale.of("en", "PW"), // Palau / lang=English
            Locale.of("ar", "PS"), // Palestine, State of / lang=Arabic
            Locale.of("es", "PA"), // Panama / lang=Spanish
            Locale.of("en", "PG"), // Papua New Guinea / lang=English
            Locale.of("es", "PY"), // Paraguay / lang=Spanish
            Locale.of("es", "PE"), // Peru / lang=Spanish
            Locale.of("en", "PH"), // Philippines / lang=English
            Locale.of("en", "PN"), // Pitcairn / lang=English
            Locale.of("pl", "PL"), // Poland / lang=Polish
            Locale.of("pt", "PT"), // Portugal / lang=Portuguese
            Locale.of("es", "PR"), // Puerto Rico / lang=Spanish
            Locale.of("ar", "QA"), // Qatar / lang=Arabic
            Locale.of("sq", "XK"), // Republic of Kosovo / lang=Albanian
            Locale.of("fr", "RE"), // Réunion / lang=French
            Locale.of("ro", "RO"), // Romania / lang=Romanian
            Locale.of("ru", "RU"), // Russian Federation / lang=Russian
            Locale.of("rw", "RW"), // Rwanda / lang=Kinyarwanda
            Locale.of("fr", "BL"), // Saint Barthélemy / lang=French
            Locale.of("en", "SH"), // Saint Helena, Ascension and Tristan da Cunha / lang=English
            Locale.of("en", "KN"), // Saint Kitts and Nevis / lang=English
            Locale.of("en", "LC"), // Saint Lucia / lang=English
            Locale.of("en", "MF"), // Saint Martin (French part) / lang=English
            Locale.of("fr", "PM"), // Saint Pierre and Miquelon / lang=French
            Locale.of("en", "VC"), // Saint Vincent and the Grenadines / lang=English
            Locale.of("sm", "WS"), // Samoa / lang=Samoan
            Locale.of("it", "SM"), // San Marino / lang=Italian
            Locale.of("pt", "ST"), // Sao Tome and Principe / lang=Portuguese
            Locale.of("ar", "SA"), // Saudi Arabia / lang=Arabic
            Locale.of("fr", "SN"), // Senegal / lang=French
            Locale.of("sr", "RS"), // Serbia / lang=Serbian
            Locale.of("fr", "SC"), // Seychelles / lang=French
            Locale.of("en", "SL"), // Sierra Leone / lang=English
            Locale.of("en", "SG"), // Singapore / lang=English
            Locale.of("nl", "SX"), // Sint Maarten (Dutch part) / lang=Dutch
            Locale.of("sk", "SK"), // Slovakia / lang=Slovak
            Locale.of("sl", "SI"), // Slovenia / lang=Slovene
            Locale.of("en", "SB"), // Solomon Islands / lang=English
            Locale.of("so", "SO"), // Somalia / lang=Somali
            Locale.of("af", "ZA"), // South Africa / lang=Afrikaans
            Locale.of("en", "GS"), // South Georgia and the South Sandwich Islands / lang=English
            Locale.of("ko", "KR"), // Korea (Republic of) / lang=Korean
            Locale.of("en", "SS"), // South Sudan / lang=English
            Locale.of("es", "ES"), // Spain / lang=Spanish
            Locale.of("si", "LK"), // Sri Lanka / lang=Sinhalese
            Locale.of("ar", "SD"), // Sudan / lang=Arabic
            Locale.of("nl", "SR"), // Suriname / lang=Dutch
            Locale.of("no", "SJ"), // Svalbard and Jan Mayen / lang=Norwegian
            Locale.of("en", "SZ"), // Swaziland / lang=English
            Locale.of("sv", "SE"), // Sweden / lang=Swedish
            Locale.of("de", "CH"), // Switzerland / lang=German
            Locale.of("ar", "SY"), // Syrian Arab Republic / lang=Arabic
            Locale.of("zh", "TW"), // Taiwan / lang=Chinese
            Locale.of("tg", "TJ"), // Tajikistan / lang=Tajik
            Locale.of("sw", "TZ"), // Tanzania, United Republic of / lang=Swahili
            Locale.of("th", "TH"), // Thailand / lang=Thai
            Locale.of("pt", "TL"), // Timor-Leste / lang=Portuguese
            Locale.of("fr", "TG"), // Togo / lang=French
            Locale.of("en", "TK"), // Tokelau / lang=English
            Locale.of("en", "TO"), // Tonga / lang=English
            Locale.of("en", "TT"), // Trinidad and Tobago / lang=English
            Locale.of("ar", "TN"), // Tunisia / lang=Arabic
            Locale.of("tr", "TR"), // Turkey / lang=Turkish
            Locale.of("tk", "TM"), // Turkmenistan / lang=Turkmen
            Locale.of("en", "TC"), // Turks and Caicos Islands / lang=English
            Locale.of("en", "TV"), // Tuvalu / lang=English
            Locale.of("en", "UG"), // Uganda / lang=English
            Locale.of("uk", "UA"), // Ukraine / lang=Ukrainian
            Locale.of("ar", "AE"), // United Arab Emirates / lang=Arabic
            Locale.of("en", "GB"), // United Kingdom of Great Britain and Northern Ireland / lang=English
            Locale.of("en", "US"), // United States of America / lang=English
            Locale.of("es", "UY"), // Uruguay / lang=Spanish
            Locale.of("uz", "UZ"), // Uzbekistan / lang=Uzbek
            Locale.of("bi", "VU"), // Vanuatu / lang=Bislama
            Locale.of("es", "VE"), // Venezuela (Bolivarian Republic of) / lang=Spanish
            Locale.of("vi", "VN"), // Vietnam / lang=Vietnamese
            Locale.of("fr", "WF"), // Wallis and Futuna / lang=French
            Locale.of("es", "EH"), // Western Sahara / lang=Spanish
            Locale.of("ar", "YE"), // Yemen / lang=Arabic
            Locale.of("en", "ZM"), // Zambia / lang=English
            Locale.of("en", "ZW")  // Zimbabwe / lang=English
    );
}
