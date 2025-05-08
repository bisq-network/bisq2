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

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MarketRepository {
    public static Market getDefaultBtcFiatMarket() {
        return new Market(CryptoCurrencyRepository.BITCOIN.getCode(),
                FiatCurrencyRepository.getDefaultCurrency().getCode(),
                CryptoCurrencyRepository.BITCOIN.getName(),
                FiatCurrencyRepository.getDefaultCurrency().getName()
        );
    }

    public static Market getDefaultCryptoBtcMarket() {
        return new Market(CryptoCurrencyRepository.L_BTC.getCode(),
                CryptoCurrencyRepository.BITCOIN.getCode(),
                CryptoCurrencyRepository.L_BTC.getName(),
                CryptoCurrencyRepository.BITCOIN.getName()
        );
    }

    public static Market getUSDBitcoinMarket() {
        return getAllFiatMarkets().stream()
                .filter(e -> e.getQuoteCurrencyCode().equals("USD"))
                .findAny()
                .orElseThrow();
    }

    public static Market getBsqMarket() {
        return new Market("BSQ", "BTC", "BSQ", "Bitcoin");
    }

    public static Market getXmrMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }

    public static List<Market> getMajorMarkets() {
        return Stream.concat(getMajorFiatMarkets().stream(), getMajorCryptoCurrencyMarkets().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMinorMarkets() {
        return Stream.concat(getMinorFiatMarkets().stream(), getMinorCryptoCurrencyMarkets().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMinorFiatMarkets() {
        return FiatCurrencyRepository.getMinorCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode(), "Bitcoin", currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMajorFiatMarkets() {
        return FiatCurrencyRepository.getMajorCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode(), "Bitcoin", currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllUnsortedFiatMarkets() {
        return FiatCurrencyRepository.getAllCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode(), "Bitcoin", currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMinorCryptoCurrencyMarkets() {
        return CryptoCurrencyRepository.getMinorCurrencies().stream()
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMajorCryptoCurrencyMarkets() {
        return CryptoCurrencyRepository.getMajorCurrencies().stream()
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllCryptoCurrencyMarkets() {
        return CryptoCurrencyRepository.getALL_CURRENCIES().stream()
                .filter(currency -> !currency.equals(CryptoCurrencyRepository.BITCOIN))
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllNonXMRCryptoCurrencyMarkets() {
        List<CryptoCurrency> allCurrencies = CryptoCurrencyRepository.getALL_CURRENCIES();
        return allCurrencies.stream()
                .filter(currency -> {
                    var ee = CryptoCurrencyRepository.getALL_CURRENCIES();
                    if (currency == null) {
                        log.error("");
                    }
                    return true;
                })
                .filter(currency -> !currency.equals(CryptoCurrencyRepository.BITCOIN))
                .filter(currency -> !currency.equals(CryptoCurrencyRepository.XMR))
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllMarkets() {
        List<Market> list = new ArrayList<>();
        list.add(getDefaultBtcFiatMarket());
        list.addAll(getMajorMarkets());
        list.addAll(getMinorMarkets());
        return list.stream().distinct().collect(Collectors.toList());
    }

    public static List<Market> getAllFiatMarkets() {
        List<Market> list = new ArrayList<>();
        list.add(getDefaultBtcFiatMarket());
        list.addAll(getMajorFiatMarkets());
        list.addAll(getMinorFiatMarkets());
        return list.stream().distinct().collect(Collectors.toList());
    }

    public static Optional<Market> findAnyMarketByMarketCodes(String marketCodes) {
        return MarketRepository.getAllMarkets().stream()
                .filter(e -> e.getMarketCodes().equals(marketCodes))
                .findAny();
    }

    public static Optional<Market> findAnyFiatMarketByMarketCodes(String marketCodes) {
        return MarketRepository.getAllFiatMarkets().stream()
                .filter(e -> e.getMarketCodes().equals(marketCodes))
                .findAny();
    }
}