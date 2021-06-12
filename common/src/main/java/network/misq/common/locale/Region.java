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

import java.util.HashMap;
import java.util.Map;

public record Region(String code, String name) {
    public static final Map<String, String> NAME_BY_CODE = new HashMap<>();
    // Key is: ISO 3166 code, value is region code as defined in regionCodeToNameMap
    public static final Map<String, String> REGION_CODE_BY_COUNTRY_CODE = new HashMap<>();

    static {
        NAME_BY_CODE.put("AM", "Americas");
        NAME_BY_CODE.put("AF", "Africa");
        NAME_BY_CODE.put("EU", "Europe");
        NAME_BY_CODE.put("AS", "Asia");
        NAME_BY_CODE.put("OC", "Oceania");

        // Data extracted from https://restcountries.eu/rest/v2/all?fields=name;region;subregion;alpha2Code;languages
        REGION_CODE_BY_COUNTRY_CODE.put("AF", "AS"); // name=Afghanistan / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("AX", "EU"); // name=Åland Islands / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("AL", "EU"); // name=Albania / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("DZ", "AF"); // name=Algeria / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("AS", "OC"); // name=American Samoa / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("AD", "EU"); // name=Andorra / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("AO", "AF"); // name=Angola / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("AI", "AM"); // name=Anguilla / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("AG", "AM"); // name=Antigua and Barbuda / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("AR", "AM"); // name=Argentina / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("AM", "AS"); // name=Armenia / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("AW", "AM"); // name=Aruba / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("AU", "OC"); // name=Australia / region=Oceania / subregion=Australia and New Zealand
        REGION_CODE_BY_COUNTRY_CODE.put("AT", "EU"); // name=Austria / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("AZ", "AS"); // name=Azerbaijan / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("BS", "AM"); // name=Bahamas / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("BH", "AS"); // name=Bahrain / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("BD", "AS"); // name=Bangladesh / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("BB", "AM"); // name=Barbados / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("BY", "EU"); // name=Belarus / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("BE", "EU"); // name=Belgium / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("BZ", "AM"); // name=Belize / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("BJ", "AF"); // name=Benin / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("BM", "AM"); // name=Bermuda / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("BT", "AS"); // name=Bhutan / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("BO", "AM"); // name=Bolivia (Plurinational State of) / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("BQ", "AM"); // name=Bonaire, Sint Eustatius and Saba / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("BA", "EU"); // name=Bosnia and Herzegovina / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("BW", "AF"); // name=Botswana / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("BR", "AM"); // name=Brazil / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("IO", "AF"); // name=British Indian Ocean Territory / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("UM", "AM"); // name=United States Minor Outlying Islands / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("VG", "AM"); // name=Virgin Islands (British) / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("VI", "AM"); // name=Virgin Islands (U.S.) / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("BN", "AS"); // name=Brunei Darussalam / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("BG", "EU"); // name=Bulgaria / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("BF", "AF"); // name=Burkina Faso / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("BI", "AF"); // name=Burundi / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("KH", "AS"); // name=Cambodia / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("CM", "AF"); // name=Cameroon / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("CA", "AM"); // name=Canada / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("CV", "AF"); // name=Cabo Verde / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("KY", "AM"); // name=Cayman Islands / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("CF", "AF"); // name=Central African Republic / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("TD", "AF"); // name=Chad / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("CL", "AM"); // name=Chile / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("CN", "AS"); // name=China / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("CX", "OC"); // name=Christmas Island / region=Oceania / subregion=Australia and New Zealand
        REGION_CODE_BY_COUNTRY_CODE.put("CC", "OC"); // name=Cocos (Keeling) Islands / region=Oceania / subregion=Australia and New Zealand
        REGION_CODE_BY_COUNTRY_CODE.put("CO", "AM"); // name=Colombia / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("KM", "AF"); // name=Comoros / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("CG", "AF"); // name=Congo / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("CD", "AF"); // name=Congo (Democratic Republic of the) / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("CK", "OC"); // name=Cook Islands / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("CR", "AM"); // name=Costa Rica / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("HR", "EU"); // name=Croatia / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("CU", "AM"); // name=Cuba / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("CW", "AM"); // name=Curaçao / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("CY", "EU"); // name=Cyprus / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("CZ", "EU"); // name=Czech Republic / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("DK", "EU"); // name=Denmark / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("DJ", "AF"); // name=Djibouti / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("DM", "AM"); // name=Dominica / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("DO", "AM"); // name=Dominican Republic / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("EC", "AM"); // name=Ecuador / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("EG", "AF"); // name=Egypt / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SV", "AM"); // name=El Salvador / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("GQ", "AF"); // name=Equatorial Guinea / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("ER", "AF"); // name=Eritrea / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("EE", "EU"); // name=Estonia / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("ET", "AF"); // name=Ethiopia / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("FK", "AM"); // name=Falkland Islands (Malvinas) / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("FO", "EU"); // name=Faroe Islands / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("FJ", "OC"); // name=Fiji / region=Oceania / subregion=Melanesia
        REGION_CODE_BY_COUNTRY_CODE.put("FI", "EU"); // name=Finland / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("FR", "EU"); // name=France / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("GF", "AM"); // name=French Guiana / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("PF", "OC"); // name=French Polynesia / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("TF", "AF"); // name=French Southern Territories / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GA", "AF"); // name=Gabon / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GM", "AF"); // name=Gambia / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GE", "AS"); // name=Georgia / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("DE", "EU"); // name=Germany / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("GH", "AF"); // name=Ghana / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GI", "EU"); // name=Gibraltar / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("GR", "EU"); // name=Greece / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("GL", "AM"); // name=Greenland / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("GD", "AM"); // name=Grenada / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("GP", "AM"); // name=Guadeloupe / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("GU", "OC"); // name=Guam / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("GT", "AM"); // name=Guatemala / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("GG", "EU"); // name=Guernsey / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("GN", "AF"); // name=Guinea / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GW", "AF"); // name=Guinea-Bissau / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GY", "AM"); // name=Guyana / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("HT", "AM"); // name=Haiti / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("VA", "EU"); // name=Holy See / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("HN", "AM"); // name=Honduras / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("HK", "AS"); // name=Hong Kong / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("HU", "EU"); // name=Hungary / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("IS", "EU"); // name=Iceland / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("IN", "AS"); // name=India / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("ID", "AS"); // name=Indonesia / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("CI", "AF"); // name=Côte d'Ivoire / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("IR", "AS"); // name=Iran (Islamic Republic of) / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("IQ", "AS"); // name=Iraq / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("IE", "EU"); // name=Ireland / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("IM", "EU"); // name=Isle of Man / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("IL", "AS"); // name=Israel / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("IT", "EU"); // name=Italy / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("JM", "AM"); // name=Jamaica / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("JP", "AS"); // name=Japan / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("JE", "EU"); // name=Jersey / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("JO", "AS"); // name=Jordan / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("KZ", "AS"); // name=Kazakhstan / region=Asia / subregion=Central Asia
        REGION_CODE_BY_COUNTRY_CODE.put("KE", "AF"); // name=Kenya / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("KI", "OC"); // name=Kiribati / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("KW", "AS"); // name=Kuwait / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("KG", "AS"); // name=Kyrgyzstan / region=Asia / subregion=Central Asia
        REGION_CODE_BY_COUNTRY_CODE.put("LA", "AS"); // name=Lao People's Democratic Republic / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("LV", "EU"); // name=Latvia / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("LB", "AS"); // name=Lebanon / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("LS", "AF"); // name=Lesotho / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("LR", "AF"); // name=Liberia / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("LY", "AF"); // name=Libya / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("LI", "EU"); // name=Liechtenstein / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("LT", "EU"); // name=Lithuania / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("LU", "EU"); // name=Luxembourg / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MO", "AS"); // name=Macao / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("MK", "EU"); // name=Macedonia (the former Yugoslav Republic of) / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MG", "AF"); // name=Madagascar / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MW", "AF"); // name=Malawi / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MY", "AS"); // name=Malaysia / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("MV", "AS"); // name=Maldives / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("ML", "AF"); // name=Mali / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MT", "EU"); // name=Malta / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MH", "OC"); // name=Marshall Islands / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("MQ", "AM"); // name=Martinique / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("MR", "AF"); // name=Mauritania / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MU", "AF"); // name=Mauritius / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("YT", "AF"); // name=Mayotte / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MX", "AM"); // name=Mexico / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("FM", "OC"); // name=Micronesia (Federated States of) / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("MD", "EU"); // name=Moldova (Republic of) / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MC", "EU"); // name=Monaco / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MN", "AS"); // name=Mongolia / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("ME", "EU"); // name=Montenegro / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("MS", "AM"); // name=Montserrat / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("MA", "AF"); // name=Morocco / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MZ", "AF"); // name=Mozambique / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("MM", "AS"); // name=Myanmar / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("NA", "AF"); // name=Namibia / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("NR", "OC"); // name=Nauru / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("NP", "AS"); // name=Nepal / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("NL", "EU"); // name=Netherlands / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("NC", "OC"); // name=New Caledonia / region=Oceania / subregion=Melanesia
        REGION_CODE_BY_COUNTRY_CODE.put("NZ", "OC"); // name=New Zealand / region=Oceania / subregion=Australia and New Zealand
        REGION_CODE_BY_COUNTRY_CODE.put("NI", "AM"); // name=Nicaragua / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("NE", "AF"); // name=Niger / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("NG", "AF"); // name=Nigeria / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("NU", "OC"); // name=Niue / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("NF", "OC"); // name=Norfolk Island / region=Oceania / subregion=Australia and New Zealand
        REGION_CODE_BY_COUNTRY_CODE.put("KP", "AS"); // name=Korea (Democratic People's Republic of) / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("MP", "OC"); // name=Northern Mariana Islands / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("NO", "EU"); // name=Norway / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("OM", "AS"); // name=Oman / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("PK", "AS"); // name=Pakistan / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("PW", "OC"); // name=Palau / region=Oceania / subregion=Micronesia
        REGION_CODE_BY_COUNTRY_CODE.put("PS", "AS"); // name=Palestine, State of / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("PA", "AM"); // name=Panama / region=Americas / subregion=Central America
        REGION_CODE_BY_COUNTRY_CODE.put("PG", "OC"); // name=Papua New Guinea / region=Oceania / subregion=Melanesia
        REGION_CODE_BY_COUNTRY_CODE.put("PY", "AM"); // name=Paraguay / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("PE", "AM"); // name=Peru / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("PH", "AS"); // name=Philippines / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("PN", "OC"); // name=Pitcairn / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("PL", "EU"); // name=Poland / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("PT", "EU"); // name=Portugal / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("PR", "AM"); // name=Puerto Rico / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("QA", "AS"); // name=Qatar / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("XK", "EU"); // name=Republic of Kosovo / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("RE", "AF"); // name=Réunion / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("RO", "EU"); // name=Romania / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("RU", "EU"); // name=Russian Federation / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("RW", "AF"); // name=Rwanda / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("BL", "AM"); // name=Saint Barthélemy / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("SH", "AF"); // name=Saint Helena, Ascension and Tristan da Cunha / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("KN", "AM"); // name=Saint Kitts and Nevis / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("LC", "AM"); // name=Saint Lucia / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("MF", "AM"); // name=Saint Martin (French part) / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("PM", "AM"); // name=Saint Pierre and Miquelon / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("VC", "AM"); // name=Saint Vincent and the Grenadines / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("WS", "OC"); // name=Samoa / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("SM", "EU"); // name=San Marino / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("ST", "AF"); // name=Sao Tome and Principe / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SA", "AS"); // name=Saudi Arabia / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("SN", "AF"); // name=Senegal / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("RS", "EU"); // name=Serbia / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("SC", "AF"); // name=Seychelles / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SL", "AF"); // name=Sierra Leone / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SG", "AS"); // name=Singapore / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("SX", "AM"); // name=Sint Maarten (Dutch part) / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("SK", "EU"); // name=Slovakia / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("SI", "EU"); // name=Slovenia / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("SB", "OC"); // name=Solomon Islands / region=Oceania / subregion=Melanesia
        REGION_CODE_BY_COUNTRY_CODE.put("SO", "AF"); // name=Somalia / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("ZA", "AF"); // name=South Africa / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("GS", "AM"); // name=South Georgia and the South Sandwich Islands / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("KR", "AS"); // name=Korea (Republic of) / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("SS", "AF"); // name=South Sudan / region=Africa / subregion=Middle Africa
        REGION_CODE_BY_COUNTRY_CODE.put("ES", "EU"); // name=Spain / region=Europe / subregion=Southern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("LK", "AS"); // name=Sri Lanka / region=Asia / subregion=Southern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("SD", "AF"); // name=Sudan / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SR", "AM"); // name=Suriname / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("SJ", "EU"); // name=Svalbard and Jan Mayen / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("SZ", "AF"); // name=Swaziland / region=Africa / subregion=Southern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("SE", "EU"); // name=Sweden / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("CH", "EU"); // name=Switzerland / region=Europe / subregion=Western Europe
        REGION_CODE_BY_COUNTRY_CODE.put("SY", "AS"); // name=Syrian Arab Republic / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TW", "AS"); // name=Taiwan / region=Asia / subregion=Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TJ", "AS"); // name=Tajikistan / region=Asia / subregion=Central Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TZ", "AF"); // name=Tanzania, United Republic of / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("TH", "AS"); // name=Thailand / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TL", "AS"); // name=Timor-Leste / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TG", "AF"); // name=Togo / region=Africa / subregion=Western Africa
        REGION_CODE_BY_COUNTRY_CODE.put("TK", "OC"); // name=Tokelau / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("TO", "OC"); // name=Tonga / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("TT", "AM"); // name=Trinidad and Tobago / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("TN", "AF"); // name=Tunisia / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("TR", "AS"); // name=Turkey / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TM", "AS"); // name=Turkmenistan / region=Asia / subregion=Central Asia
        REGION_CODE_BY_COUNTRY_CODE.put("TC", "AM"); // name=Turks and Caicos Islands / region=Americas / subregion=Caribbean
        REGION_CODE_BY_COUNTRY_CODE.put("TV", "OC"); // name=Tuvalu / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("UG", "AF"); // name=Uganda / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("UA", "EU"); // name=Ukraine / region=Europe / subregion=Eastern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("AE", "AS"); // name=United Arab Emirates / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("GB", "EU"); // name=United Kingdom of Great Britain and Northern Ireland / region=Europe / subregion=Northern Europe
        REGION_CODE_BY_COUNTRY_CODE.put("US", "AM"); // name=United States of America / region=Americas / subregion=Northern America
        REGION_CODE_BY_COUNTRY_CODE.put("UY", "AM"); // name=Uruguay / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("UZ", "AS"); // name=Uzbekistan / region=Asia / subregion=Central Asia
        REGION_CODE_BY_COUNTRY_CODE.put("VU", "OC"); // name=Vanuatu / region=Oceania / subregion=Melanesia
        REGION_CODE_BY_COUNTRY_CODE.put("VE", "AM"); // name=Venezuela (Bolivarian Republic of) / region=Americas / subregion=South America
        REGION_CODE_BY_COUNTRY_CODE.put("VN", "AS"); // name=Vietnam / region=Asia / subregion=South-Eastern Asia
        REGION_CODE_BY_COUNTRY_CODE.put("WF", "OC"); // name=Wallis and Futuna / region=Oceania / subregion=Polynesia
        REGION_CODE_BY_COUNTRY_CODE.put("EH", "AF"); // name=Western Sahara / region=Africa / subregion=Northern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("YE", "AS"); // name=Yemen / region=Asia / subregion=Western Asia
        REGION_CODE_BY_COUNTRY_CODE.put("ZM", "AF"); // name=Zambia / region=Africa / subregion=Eastern Africa
        REGION_CODE_BY_COUNTRY_CODE.put("ZW", "AF"); // name=Zimbabwe / region=Africa / subregion=Eastern Africa
    }

    public static String getRegionName(final String regionCode) {
        return NAME_BY_CODE.get(regionCode);
    }

    public static String getRegionCode(String countryCode) {
        return REGION_CODE_BY_COUNTRY_CODE.getOrDefault(countryCode, "Undefined");
    }
}
