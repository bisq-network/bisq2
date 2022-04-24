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
    private static Map<String, String> nameByCode = new HashMap<>();
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
        currencyByCode.put("XMR", new CryptoCurrency("XMR", "Monero"));
        currencyByCode.put("L-BTC", new CryptoCurrency("L-BTC", "Liquid-Bitcoin"));
        currencyByCode.put("USDT", new CryptoCurrency("USDT", "USD-Tether"));
        currencyByCode.put("GRIN", new CryptoCurrency("GRIN", "Grin"));
        currencyByCode.put("ZEC", new CryptoCurrency("ZEC", "Zcash"));
        currencyByCode.put("ETH", new CryptoCurrency("ETH", "Ethereum"));

        defaultCurrency = btc;

        majorCurrencies = initMajorCurrencies();
        majorCurrencies.remove(defaultCurrency);

        minorCurrencies = new ArrayList<>(currencyByCode.values());
        minorCurrencies.remove(defaultCurrency);
        minorCurrencies.removeAll(majorCurrencies);
        minorCurrencies.sort(Comparator.comparing(TradeCurrency::getNameAndCode));

        allCurrencies = new ArrayList<>();
        allCurrencies.add(defaultCurrency);
        allCurrencies.addAll(majorCurrencies);
        allCurrencies.addAll(minorCurrencies);

        fillNameByCodeMap();

        nameByCode.forEach((code, name) -> {
            CryptoCurrency cryptoCurrency = new CryptoCurrency(code, name);
            currencyByCode.put(code, cryptoCurrency);
            if (!defaultCurrency.equals(cryptoCurrency) &&
                    !majorCurrencies.contains(cryptoCurrency) &&
                    !minorCurrencies.contains(cryptoCurrency)) {
                minorCurrencies.add(cryptoCurrency);
            }
        });
    }

    private static List<CryptoCurrency> initMajorCurrencies() {
        List<String> mainCodes = new ArrayList<>(List.of("BTC", "XMR", "L-BTC", "USDT", "GRIN", "ZEC", "ETH"));
        return mainCodes.stream()
                .map(code -> currencyByCode.get(code))
                .distinct()
                .collect(Collectors.toList());
    }

    public static Optional<String> getName(String code) {
        return Optional.ofNullable(currencyByCode.get(code)).map(TradeCurrency::getName);
    }


    //todo fill with major coins
    private static void fillNameByCodeMap() {
        nameByCode.put("ALGO", "Algorand");
    }

}
