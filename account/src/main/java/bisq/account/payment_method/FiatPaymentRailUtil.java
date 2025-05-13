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
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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


    static List<String> getSepaEuroCountries() {
        return List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA");
    }

    static List<TradeCurrency> toTradeCurrencies(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCode)
                .distinct()
                .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                .collect(Collectors.toList());
    }

    // https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from?origin=related-article-2571942
    // https://github.com/bisq-network/proposals/issues/243
    static List<String> getWiseCountries() {
        List<String> list = new ArrayList<>(List.of("AR", "AU", "BD", "BR", "BG", "CA", "CL", "CN", "CO", "CR", "CZ", "DK", "EG",
                "GE", "GH", "HK", "HU", "IN", "ID", "IL", "JP", "KE", "MY", "MX", "MA", "NP", "NZ", "NO",
                "PK", "PH", "PL", "RO", "SG", "ZA", "KR", "LK", "SE", "CH", "TZ", "TH", "TR", "UG", "UA", "AE",
                "GB", "US", "UY", "VN", "ZM"));
        list.addAll(getSepaEuroCountries());
        return list;
    }

    // Took all currencies from: https://wise.com/help/articles/2571907/what-currencies-can-i-send-to-and-from
    static List<String> getWiseCurrencies() {
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

    // https://help.revolut.com/help/wealth/exchanging-money/what-currencies-are-available/what-currencies-are-supported-for-holding-and-exchange/
    static List<String> getRevolutCountries() {
        return List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US");
    }

    static List<String> getRevolutCurrencies() {
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

    //https://support.volet.com/hc/en-us/articles/14575021212572-In-what-countries-is-this-available
    static List<String> getVoletCountries() {
        return List.of("AT", "BE", "HR", "CY", "EE", "FI", "FR", "DE", "GR", "IE",
                "IT", "LV", "LT", "LU", "MT", "NL", "PT", "SK", "SI", "ES",
                "IN", "TR", "AE", "MX", "VN", "ID", "NG", "CL", "CO", "PE",
                "BR", "KE", "TH", "MY", "PH", "AR");
    }

    //https://volet.com/
    static List<String> getVoletCurrencies() {
        return List.of(
                "USD",
                "EUR",
                "GBP",
                "KZT",
                "BRL",
                "TRY",
                "VND",
                "AZN"
        );
    }

    static List<String> getMoneseCountries() {
        List<String> countries = new ArrayList<>(getSepaEuroCountries());
        countries.add("GB");
        countries.add("RO");
        return countries;
    }

    static List<String> getMoneseCurrencies() {
        return List.of(
                "EUR",
                "GBP",
                "RON"
        );
    }

    static List<String> getMoneyGramCountries() {
        List<String> countries = new ArrayList<>(List.of("AE", "AR", "AU", "BN", "CA", "CH", "CZ", "DK", "FJ", "GB", "HK", "HU",
                "ID", "IL", "IN", "JP", "KR", "KW", "LK", "MA", "MG", "MX", "MY", "NO", "NZ", "OM", "PE", "PG", "PH", "PK",
                "PL", "SA", "SB", "SC", "SE", "SG", "TH", "TO", "TR", "TW", "US", "VN", "VU", "WS", "ZA"));

        // Add XOF countries (West African CFA franc)
        countries.addAll(List.of("BJ", "BF", "CI", "GW", "ML", "NE", "SN", "TG"));

        // Add XPF countries (CFP franc)
        countries.addAll(List.of("PF", "NC", "WF"));

        // Add SEPA Euro countries
        countries.addAll(getSepaEuroCountries());

        return countries;
    }

    static List<String> getMoneyGramCurrencies() {
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

    static List<String> getPaxumCountries() {
        List<String> countries = new ArrayList<>(getSepaEuroCountries());

        // Add countries for non-EUR currencies
        countries.addAll(List.of("US", "CA", "DK", "CZ", "AU", "ZA", "TH", "CH", "SE", "RO", "PL", "NZ", "NO",
                "IN", "ID", "HU", "GB"));

        return countries;
    }

    static List<String> getPaxumCurrencies() {
        return List.of(
                "USD",
                "CAD",
                "EUR",
                "DKK",
                "CZK",
                "AUD",
                "ZAR",
                "THB",
                "CHF",
                "SEK",
                "RON",
                "PLN",
                "NZD",
                "NOK",
                "INR",
                "IDR",
                "HUF",
                "GBP"
        );
    }

    static List<String> getPayseraCountries() {
        List<String> countries = new ArrayList<>(getSepaEuroCountries());

        // Add countries for non-EUR currencies
        countries.addAll(List.of("AU", "BG", "BY", "CA", "CH", "CN", "CZ", "DK", "GB", "GE", "HK", "HR", "HU",
                "IL", "IN", "JP", "KZ", "MX", "NO", "NZ", "PH", "PL", "RO", "RS", "RU", "SE", "SG", "TH", "TR", "US",
                "ZA"));

        return countries;
    }

    static List<String> getPayseraCurrencies() {
        return List.of(
                "AUD",
                "BGN",
                "BYN",
                "CAD",
                "CHF",
                "CNY",
                "CZK",
                "DKK",
                "EUR",
                "GBP",
                "GEL",
                "HKD",
                "HUF",
                "ILS",
                "INR",
                "JPY",
                "KZT",
                "MXN",
                "NOK",
                "NZD",
                "PHP",
                "PLN",
                "RON",
                "RSD",
                "RUB",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "USD",
                "ZAR"
        );
    }

    //https://support.satispay.com/en/articles/information-required-during-signup
    static List<String> getSatispayCountries() {
        return List.of(
                "LU", "AT", "BE", "DK", "EE", "FI", "FR", "DE", "GR", "IE",
                "IT", "LT", "NL", "NO", "PT", "ES", "SE", "CZ", "HU", "LV",
                "PL", "SK", "BG", "CY", "RO", "SI", "MT", "HR", "IS", "LI"
        );
    }

    static List<String> getSatispayCurrencies() {
        return List.of(
                "EUR"
        );
    }

    static List<String> getUpholdCountries() {
        return List.of(
                "AD", "AR", "AT", "AU", "BE", "BG", "BM", "BR", "CA", "CH",
                "CL", "CR", "CY", "CZ", "DK", "EE", "ES", "FI", "FR", "GB",
                "GR", "GT", "HK", "HR", "HU", "IE", "IL", "IS", "IT", "JP",
                "KR", "LI", "LT", "LU", "LV", "MC", "MT", "MX", "MY", "NO",
                "NZ", "PE", "PH", "PL", "PT", "RO", "SE", "SG", "SI", "SK",
                "SM", "TH", "US", "UY", "VA", "ZA"
        );
    }

    static List<String> getUpholdCurrencies() {
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

    static List<String> getVerseCountries() {
        return List.of("AT", "BE", "DK", "FI", "FR", "DE", "GR", "IE", "IT", "PL", "PT", "SK", "ES", "SE",
                "NL", "GB");
    }

    static List<String> getVerseCurrencies() {
        return List.of(
                "EUR",
                "GBP",
                "SEK",
                "HUF",
                "DKK",
                "PLN"
        );
    }

    //https://support.bigcommerce.com/s/article/Connecting-with-Mercado-Pago?language=en_US
    static List<String> getMercadoPagoCountries() {
        return List.of("AR", "BR", "CL", "CO", "MX", "PE", "UY");
    }

    static List<String> getMercadoPagoCurrencies() {
        return List.of(
                "ARS",
                "BRL",
                "CLP",
                "COP",
                "MXN",
                "PEN",
                "UYU"
        );
    }
}