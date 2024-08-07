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
            Locale locale = new Locale(properties.getProperty("language"), properties.getProperty("country"));
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
            new Locale("ps", "AF"), // Afghanistan / lang=Pashto
            new Locale("sv", "AX"), // Åland Islands / lang=Swedish
            new Locale("sq", "AL"), // Albania / lang=Albanian
            new Locale("ar", "DZ"), // Algeria / lang=Arabic
            new Locale("en", "AS"), // American Samoa / lang=English
            new Locale("ca", "AD"), // Andorra / lang=Catalan
            new Locale("pt", "AO"), // Angola / lang=Portuguese
            new Locale("en", "AI"), // Anguilla / lang=English
            new Locale("en", "AG"), // Antigua and Barbuda / lang=English
            new Locale("es", "AR"), // Argentina / lang=Spanish
            new Locale("hy", "AM"), // Armenia / lang=Armenian
            new Locale("nl", "AW"), // Aruba / lang=Dutch
            new Locale("en", "AU"), // Australia / lang=English
            new Locale("de", "AT"), // Austria / lang=German
            new Locale("az", "AZ"), // Azerbaijan / lang=Azerbaijani
            new Locale("en", "BS"), // Bahamas / lang=English
            new Locale("ar", "BH"), // Bahrain / lang=Arabic
            new Locale("bn", "BD"), // Bangladesh / lang=Bengali
            new Locale("en", "BB"), // Barbados / lang=English
            new Locale("be", "BY"), // Belarus / lang=Belarusian
            new Locale("nl", "BE"), // Belgium / lang=Dutch
            new Locale("en", "BZ"), // Belize / lang=English
            new Locale("fr", "BJ"), // Benin / lang=French
            new Locale("en", "BM"), // Bermuda / lang=English
            new Locale("dz", "BT"), // Bhutan / lang=Dzongkha
            new Locale("es", "BO"), // Bolivia (Plurinational State of) / lang=Spanish
            new Locale("nl", "BQ"), // Bonaire, Sint Eustatius and Saba / lang=Dutch
            new Locale("bs", "BA"), // Bosnia and Herzegovina / lang=Bosnian
            new Locale("en", "BW"), // Botswana / lang=English
            new Locale("pt", "BR"), // Brazil / lang=Portuguese
            new Locale("en", "IO"), // British Indian Ocean Territory / lang=English
            new Locale("en", "UM"), // United States Minor Outlying Islands / lang=English
            new Locale("en", "VG"), // Virgin Islands (British) / lang=English
            new Locale("en", "VI"), // Virgin Islands (U.S.) / lang=English
            new Locale("ms", "BN"), // Brunei Darussalam / lang=Malay
            new Locale("bg", "BG"), // Bulgaria / lang=Bulgarian
            new Locale("fr", "BF"), // Burkina Faso / lang=French
            new Locale("fr", "BI"), // Burundi / lang=French
            new Locale("km", "KH"), // Cambodia / lang=Khmer
            new Locale("en", "CM"), // Cameroon / lang=English
            new Locale("en", "CA"), // Canada / lang=English
            new Locale("pt", "CV"), // Cabo Verde / lang=Portuguese
            new Locale("en", "KY"), // Cayman Islands / lang=English
            new Locale("fr", "CF"), // Central African Republic / lang=French
            new Locale("fr", "TD"), // Chad / lang=French
            new Locale("es", "CL"), // Chile / lang=Spanish
            new Locale("zh", "CN"), // China / lang=Chinese
            new Locale("en", "CX"), // Christmas Island / lang=English
            new Locale("en", "CC"), // Cocos (Keeling) Islands / lang=English
            new Locale("es", "CO"), // Colombia / lang=Spanish
            new Locale("ar", "KM"), // Comoros / lang=Arabic
            new Locale("fr", "CG"), // Congo / lang=French
            new Locale("fr", "CD"), // Congo (Democratic Republic of the) / lang=French
            new Locale("en", "CK"), // Cook Islands / lang=English
            new Locale("es", "CR"), // Costa Rica / lang=Spanish
            new Locale("hr", "HR"), // Croatia / lang=Croatian
            new Locale("es", "CU"), // Cuba / lang=Spanish
            new Locale("nl", "CW"), // Curaçao / lang=Dutch
            new Locale("el", "CY"), // Cyprus / lang=Greek (modern)
            new Locale("cs", "CZ"), // Czech Republic / lang=Czech
            new Locale("da", "DK"), // Denmark / lang=Danish
            new Locale("fr", "DJ"), // Djibouti / lang=French
            new Locale("en", "DM"), // Dominica / lang=English
            new Locale("es", "DO"), // Dominican Republic / lang=Spanish
            new Locale("es", "EC"), // Ecuador / lang=Spanish
            new Locale("ar", "EG"), // Egypt / lang=Arabic
            new Locale("es", "SV"), // El Salvador / lang=Spanish
            new Locale("es", "GQ"), // Equatorial Guinea / lang=Spanish
            new Locale("ti", "ER"), // Eritrea / lang=Tigrinya
            new Locale("et", "EE"), // Estonia / lang=Estonian
            new Locale("am", "ET"), // Ethiopia / lang=Amharic
            new Locale("en", "FK"), // Falkland Islands (Malvinas) / lang=English
            new Locale("fo", "FO"), // Faroe Islands / lang=Faroese
            new Locale("en", "FJ"), // Fiji / lang=English
            new Locale("fi", "FI"), // Finland / lang=Finnish
            new Locale("fr", "FR"), // France / lang=French
            new Locale("fr", "GF"), // French Guiana / lang=French
            new Locale("fr", "PF"), // French Polynesia / lang=French
            new Locale("fr", "TF"), // French Southern Territories / lang=French
            new Locale("fr", "GA"), // Gabon / lang=French
            new Locale("en", "GM"), // Gambia / lang=English
            new Locale("ka", "GE"), // Georgia / lang=Georgian
            new Locale("de", "DE"), // Germany / lang=German
            new Locale("en", "GH"), // Ghana / lang=English
            new Locale("en", "GI"), // Gibraltar / lang=English
            new Locale("el", "GR"), // Greece / lang=Greek (modern)
            new Locale("kl", "GL"), // Greenland / lang=Kalaallisut
            new Locale("en", "GD"), // Grenada / lang=English
            new Locale("fr", "GP"), // Guadeloupe / lang=French
            new Locale("en", "GU"), // Guam / lang=English
            new Locale("es", "GT"), // Guatemala / lang=Spanish
            new Locale("en", "GG"), // Guernsey / lang=English
            new Locale("fr", "GN"), // Guinea / lang=French
            new Locale("pt", "GW"), // Guinea-Bissau / lang=Portuguese
            new Locale("en", "GY"), // Guyana / lang=English
            new Locale("fr", "HT"), // Haiti / lang=French
            new Locale("la", "VA"), // Holy See / lang=Latin
            new Locale("es", "HN"), // Honduras / lang=Spanish
            new Locale("en", "HK"), // Hong Kong / lang=English
            new Locale("hu", "HU"), // Hungary / lang=Hungarian
            new Locale("is", "IS"), // Iceland / lang=Icelandic
            new Locale("hi", "IN"), // India / lang=Hindi
            new Locale("id", "ID"), // Indonesia / lang=Indonesian
            new Locale("fr", "CI"), // Côte d'Ivoire / lang=French
            new Locale("fa", "IR"), // Iran (Islamic Republic of) / lang=Persian (Farsi)
            new Locale("ar", "IQ"), // Iraq / lang=Arabic
            new Locale("ga", "IE"), // Ireland / lang=Irish
            new Locale("en", "IM"), // Isle of Man / lang=English
            new Locale("he", "IL"), // Israel / lang=Hebrew (modern)
            new Locale("it", "IT"), // Italy / lang=Italian
            new Locale("en", "JM"), // Jamaica / lang=English
            new Locale("ja", "JP"), // Japan / lang=Japanese
            new Locale("en", "JE"), // Jersey / lang=English
            new Locale("ar", "JO"), // Jordan / lang=Arabic
            new Locale("kk", "KZ"), // Kazakhstan / lang=Kazakh
            new Locale("en", "KE"), // Kenya / lang=English
            new Locale("en", "KI"), // Kiribati / lang=English
            new Locale("ar", "KW"), // Kuwait / lang=Arabic
            new Locale("ky", "KG"), // Kyrgyzstan / lang=Kyrgyz
            new Locale("lo", "LA"), // Lao People's Democratic Republic / lang=Lao
            new Locale("lv", "LV"), // Latvia / lang=Latvian
            new Locale("ar", "LB"), // Lebanon / lang=Arabic
            new Locale("en", "LS"), // Lesotho / lang=English
            new Locale("en", "LR"), // Liberia / lang=English
            new Locale("ar", "LY"), // Libya / lang=Arabic
            new Locale("de", "LI"), // Liechtenstein / lang=German
            new Locale("lt", "LT"), // Lithuania / lang=Lithuanian
            new Locale("fr", "LU"), // Luxembourg / lang=French
            new Locale("zh", "MO"), // Macao / lang=Chinese
            new Locale("mk", "MK"), // Macedonia (the former Yugoslav Republic of) / lang=Macedonian
            new Locale("fr", "MG"), // Madagascar / lang=French
            new Locale("en", "MW"), // Malawi / lang=English
            new Locale("en", "MY"), // Malaysia / lang=Malaysian
            new Locale("dv", "MV"), // Maldives / lang=Divehi
            new Locale("fr", "ML"), // Mali / lang=French
            new Locale("mt", "MT"), // Malta / lang=Maltese
            new Locale("en", "MH"), // Marshall Islands / lang=English
            new Locale("fr", "MQ"), // Martinique / lang=French
            new Locale("ar", "MR"), // Mauritania / lang=Arabic
            new Locale("en", "MU"), // Mauritius / lang=English
            new Locale("fr", "YT"), // Mayotte / lang=French
            new Locale("es", "MX"), // Mexico / lang=Spanish
            new Locale("en", "FM"), // Micronesia (Federated States of) / lang=English
            new Locale("ro", "MD"), // Moldova (Republic of) / lang=Romanian
            new Locale("fr", "MC"), // Monaco / lang=French
            new Locale("mn", "MN"), // Mongolia / lang=Mongolian
            new Locale("sr", "ME"), // Montenegro / lang=Serbian
            new Locale("en", "MS"), // Montserrat / lang=English
            new Locale("ar", "MA"), // Morocco / lang=Arabic
            new Locale("pt", "MZ"), // Mozambique / lang=Portuguese
            new Locale("my", "MM"), // Myanmar / lang=Burmese
            new Locale("en", "NA"), // Namibia / lang=English
            new Locale("en", "NR"), // Nauru / lang=English
            new Locale("ne", "NP"), // Nepal / lang=Nepali
            new Locale("nl", "NL"), // Netherlands / lang=Dutch
            new Locale("fr", "NC"), // New Caledonia / lang=French
            new Locale("en", "NZ"), // New Zealand / lang=English
            new Locale("es", "NI"), // Nicaragua / lang=Spanish
            new Locale("fr", "NE"), // Niger / lang=French
            new Locale("en", "NG"), // Nigeria / lang=English
            new Locale("en", "NU"), // Niue / lang=English
            new Locale("en", "NF"), // Norfolk Island / lang=English
            new Locale("ko", "KP"), // Korea (Democratic People's Republic of) / lang=Korean
            new Locale("en", "MP"), // Northern Mariana Islands / lang=English
            new Locale("no", "NO"), // Norway / lang=Norwegian
            new Locale("ar", "OM"), // Oman / lang=Arabic
            new Locale("en", "PK"), // Pakistan / lang=English
            new Locale("en", "PW"), // Palau / lang=English
            new Locale("ar", "PS"), // Palestine, State of / lang=Arabic
            new Locale("es", "PA"), // Panama / lang=Spanish
            new Locale("en", "PG"), // Papua New Guinea / lang=English
            new Locale("es", "PY"), // Paraguay / lang=Spanish
            new Locale("es", "PE"), // Peru / lang=Spanish
            new Locale("en", "PH"), // Philippines / lang=English
            new Locale("en", "PN"), // Pitcairn / lang=English
            new Locale("pl", "PL"), // Poland / lang=Polish
            new Locale("pt", "PT"), // Portugal / lang=Portuguese
            new Locale("es", "PR"), // Puerto Rico / lang=Spanish
            new Locale("ar", "QA"), // Qatar / lang=Arabic
            new Locale("sq", "XK"), // Republic of Kosovo / lang=Albanian
            new Locale("fr", "RE"), // Réunion / lang=French
            new Locale("ro", "RO"), // Romania / lang=Romanian
            new Locale("ru", "RU"), // Russian Federation / lang=Russian
            new Locale("rw", "RW"), // Rwanda / lang=Kinyarwanda
            new Locale("fr", "BL"), // Saint Barthélemy / lang=French
            new Locale("en", "SH"), // Saint Helena, Ascension and Tristan da Cunha / lang=English
            new Locale("en", "KN"), // Saint Kitts and Nevis / lang=English
            new Locale("en", "LC"), // Saint Lucia / lang=English
            new Locale("en", "MF"), // Saint Martin (French part) / lang=English
            new Locale("fr", "PM"), // Saint Pierre and Miquelon / lang=French
            new Locale("en", "VC"), // Saint Vincent and the Grenadines / lang=English
            new Locale("sm", "WS"), // Samoa / lang=Samoan
            new Locale("it", "SM"), // San Marino / lang=Italian
            new Locale("pt", "ST"), // Sao Tome and Principe / lang=Portuguese
            new Locale("ar", "SA"), // Saudi Arabia / lang=Arabic
            new Locale("fr", "SN"), // Senegal / lang=French
            new Locale("sr", "RS"), // Serbia / lang=Serbian
            new Locale("fr", "SC"), // Seychelles / lang=French
            new Locale("en", "SL"), // Sierra Leone / lang=English
            new Locale("en", "SG"), // Singapore / lang=English
            new Locale("nl", "SX"), // Sint Maarten (Dutch part) / lang=Dutch
            new Locale("sk", "SK"), // Slovakia / lang=Slovak
            new Locale("sl", "SI"), // Slovenia / lang=Slovene
            new Locale("en", "SB"), // Solomon Islands / lang=English
            new Locale("so", "SO"), // Somalia / lang=Somali
            new Locale("af", "ZA"), // South Africa / lang=Afrikaans
            new Locale("en", "GS"), // South Georgia and the South Sandwich Islands / lang=English
            new Locale("ko", "KR"), // Korea (Republic of) / lang=Korean
            new Locale("en", "SS"), // South Sudan / lang=English
            new Locale("es", "ES"), // Spain / lang=Spanish
            new Locale("si", "LK"), // Sri Lanka / lang=Sinhalese
            new Locale("ar", "SD"), // Sudan / lang=Arabic
            new Locale("nl", "SR"), // Suriname / lang=Dutch
            new Locale("no", "SJ"), // Svalbard and Jan Mayen / lang=Norwegian
            new Locale("en", "SZ"), // Swaziland / lang=English
            new Locale("sv", "SE"), // Sweden / lang=Swedish
            new Locale("de", "CH"), // Switzerland / lang=German
            new Locale("ar", "SY"), // Syrian Arab Republic / lang=Arabic
            new Locale("zh", "TW"), // Taiwan / lang=Chinese
            new Locale("tg", "TJ"), // Tajikistan / lang=Tajik
            new Locale("sw", "TZ"), // Tanzania, United Republic of / lang=Swahili
            new Locale("th", "TH"), // Thailand / lang=Thai
            new Locale("pt", "TL"), // Timor-Leste / lang=Portuguese
            new Locale("fr", "TG"), // Togo / lang=French
            new Locale("en", "TK"), // Tokelau / lang=English
            new Locale("en", "TO"), // Tonga / lang=English
            new Locale("en", "TT"), // Trinidad and Tobago / lang=English
            new Locale("ar", "TN"), // Tunisia / lang=Arabic
            new Locale("tr", "TR"), // Turkey / lang=Turkish
            new Locale("tk", "TM"), // Turkmenistan / lang=Turkmen
            new Locale("en", "TC"), // Turks and Caicos Islands / lang=English
            new Locale("en", "TV"), // Tuvalu / lang=English
            new Locale("en", "UG"), // Uganda / lang=English
            new Locale("uk", "UA"), // Ukraine / lang=Ukrainian
            new Locale("ar", "AE"), // United Arab Emirates / lang=Arabic
            new Locale("en", "GB"), // United Kingdom of Great Britain and Northern Ireland / lang=English
            new Locale("en", "US"), // United States of America / lang=English
            new Locale("es", "UY"), // Uruguay / lang=Spanish
            new Locale("uz", "UZ"), // Uzbekistan / lang=Uzbek
            new Locale("bi", "VU"), // Vanuatu / lang=Bislama
            new Locale("es", "VE"), // Venezuela (Bolivarian Republic of) / lang=Spanish
            new Locale("vi", "VN"), // Vietnam / lang=Vietnamese
            new Locale("fr", "WF"), // Wallis and Futuna / lang=French
            new Locale("es", "EH"), // Western Sahara / lang=Spanish
            new Locale("ar", "YE"), // Yemen / lang=Arabic
            new Locale("en", "ZM"), // Zambia / lang=English
            new Locale("en", "ZW")  // Zimbabwe / lang=English
    );
}
