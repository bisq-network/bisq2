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

package bisq.account.payment_method;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.FiatCurrency;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FiatPaymentRailUtil {
    public static List<FiatPaymentRail> getPaymentRails() {
        return List.of(FiatPaymentRail.values());
    }

    public static List<FiatPaymentRail> getPaymentRails(TradeProtocolType protocolType) {
        return switch (protocolType) {
            case BISQ_EASY, MU_SIG, BISQ_LIGHTNING -> getPaymentRails();
            case MONERO_SWAP, LIQUID_SWAP, BSQ_SWAP ->
                    throw new UnsupportedOperationException("No paymentMethods for that protocolType");
            default -> throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        };
    }

    public static List<FiatPaymentRail> getPaymentRails(String currencyCode) {
        return getPaymentRails().stream()
                .filter(fiatPaymentRail -> {
                    if (currencyCode.equals("EUR") &&
                            (fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK)) {
                        // For EUR, we don't add NATIONAL_BANK as SEPA is the common payment rail for EUR
                        // SWIFT is added to support non-EUR countries offering EUR accounts like Switzerland
                        return false;
                    }
                    // We add NATIONAL_BANK to all others
                    if (fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK) {
                        return true;
                    }
                    return fiatPaymentRail.supportsCurrency(currencyCode);
                })
                .collect(Collectors.toList());
    }

    /* --------------------------------------------------------------------- */
    // SEPA
    /* --------------------------------------------------------------------- */

    public static List<String> getAllSepaCountryCodes() {
        List<String> sepaEuroCountries = getSepaEuroCountries();
        List<String> sepaNonEuroCountries = getSepaNonEuroCountries();
        List<String> sepaCountries = new ArrayList<>(sepaEuroCountries);
        sepaCountries.addAll(sepaNonEuroCountries);
        Collections.sort(sepaCountries);
        return sepaCountries;
    }

    public static List<String> getSepaEuroCountries() {
        return List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA");
    }

    public static List<String> getSepaNonEuroCountries() {
        return List.of("BG", "HR", "CZ", "DK", "GB", "HU", "PL", "RO",
                "SE", "IS", "NO", "LI", "CH", "JE", "GI");
    }

    public static List<Country> getAllSepaCountries() {
        return CountryRepository.getCountriesFromCodes(getAllSepaCountryCodes());
    }


    /* --------------------------------------------------------------------- */
    //     NATIONAL_BANK(allCountries(),
    /* --------------------------------------------------------------------- */

    public static List<String> getNationalBankAccountCountries() {
        List<String> list = new ArrayList<>(CountryRepository.getAllCountyCodes());
        list.removeAll(getSepaEuroCountries());
        Collections.sort(list);
        return list;
    }


    /* --------------------------------------------------------------------- */
    // REVOLUT
    /* --------------------------------------------------------------------- */

    // https://help.revolut.com/help/wealth/exchanging-money/what-currencies-are-available/what-currencies-are-supported-for-holding-and-exchange/
    public static List<String> getRevolutCountryCodes() {
        return List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US");
    }

    public static List<Country> getRevolutCountries() {
        return CountryRepository.getCountriesFromCodes(getRevolutCountryCodes());
    }

    public static List<String> getRevolutCurrencyCodes() {
        return List.of(
                "AED",
                "AUD",
                "BGN",
                "CAD",
                "CHF",
                "CZK",
                "DKK",
                "EUR",
                "GBP",
                "HKD",
                "HUF",
                "ILS",
                "ISK",
                "JPY",
                "MAD",
                "MXN",
                "NOK",
                "NZD",
                "PLN",
                "QAR",
                "RON",
                "RSD",
                "RUB",
                "SAR",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "USD",
                "ZAR"
        );
    }

    public static List<FiatCurrency> getRevolutCurrencies() {
        return currenciesFromCodes(getRevolutCurrencyCodes());
    }


    /* --------------------------------------------------------------------- */
    // WISE
    /* --------------------------------------------------------------------- */

    // https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from?origin=related-article-2571942
    // https://github.com/bisq-network/proposals/issues/243
    public static List<String> getWiseCountryCodes() {
        List<String> list = new ArrayList<>(List.of("AR", "AU", "BD", "BR", "BG", "CA", "CL", "CN", "CO", "CR", "CZ", "DK", "EG",
                "GE", "GH", "HK", "HU", "IN", "ID", "IL", "JP", "KE", "MY", "MX", "MA", "NP", "NZ", "NO",
                "PK", "PH", "PL", "RO", "SG", "ZA", "KR", "LK", "SE", "CH", "TZ", "TH", "TR", "UG", "UA", "AE",
                "GB", "US", "UY", "VN", "ZM"));
        list.addAll(getSepaEuroCountries());
        return list;
    }

    public static List<Country> getWiseCountries() {
        return CountryRepository.getCountriesFromCodes(getWiseCountryCodes());
    }

    // Took all currencies from: https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from
    public static List<String> getWiseCurrencyCodes() {
        return List.of(
                "AED",
                "ARS",
                "AUD",
                "BDT",
                "BGN",
                "BRL",
                "BWP",
                "CAD",
                "CHF",
                "CLP",
                "CNY",
                "COP",
                "CRC",
                "CZK",
                "DKK",
                "EGP",
                "EUR",
                "FJD",
                "GEL",
                "GHS",
                "GBP",
                "HKD",
                "HUF",
                "IDR",
                "ILS",
                "INR",
                "JPY",
                "KES",
                "KRW",
                "LKR",
                "MAD",
                "MXN",
                "MYR",
                "NOK",
                "NPR",
                "NZD",
                "PHP",
                "PKR",
                "PLN",
                "RON",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "UAH",
                "UGX",
                "USD",
                "UYU",
                "VND",
                "ZAR",
                "ZMW"
        );
    }

    public static List<FiatCurrency> getWiseCurrencies() {
        return currenciesFromCodes(getWiseCurrencyCodes());
    }


    /* --------------------------------------------------------------------- */
    // UPHOLD
    /* --------------------------------------------------------------------- */

    // Generated by ChatGpt, not sure if correct
    public static List<String> getUpholdCountryCodes() {
        List<String> list = new ArrayList<>(List.of(
                "AD","AE","AG","AI","AL","AO","AQ","AR","AS","AT","AU","AW","BB","BD","BE",
                "BF","BG","BH","BI","BJ","BN","BO","BR","BS","BT","BV","BW","BY","BZ","CK",
                "CL","CR","CW","CX","CY","CZ","DJ","DK","DM","DO","DZ","EE","ES","FI","FJ",
                "FK","FM","FO","FR","GA","GB","GD","GG","GH","GI","GL","GM","GP","GQ","GR",
                "GT","GU","GW","GY","HK","HM","HN","HR","HT","HU","IE","IL","IM","IS","IT",
                "JE","JM","JO","JP","KE","KI","KM","KN","KP","KR","KW","KY","KZ","LA","LC",
                "LI","LK","LS","LT","LU","LV","MA","MC","MG","MH","MK","ML","MM","MO","MP",
                "MQ","MR","MS","MT","MU","MV","MW","MX","MY","MZ","NA","NC","NE","NF","NI",
                "NL","NO","NP","NR","NU","NZ","OM","PE","PF","PG","PH","PL","PM","PN","PR",
                "PT","PW","PY","QA","RE","RO","RW","SA","SB","SC","SD","SE","SG","SH","SI",
                "SJ","SK","SM","SN","SR","ST","SV","SX","SY","SZ","TC","TF","TH","TJ","TK",
                "TL","TM","TN","TO","TR","TT","TV","TW","TZ","US","UY","UZ","VA","VC","VG",
                "VI","VU","WF","WS","YE","YT","ZA","ZM"
        ));
        list.addAll(getSepaEuroCountries());
        return list;
    }

    public static List<Country> getUpholdCountries() {
        return CountryRepository.getCountriesFromCodes(getUpholdCountryCodes());
    }

    //  https://support.uphold.com/hc/en-us/articles/202473803-Supported-currencies
    public static List<String> getUpholdCurrencyCodes() {
        return List.of(
                "AED",
                "ARS",
                "AUD",
                "BRL",
                "CAD",
                "CHF",
                "CNY",
                "DKK",
                "EUR",
                "GBP",
                "HKD",
                "ILS",
                "INR",
                "JPY",
                "KES",
                "MXN",
                "NOK",
                "NZD",
                "PHP",
                "PLN",
                "SEK",
                "SGD",
                "USD"
        );
    }

    public static List<FiatCurrency> getUpholdCurrencies() {
        return currenciesFromCodes(getUpholdCurrencyCodes());
    }


    /* --------------------------------------------------------------------- */
    // AMAZON_GIFT_CARD
    /* --------------------------------------------------------------------- */

    public static List<String> getAmazonGiftCardCountryCodes() {
        return new ArrayList<>(List.of("AU", "CA", "FR", "DE", "IT", "NL", "ES", "GB", "IN", "JP",
                "SA", "SE", "SG", "TR", "US"));
    }

    public static List<Country> getAmazonGiftCardCountries() {
        return CountryRepository.getCountriesFromCodes(getAmazonGiftCardCountryCodes());
    }

    public static List<String> getAmazonGiftCardCurrencyCodes() {
        return List.of("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "SAR", "SEK", "SGD", "TRY", "USD");
    }

    public static List<FiatCurrency> getAmazonGiftCardCurrencies() {
        return currenciesFromCodes(getAmazonGiftCardCurrencyCodes());
    }


    /* --------------------------------------------------------------------- */
    // MONEY_BEAM
    /* --------------------------------------------------------------------- */

    public static List<String> getMoneyBeamCurrencyCodes() {
        return List.of("EUR", "GBP");
    }

    public static List<FiatCurrency> getMoneyBeamCurrencies() {
        return currenciesFromCodes(getMoneyBeamCurrencyCodes());
    }


    /* --------------------------------------------------------------------- */
    // MONEY_GRAM
    /* --------------------------------------------------------------------- */

    // Generated with deepseek, not sure if correct
    public static List<String> getMoneyGramCountryCodes() {
        return new ArrayList<>(List.of(
                "AE", "AR", "AU", "KI", "NR", "TV", "BN", "CA", "CH", "LI",
                "CZ", "DK", "FO", "GL", "AD", "AT", "BE", "CY", "EE", "FI",
                "FR", "DE", "GR", "IE", "IT", "LV", "LT", "LU", "MT", "MC",
                "ME", "NL", "PT", "SM", "SK", "SI", "ES", "VA", "FJ", "GB",
                "GG", "IM", "JE", "HK", "HU", "ID", "IL", "IN", "JP", "KR",
                "KW", "LK", "MA", "MG", "MX", "MY", "NO", "NZ", "OM", "PE",
                "PG", "PH", "PK", "PL", "SA", "SB", "SC", "SE", "SG", "TH",
                "TO", "TR", "TW", "US", "VN", "VU", "WS", "BJ", "BF", "ML",
                "NE", "SN", "TG", "PF", "NC", "WF", "ZA"
        ));
    }

    public static List<Country> getMoneyGramCountries() {
        return CountryRepository.getCountriesFromCodes(getMoneyGramCountryCodes());
    }

    public static List<String> getMoneyGramCurrencyCodes() {
        return List.of(
                "AED",
                "ARS",
                "AUD",
                "BND",
                "CAD",
                "CHF",
                "CZK",
                "DKK",
                "EUR",
                "FJD",
                "GBP",
                "HKD",
                "HUF",
                "IDR",
                "ILS",
                "INR",
                "JPY",
                "KRW",
                "KWD",
                "LKR",
                "MAD",
                "MGA",
                "MXN",
                "MYR",
                "NOK",
                "NZD",
                "OMR",
                "PEN",
                "PGK",
                "PHP",
                "PKR",
                "PLN",
                "SAR",
                "SBD",
                "SCR",
                "SEK",
                "SGD",
                "THB",
                "TOP",
                "TRY",
                "TWD",
                "USD",
                "VND",
                "VUV",
                "WST",
                "XOF",
                "XPF",
                "ZAR"
        );
    }

    public static List<FiatCurrency> getMoneyGramCurrencies() {
        return currenciesFromCodes(getMoneyBeamCurrencyCodes());
    }



    /* --------------------------------------------------------------------- */
    // MONEY_GRAM
    /* --------------------------------------------------------------------- */

    //
    public static List<String> getHalCashCountryCodes() {
        return new ArrayList<>(List.of("ES", "PL"));
    }

    public static List<Country> getHalCashCountries() {
        return CountryRepository.getCountriesFromCodes(getHalCashCountryCodes());
    }



    /* --------------------------------------------------------------------- */
    // Popularity
    /* --------------------------------------------------------------------- */

    // Popularity scores based on historical snapshot of Bisq1 offers count
    // Higher scores indicate more commonly used payment methods
    public static Map<FiatPaymentRail, Integer> getPopularityScore() {
        return Map.ofEntries(
                Map.entry(FiatPaymentRail.SEPA, 10),
                Map.entry(FiatPaymentRail.SEPA_INSTANT, 10), //?
                Map.entry(FiatPaymentRail.ZELLE, 9),
                Map.entry(FiatPaymentRail.PIX, 8),
                Map.entry(FiatPaymentRail.NATIONAL_BANK, 7),
                Map.entry(FiatPaymentRail.REVOLUT, 6),
                Map.entry(FiatPaymentRail.CASH_BY_MAIL, 6),
                Map.entry(FiatPaymentRail.ACH_TRANSFER, 5),
                Map.entry(FiatPaymentRail.STRIKE, 5),
                Map.entry(FiatPaymentRail.INTERAC_E_TRANSFER, 4),
                Map.entry(FiatPaymentRail.WISE, 4),
                Map.entry(FiatPaymentRail.WISE_USD, 4), //?
                Map.entry(FiatPaymentRail.UPHOLD, 3), //?
                Map.entry(FiatPaymentRail.F2F, 3),
                Map.entry(FiatPaymentRail.US_POSTAL_MONEY_ORDER, 3),
                Map.entry(FiatPaymentRail.DOMESTIC_WIRE_TRANSFER, 3), //?
                Map.entry(FiatPaymentRail.PAY_ID, 3),
                Map.entry(FiatPaymentRail.FASTER_PAYMENTS, 3),
                Map.entry(FiatPaymentRail.AMAZON_GIFT_CARD, 2),
                Map.entry(FiatPaymentRail.MONEY_BEAM, 2), //?
                Map.entry(FiatPaymentRail.SWISH, 2), //?
                Map.entry(FiatPaymentRail.SWIFT, 2),
                Map.entry(FiatPaymentRail.BIZUM, 2),
                Map.entry(FiatPaymentRail.PROMPT_PAY, 2),//?
                Map.entry(FiatPaymentRail.MONEY_GRAM, 2), //?
                Map.entry(FiatPaymentRail.CASH_DEPOSIT, 2),
                Map.entry(FiatPaymentRail.SAME_BANK, 2), //?
                Map.entry(FiatPaymentRail.HAL_CASH, 2), //?
                Map.entry(FiatPaymentRail.PIN_4, 1), // not in bisq 1, polish version of halcash
                Map.entry(FiatPaymentRail.UPI, 1)
        );
    }

    private static List<FiatCurrency> currenciesFromCodes(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCode)
                .collect(Collectors.toList());
    }

    private static List<TradeCurrency> toTradeCurrencies(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCode)
                .distinct()
                .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                .collect(Collectors.toList());
    }
}