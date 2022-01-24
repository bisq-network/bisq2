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

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class CryptoCurrencyRepository {
    @Getter
    private static Map<String, CryptoCurrency> currencyByCode = new HashMap<>();
    @Getter
    private static List<CryptoCurrency> majorCurrencies;
    @Getter
    private static List<CryptoCurrency> minorCurrencies;
    @Getter
    private static List<CryptoCurrency> allCurrencies;
    @Getter
    private static CryptoCurrency defaultCurrency;

    static {
        CryptoCurrency btc = new CryptoCurrency("BTC", "Bitcoin");
        currencyByCode.put("BTC", btc);
        currencyByCode.put("USDT", new CryptoCurrency("USDT", "USD-Tether"));
        currencyByCode.put("XMR", new CryptoCurrency("XMR", "Monero"));
        currencyByCode.put("BTC-L", new CryptoCurrency("BTC-L", "Liquid-Bitcoin"));

        defaultCurrency = btc;
        majorCurrencies = initMajorCurrencies();
        minorCurrencies = new ArrayList<>(currencyByCode.values());
        minorCurrencies.remove(defaultCurrency);
        minorCurrencies.removeAll(majorCurrencies);
        minorCurrencies.sort(Comparator.comparing(TradeCurrency::getNameAndCode));
        allCurrencies = new ArrayList<>();
        allCurrencies.add(defaultCurrency);
        allCurrencies.addAll(majorCurrencies);
        allCurrencies.addAll(minorCurrencies);
    }

    private static List<CryptoCurrency> initMajorCurrencies() {
        List<String> mainCodes = new ArrayList<>(List.of("BTC", "XMR"));
        return mainCodes.stream()
                .map(code -> currencyByCode.get(code))
                .distinct()
                .collect(Collectors.toList());
    }
}
