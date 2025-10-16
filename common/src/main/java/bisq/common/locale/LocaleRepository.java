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
            new Locale.Builder().setLanguage("ps").setRegion("AF").build(), // Afghanistan / lang=Pashto
            new Locale.Builder().setLanguage("sv").setRegion("AX").build(), // Åland Islands / lang=Swedish
            new Locale.Builder().setLanguage("sq").setRegion("AL").build(), // Albania / lang=Albanian
            new Locale.Builder().setLanguage("ar").setRegion("DZ").build(), // Algeria / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("AS").build(), // American Samoa / lang=English
            new Locale.Builder().setLanguage("ca").setRegion("AD").build(), // Andorra / lang=Catalan
            new Locale.Builder().setLanguage("pt").setRegion("AO").build(), // Angola / lang=Portuguese
            new Locale.Builder().setLanguage("en").setRegion("AI").build(), // Anguilla / lang=English
            new Locale.Builder().setLanguage("en").setRegion("AG").build(), // Antigua and Barbuda / lang=English
            new Locale.Builder().setLanguage("es").setRegion("AR").build(), // Argentina / lang=Spanish
            new Locale.Builder().setLanguage("hy").setRegion("AM").build(), // Armenia / lang=Armenian
            new Locale.Builder().setLanguage("nl").setRegion("AW").build(), // Aruba / lang=Dutch
            new Locale.Builder().setLanguage("en").setRegion("AU").build(), // Australia / lang=English
            new Locale.Builder().setLanguage("de").setRegion("AT").build(), // Austria / lang=German
            new Locale.Builder().setLanguage("az").setRegion("AZ").build(), // Azerbaijan / lang=Azerbaijani
            new Locale.Builder().setLanguage("en").setRegion("BS").build(), // Bahamas / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("BH").build(), // Bahrain / lang=Arabic
            new Locale.Builder().setLanguage("bn").setRegion("BD").build(), // Bangladesh / lang=Bengali
            new Locale.Builder().setLanguage("en").setRegion("BB").build(), // Barbados / lang=English
            new Locale.Builder().setLanguage("be").setRegion("BY").build(), // Belarus / lang=Belarusian
            new Locale.Builder().setLanguage("nl").setRegion("BE").build(), // Belgium / lang=Dutch
            new Locale.Builder().setLanguage("en").setRegion("BZ").build(), // Belize / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("BJ").build(), // Benin / lang=French
            new Locale.Builder().setLanguage("en").setRegion("BM").build(), // Bermuda / lang=English
            new Locale.Builder().setLanguage("dz").setRegion("BT").build(), // Bhutan / lang=Dzongkha
            new Locale.Builder().setLanguage("es").setRegion("BO").build(), // Bolivia (Plurinational State of) / lang=Spanish
            new Locale.Builder().setLanguage("nl").setRegion("BQ").build(), // Bonaire, Sint Eustatius and Saba / lang=Dutch
            new Locale.Builder().setLanguage("bs").setRegion("BA").build(), // Bosnia and Herzegovina / lang=Bosnian
            new Locale.Builder().setLanguage("en").setRegion("BW").build(), // Botswana / lang=English
            new Locale.Builder().setLanguage("pt").setRegion("BR").build(), // Brazil / lang=Portuguese
            new Locale.Builder().setLanguage("en").setRegion("IO").build(), // British Indian Ocean Territory / lang=English
            new Locale.Builder().setLanguage("en").setRegion("UM").build(), // United States Minor Outlying Islands / lang=English
            new Locale.Builder().setLanguage("en").setRegion("VG").build(), // Virgin Islands (British) / lang=English
            new Locale.Builder().setLanguage("en").setRegion("VI").build(), // Virgin Islands (U.S.) / lang=English
            new Locale.Builder().setLanguage("ms").setRegion("BN").build(), // Brunei Darussalam / lang=Malay
            new Locale.Builder().setLanguage("bg").setRegion("BG").build(), // Bulgaria / lang=Bulgarian
            new Locale.Builder().setLanguage("fr").setRegion("BF").build(), // Burkina Faso / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("BI").build(), // Burundi / lang=French
            new Locale.Builder().setLanguage("km").setRegion("KH").build(), // Cambodia / lang=Khmer
            new Locale.Builder().setLanguage("en").setRegion("CM").build(), // Cameroon / lang=English
            new Locale.Builder().setLanguage("en").setRegion("CA").build(), // Canada / lang=English
            new Locale.Builder().setLanguage("pt").setRegion("CV").build(), // Cabo Verde / lang=Portuguese
            new Locale.Builder().setLanguage("en").setRegion("KY").build(), // Cayman Islands / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("CF").build(), // Central African Republic / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("TD").build(), // Chad / lang=French
            new Locale.Builder().setLanguage("es").setRegion("CL").build(), // Chile / lang=Spanish
            new Locale.Builder().setLanguage("zh").setRegion("CN").build(), // China / lang=Chinese
            new Locale.Builder().setLanguage("en").setRegion("CX").build(), // Christmas Island / lang=English
            new Locale.Builder().setLanguage("en").setRegion("CC").build(), // Cocos (Keeling) Islands / lang=English
            new Locale.Builder().setLanguage("es").setRegion("CO").build(), // Colombia / lang=Spanish
            new Locale.Builder().setLanguage("ar").setRegion("KM").build(), // Comoros / lang=Arabic
            new Locale.Builder().setLanguage("fr").setRegion("CG").build(), // Congo / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("CD").build(), // Congo (Democratic Republic of the) / lang=French
            new Locale.Builder().setLanguage("en").setRegion("CK").build(), // Cook Islands / lang=English
            new Locale.Builder().setLanguage("es").setRegion("CR").build(), // Costa Rica / lang=Spanish
            new Locale.Builder().setLanguage("hr").setRegion("HR").build(), // Croatia / lang=Croatian
            new Locale.Builder().setLanguage("es").setRegion("CU").build(), // Cuba / lang=Spanish
            new Locale.Builder().setLanguage("nl").setRegion("CW").build(), // Curaçao / lang=Dutch
            new Locale.Builder().setLanguage("el").setRegion("CY").build(), // Cyprus / lang=Greek (modern)
            new Locale.Builder().setLanguage("cs").setRegion("CZ").build(), // Czech Republic / lang=Czech
            new Locale.Builder().setLanguage("da").setRegion("DK").build(), // Denmark / lang=Danish
            new Locale.Builder().setLanguage("fr").setRegion("DJ").build(), // Djibouti / lang=French
            new Locale.Builder().setLanguage("en").setRegion("DM").build(), // Dominica / lang=English
            new Locale.Builder().setLanguage("es").setRegion("DO").build(), // Dominican Republic / lang=Spanish
            new Locale.Builder().setLanguage("es").setRegion("EC").build(), // Ecuador / lang=Spanish
            new Locale.Builder().setLanguage("ar").setRegion("EG").build(), // Egypt / lang=Arabic
            new Locale.Builder().setLanguage("es").setRegion("SV").build(), // El Salvador / lang=Spanish
            new Locale.Builder().setLanguage("es").setRegion("GQ").build(), // Equatorial Guinea / lang=Spanish
            new Locale.Builder().setLanguage("ti").setRegion("ER").build(), // Eritrea / lang=Tigrinya
            new Locale.Builder().setLanguage("et").setRegion("EE").build(), // Estonia / lang=Estonian
            new Locale.Builder().setLanguage("am").setRegion("ET").build(), // Ethiopia / lang=Amharic
            new Locale.Builder().setLanguage("en").setRegion("FK").build(), // Falkland Islands (Malvinas) / lang=English
            new Locale.Builder().setLanguage("fo").setRegion("FO").build(), // Faroe Islands / lang=Faroese
            new Locale.Builder().setLanguage("en").setRegion("FJ").build(), // Fiji / lang=English
            new Locale.Builder().setLanguage("fi").setRegion("FI").build(), // Finland / lang=Finnish
            new Locale.Builder().setLanguage("fr").setRegion("FR").build(), // France / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("GF").build(), // French Guiana / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("PF").build(), // French Polynesia / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("TF").build(), // French Southern Territories / lang=French
            new Locale.Builder().setLanguage("fr").setRegion("GA").build(), // Gabon / lang=French
            new Locale.Builder().setLanguage("en").setRegion("GM").build(), // Gambia / lang=English
            new Locale.Builder().setLanguage("ka").setRegion("GE").build(), // Georgia / lang=Georgian
            new Locale.Builder().setLanguage("de").setRegion("DE").build(), // Germany / lang=German
            new Locale.Builder().setLanguage("en").setRegion("GH").build(), // Ghana / lang=English
            new Locale.Builder().setLanguage("en").setRegion("GI").build(), // Gibraltar / lang=English
            new Locale.Builder().setLanguage("el").setRegion("GR").build(), // Greece / lang=Greek (modern)
            new Locale.Builder().setLanguage("kl").setRegion("GL").build(), // Greenland / lang=Kalaallisut
            new Locale.Builder().setLanguage("en").setRegion("GD").build(), // Grenada / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("GP").build(), // Guadeloupe / lang=French
            new Locale.Builder().setLanguage("en").setRegion("GU").build(), // Guam / lang=English
            new Locale.Builder().setLanguage("es").setRegion("GT").build(), // Guatemala / lang=Spanish
            new Locale.Builder().setLanguage("en").setRegion("GG").build(), // Guernsey / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("GN").build(), // Guinea / lang=French
            new Locale.Builder().setLanguage("pt").setRegion("GW").build(), // Guinea-Bissau / lang=Portuguese
            new Locale.Builder().setLanguage("en").setRegion("GY").build(), // Guyana / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("HT").build(), // Haiti / lang=French
            new Locale.Builder().setLanguage("la").setRegion("VA").build(), // Holy See / lang=Latin
            new Locale.Builder().setLanguage("es").setRegion("HN").build(), // Honduras / lang=Spanish
            new Locale.Builder().setLanguage("en").setRegion("HK").build(), // Hong Kong / lang=English
            new Locale.Builder().setLanguage("hu").setRegion("HU").build(), // Hungary / lang=Hungarian
            new Locale.Builder().setLanguage("is").setRegion("IS").build(), // Iceland / lang=Icelandic
            new Locale.Builder().setLanguage("hi").setRegion("IN").build(), // India / lang=Hindi
            new Locale.Builder().setLanguage("id").setRegion("ID").build(), // Indonesia / lang=Indonesian
            new Locale.Builder().setLanguage("fr").setRegion("CI").build(), // Côte d'Ivoire / lang=French
            new Locale.Builder().setLanguage("fa").setRegion("IR").build(), // Iran (Islamic Republic of) / lang=Persian (Farsi)
            new Locale.Builder().setLanguage("ar").setRegion("IQ").build(), // Iraq / lang=Arabic
            new Locale.Builder().setLanguage("ga").setRegion("IE").build(), // Ireland / lang=Irish
            new Locale.Builder().setLanguage("en").setRegion("IM").build(), // Isle of Man / lang=English
            new Locale.Builder().setLanguage("he").setRegion("IL").build(), // Israel / lang=Hebrew (modern)
            new Locale.Builder().setLanguage("it").setRegion("IT").build(), // Italy / lang=Italian
            new Locale.Builder().setLanguage("en").setRegion("JM").build(), // Jamaica / lang=English
            new Locale.Builder().setLanguage("ja").setRegion("JP").build(), // Japan / lang=Japanese
            new Locale.Builder().setLanguage("en").setRegion("JE").build(), // Jersey / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("JO").build(), // Jordan / lang=Arabic
            new Locale.Builder().setLanguage("kk").setRegion("KZ").build(), // Kazakhstan / lang=Kazakh
            new Locale.Builder().setLanguage("en").setRegion("KE").build(), // Kenya / lang=English
            new Locale.Builder().setLanguage("en").setRegion("KI").build(), // Kiribati / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("KW").build(), // Kuwait / lang=Arabic
            new Locale.Builder().setLanguage("ky").setRegion("KG").build(), // Kyrgyzstan / lang=Kyrgyz
            new Locale.Builder().setLanguage("lo").setRegion("LA").build(), // Lao People's Democratic Republic / lang=Lao
            new Locale.Builder().setLanguage("lv").setRegion("LV").build(), // Latvia / lang=Latvian
            new Locale.Builder().setLanguage("ar").setRegion("LB").build(), // Lebanon / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("LS").build(), // Lesotho / lang=English
            new Locale.Builder().setLanguage("en").setRegion("LR").build(), // Liberia / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("LY").build(), // Libya / lang=Arabic
            new Locale.Builder().setLanguage("de").setRegion("LI").build(), // Liechtenstein / lang=German
            new Locale.Builder().setLanguage("lt").setRegion("LT").build(), // Lithuania / lang=Lithuanian
            new Locale.Builder().setLanguage("fr").setRegion("LU").build(), // Luxembourg / lang=French
            new Locale.Builder().setLanguage("zh").setRegion("MO").build(), // Macao / lang=Chinese
            new Locale.Builder().setLanguage("mk").setRegion("MK").build(), // Macedonia (the former Yugoslav Republic of) / lang=Macedonian
            new Locale.Builder().setLanguage("fr").setRegion("MG").build(), // Madagascar / lang=French
            new Locale.Builder().setLanguage("en").setRegion("MW").build(), // Malawi / lang=English
            new Locale.Builder().setLanguage("en").setRegion("MY").build(), // Malaysia / lang=Malaysian
            new Locale.Builder().setLanguage("dv").setRegion("MV").build(), // Maldives / lang=Divehi
            new Locale.Builder().setLanguage("fr").setRegion("ML").build(), // Mali / lang=French
            new Locale.Builder().setLanguage("mt").setRegion("MT").build(), // Malta / lang=Maltese
            new Locale.Builder().setLanguage("en").setRegion("MH").build(), // Marshall Islands / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("MQ").build(), // Martinique / lang=French
            new Locale.Builder().setLanguage("ar").setRegion("MR").build(), // Mauritania / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("MU").build(), // Mauritius / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("YT").build(), // Mayotte / lang=French
            new Locale.Builder().setLanguage("es").setRegion("MX").build(), // Mexico / lang=Spanish
            new Locale.Builder().setLanguage("en").setRegion("FM").build(), // Micronesia (Federated States of) / lang=English
            new Locale.Builder().setLanguage("ro").setRegion("MD").build(), // Moldova (Republic of) / lang=Romanian
            new Locale.Builder().setLanguage("fr").setRegion("MC").build(), // Monaco / lang=French
            new Locale.Builder().setLanguage("mn").setRegion("MN").build(), // Mongolia / lang=Mongolian
            new Locale.Builder().setLanguage("sr").setRegion("ME").build(), // Montenegro / lang=Serbian
            new Locale.Builder().setLanguage("en").setRegion("MS").build(), // Montserrat / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("MA").build(), // Morocco / lang=Arabic
            new Locale.Builder().setLanguage("pt").setRegion("MZ").build(), // Mozambique / lang=Portuguese
            new Locale.Builder().setLanguage("my").setRegion("MM").build(), // Myanmar / lang=Burmese
            new Locale.Builder().setLanguage("en").setRegion("NA").build(), // Namibia / lang=English
            new Locale.Builder().setLanguage("en").setRegion("NR").build(), // Nauru / lang=English
            new Locale.Builder().setLanguage("ne").setRegion("NP").build(), // Nepal / lang=Nepali
            new Locale.Builder().setLanguage("nl").setRegion("NL").build(), // Netherlands / lang=Dutch
            new Locale.Builder().setLanguage("fr").setRegion("NC").build(), // New Caledonia / lang=French
            new Locale.Builder().setLanguage("en").setRegion("NZ").build(), // New Zealand / lang=English
            new Locale.Builder().setLanguage("es").setRegion("NI").build(), // Nicaragua / lang=Spanish
            new Locale.Builder().setLanguage("fr").setRegion("NE").build(), // Niger / lang=French
            new Locale.Builder().setLanguage("en").setRegion("NG").build(), // Nigeria / lang=English
            new Locale.Builder().setLanguage("en").setRegion("NU").build(), // Niue / lang=English
            new Locale.Builder().setLanguage("en").setRegion("NF").build(), // Norfolk Island / lang=English
            new Locale.Builder().setLanguage("ko").setRegion("KP").build(), // Korea (Democratic People's Republic of) / lang=Korean
            new Locale.Builder().setLanguage("en").setRegion("MP").build(), // Northern Mariana Islands / lang=English
            new Locale.Builder().setLanguage("no").setRegion("NO").build(), // Norway / lang=Norwegian
            new Locale.Builder().setLanguage("ar").setRegion("OM").build(), // Oman / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("PK").build(), // Pakistan / lang=English
            new Locale.Builder().setLanguage("en").setRegion("PW").build(), // Palau / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("PS").build(), // Palestine, State of / lang=Arabic
            new Locale.Builder().setLanguage("es").setRegion("PA").build(), // Panama / lang=Spanish
            new Locale.Builder().setLanguage("en").setRegion("PG").build(), // Papua New Guinea / lang=English
            new Locale.Builder().setLanguage("es").setRegion("PY").build(), // Paraguay / lang=Spanish
            new Locale.Builder().setLanguage("es").setRegion("PE").build(), // Peru / lang=Spanish
            new Locale.Builder().setLanguage("en").setRegion("PH").build(), // Philippines / lang=English
            new Locale.Builder().setLanguage("en").setRegion("PN").build(), // Pitcairn / lang=English
            new Locale.Builder().setLanguage("pl").setRegion("PL").build(), // Poland / lang=Polish
            new Locale.Builder().setLanguage("pt").setRegion("PT").build(), // Portugal / lang=Portuguese
            new Locale.Builder().setLanguage("es").setRegion("PR").build(), // Puerto Rico / lang=Spanish
            new Locale.Builder().setLanguage("ar").setRegion("QA").build(), // Qatar / lang=Arabic
            new Locale.Builder().setLanguage("sq").setRegion("XK").build(), // Republic of Kosovo / lang=Albanian
            new Locale.Builder().setLanguage("fr").setRegion("RE").build(), // Réunion / lang=French
            new Locale.Builder().setLanguage("ro").setRegion("RO").build(), // Romania / lang=Romanian
            new Locale.Builder().setLanguage("ru").setRegion("RU").build(), // Russian Federation / lang=Russian
            new Locale.Builder().setLanguage("rw").setRegion("RW").build(), // Rwanda / lang=Kinyarwanda
            new Locale.Builder().setLanguage("fr").setRegion("BL").build(), // Saint Barthélemy / lang=French
            new Locale.Builder().setLanguage("en").setRegion("SH").build(), // Saint Helena, Ascension and Tristan da Cunha / lang=English
            new Locale.Builder().setLanguage("en").setRegion("KN").build(), // Saint Kitts and Nevis / lang=English
            new Locale.Builder().setLanguage("en").setRegion("LC").build(), // Saint Lucia / lang=English
            new Locale.Builder().setLanguage("en").setRegion("MF").build(), // Saint Martin (French part) / lang=English
            new Locale.Builder().setLanguage("fr").setRegion("PM").build(), // Saint Pierre and Miquelon / lang=French
            new Locale.Builder().setLanguage("en").setRegion("VC").build(), // Saint Vincent and the Grenadines / lang=English
            new Locale.Builder().setLanguage("sm").setRegion("WS").build(), // Samoa / lang=Samoan
            new Locale.Builder().setLanguage("it").setRegion("SM").build(), // San Marino / lang=Italian
            new Locale.Builder().setLanguage("pt").setRegion("ST").build(), // Sao Tome and Principe / lang=Portuguese
            new Locale.Builder().setLanguage("ar").setRegion("SA").build(), // Saudi Arabia / lang=Arabic
            new Locale.Builder().setLanguage("fr").setRegion("SN").build(), // Senegal / lang=French
            new Locale.Builder().setLanguage("sr").setRegion("RS").build(), // Serbia / lang=Serbian
            new Locale.Builder().setLanguage("fr").setRegion("SC").build(), // Seychelles / lang=French
            new Locale.Builder().setLanguage("en").setRegion("SL").build(), // Sierra Leone / lang=English
            new Locale.Builder().setLanguage("en").setRegion("SG").build(), // Singapore / lang=English
            new Locale.Builder().setLanguage("nl").setRegion("SX").build(), // Sint Maarten (Dutch part) / lang=Dutch
            new Locale.Builder().setLanguage("sk").setRegion("SK").build(), // Slovakia / lang=Slovak
            new Locale.Builder().setLanguage("sl").setRegion("SI").build(), // Slovenia / lang=Slovene
            new Locale.Builder().setLanguage("en").setRegion("SB").build(), // Solomon Islands / lang=English
            new Locale.Builder().setLanguage("so").setRegion("SO").build(), // Somalia / lang=Somali
            new Locale.Builder().setLanguage("af").setRegion("ZA").build(), // South Africa / lang=Afrikaans
            new Locale.Builder().setLanguage("en").setRegion("GS").build(), // South Georgia and the South Sandwich Islands / lang=English
            new Locale.Builder().setLanguage("ko").setRegion("KR").build(), // Korea (Republic of) / lang=Korean
            new Locale.Builder().setLanguage("en").setRegion("SS").build(), // South Sudan / lang=English
            new Locale.Builder().setLanguage("es").setRegion("ES").build(), // Spain / lang=Spanish
            new Locale.Builder().setLanguage("si").setRegion("LK").build(), // Sri Lanka / lang=Sinhalese
            new Locale.Builder().setLanguage("ar").setRegion("SD").build(), // Sudan / lang=Arabic
            new Locale.Builder().setLanguage("nl").setRegion("SR").build(), // Suriname / lang=Dutch
            new Locale.Builder().setLanguage("no").setRegion("SJ").build(), // Svalbard and Jan Mayen / lang=Norwegian
            new Locale.Builder().setLanguage("en").setRegion("SZ").build(), // Swaziland / lang=English
            new Locale.Builder().setLanguage("sv").setRegion("SE").build(), // Sweden / lang=Swedish
            new Locale.Builder().setLanguage("de").setRegion("CH").build(), // Switzerland / lang=German
            new Locale.Builder().setLanguage("ar").setRegion("SY").build(), // Syrian Arab Republic / lang=Arabic
            new Locale.Builder().setLanguage("zh").setRegion("TW").build(), // Taiwan / lang=Chinese
            new Locale.Builder().setLanguage("tg").setRegion("TJ").build(), // Tajikistan / lang=Tajik
            new Locale.Builder().setLanguage("sw").setRegion("TZ").build(), // Tanzania, United Republic of / lang=Swahili
            new Locale.Builder().setLanguage("th").setRegion("TH").build(), // Thailand / lang=Thai
            new Locale.Builder().setLanguage("pt").setRegion("TL").build(), // Timor-Leste / lang=Portuguese
            new Locale.Builder().setLanguage("fr").setRegion("TG").build(), // Togo / lang=French
            new Locale.Builder().setLanguage("en").setRegion("TK").build(), // Tokelau / lang=English
            new Locale.Builder().setLanguage("en").setRegion("TO").build(), // Tonga / lang=English
            new Locale.Builder().setLanguage("en").setRegion("TT").build(), // Trinidad and Tobago / lang=English
            new Locale.Builder().setLanguage("ar").setRegion("TN").build(), // Tunisia / lang=Arabic
            new Locale.Builder().setLanguage("tr").setRegion("TR").build(), // Turkey / lang=Turkish
            new Locale.Builder().setLanguage("tk").setRegion("TM").build(), // Turkmenistan / lang=Turkmen
            new Locale.Builder().setLanguage("en").setRegion("TC").build(), // Turks and Caicos Islands / lang=English
            new Locale.Builder().setLanguage("en").setRegion("TV").build(), // Tuvalu / lang=English
            new Locale.Builder().setLanguage("en").setRegion("UG").build(), // Uganda / lang=English
            new Locale.Builder().setLanguage("uk").setRegion("UA").build(), // Ukraine / lang=Ukrainian
            new Locale.Builder().setLanguage("ar").setRegion("AE").build(), // United Arab Emirates / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("GB").build(), // United Kingdom of Great Britain and Northern Ireland / lang=English
            new Locale.Builder().setLanguage("en").setRegion("US").build(), // United States of America / lang=English
            new Locale.Builder().setLanguage("es").setRegion("UY").build(), // Uruguay / lang=Spanish
            new Locale.Builder().setLanguage("uz").setRegion("UZ").build(), // Uzbekistan / lang=Uzbek
            new Locale.Builder().setLanguage("bi").setRegion("VU").build(), // Vanuatu / lang=Bislama
            new Locale.Builder().setLanguage("es").setRegion("VE").build(), // Venezuela (Bolivarian Republic of) / lang=Spanish
            new Locale.Builder().setLanguage("vi").setRegion("VN").build(), // Vietnam / lang=Vietnamese
            new Locale.Builder().setLanguage("fr").setRegion("WF").build(), // Wallis and Futuna / lang=French
            new Locale.Builder().setLanguage("es").setRegion("EH").build(), // Western Sahara / lang=Spanish
            new Locale.Builder().setLanguage("ar").setRegion("YE").build(), // Yemen / lang=Arabic
            new Locale.Builder().setLanguage("en").setRegion("ZM").build(), // Zambia / lang=English
            new Locale.Builder().setLanguage("en").setRegion("ZW").build()  // Zimbabwe / lang=English
    );
}
