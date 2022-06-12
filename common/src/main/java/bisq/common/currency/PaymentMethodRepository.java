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

package bisq.common.currency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PaymentMethodRepository {
    public static final List<String> ALL_FIAT_PAYMENT_METHODS = new ArrayList<>();
    public static final Map<String, List<String>> FIAT_CURRENCIES_BY_PAYMENT_METHOD = new HashMap<>();

    static {
        FIAT_CURRENCIES_BY_PAYMENT_METHOD.put("SEPA", List.of("EUR"));
        FIAT_CURRENCIES_BY_PAYMENT_METHOD.put("Zelle", List.of("USD"));
        FIAT_CURRENCIES_BY_PAYMENT_METHOD.put("Revolut", getRevolutCurrencies());
        FIAT_CURRENCIES_BY_PAYMENT_METHOD.put("Wise", geWiseCurrencies());
        FIAT_CURRENCIES_BY_PAYMENT_METHOD.put("NationalBankTransfer", FiatCurrencyRepository.getAllFiatCurrencyCodes());
        ALL_FIAT_PAYMENT_METHODS.addAll(FIAT_CURRENCIES_BY_PAYMENT_METHOD.keySet());
    }

    public static List<String> getPaymentMethodsForMarket(Market market) {
        return FIAT_CURRENCIES_BY_PAYMENT_METHOD.entrySet().stream()
                .filter(e -> e.getValue().contains(market.quoteCurrencyCode()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    // https://github.com/bisq-network/proposals/issues/243
    public static List<String> geWiseCurrencies() {
        return List.of(
                "AED",
                "ARS",
                "AUD",
                "BGN",
                "CAD",
                "CHF",
                "CLP",
                "CZK",
                "DKK",
                "EGP",
                "EUR",
                "GBP",
                "GEL",
                "HKD",
                "HRK",
                "HUF",
                "IDR",
                "ILS",
                "JPY",
                "KES",
                "KRW",
                "MAD",
                "MXN",
                "MYR",
                "NOK",
                "NPR",
                "NZD",
                "PEN",
                "PHP",
                "PKR",
                "PLN",
                "RON",
                "RUB",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "UGX",
                "VND",
                "XOF",
                "ZAR",
                "ZMW"
        );
    }

    // https://www.revolut.com/help/getting-started/exchanging-currencies/what-fiat-currencies-are-supported-for-holding-and-exchange
    public static List<String> getRevolutCurrencies() {
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
                "HRK",
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
}