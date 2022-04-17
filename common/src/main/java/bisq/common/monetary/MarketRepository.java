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

package bisq.common.monetary;

import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.FiatCurrencyRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarketRepository {
    public static Market getDefault() {
        return new Market(CryptoCurrencyRepository.getDefaultCurrency().getCode(), FiatCurrencyRepository.getDefaultCurrency().getCode());
    }

    public static Market getBsqMarket() {
        return new Market("BSQ", "BTC");
    }

    public static Market getXmrMarket() {
        return new Market("XMR", "BTC");
    }

    public static List<Market> getMajorMarkets() {
        return Stream.concat(getMajorFiatMarkets().stream(), getMajorCryptoCurrencyMarkets().stream())
                .collect(Collectors.toList());
    }

    public static List<Market> getMajorFiatMarkets() {
        return FiatCurrencyRepository.getMajorCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode()))
                .collect(Collectors.toList());
    }

    public static List<Market> getAllFiatMarkets() {
        return FiatCurrencyRepository.getAllCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode()))
                .collect(Collectors.toList());
    }

    public static List<Market> getMajorCryptoCurrencyMarkets() {
        return CryptoCurrencyRepository.getMajorCurrencies().stream()
                .map(currency -> new Market(currency.getCode(), "BTC"))
                .collect(Collectors.toList());
    }

    public static List<Market> getAllCryptoCurrencyMarkets() {
        return CryptoCurrencyRepository.getAllCurrencies().stream()
                .map(currency -> new Market(currency.getCode(), "BTC"))
                .collect(Collectors.toList());
    }

    public static List<Market> getAllMarkets() {
        return Stream.concat(getAllFiatMarkets().stream(), getAllCryptoCurrencyMarkets().stream())
                .collect(Collectors.toList());
    }
}