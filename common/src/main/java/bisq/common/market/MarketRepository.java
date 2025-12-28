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

package bisq.common.market;

import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.asset.FiatCurrencyRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MarketRepository {
    public static Market getDefaultBtcFiatMarket() {
        return new Market(CryptoAssetRepository.BITCOIN.getCode(),
                FiatCurrencyRepository.getDefaultCurrency().getCode(),
                CryptoAssetRepository.BITCOIN.getName(),
                FiatCurrencyRepository.getDefaultCurrency().getName()
        );
    }

    public static Market getDefaultXmrFiatMarket() {
        return new Market(CryptoAssetRepository.XMR.getCode(),
                FiatCurrencyRepository.getDefaultCurrency().getCode(),
                CryptoAssetRepository.XMR.getName(),
                FiatCurrencyRepository.getDefaultCurrency().getName()
        );
    }

    public static Market getDefaultCryptoBtcMarket() {
        return new Market(CryptoAssetRepository.getDefaultCurrency().getCode(),
                CryptoAssetRepository.BITCOIN.getCode(),
                CryptoAssetRepository.getDefaultCurrency().getName(),
                CryptoAssetRepository.BITCOIN.getName()
        );
    }

    public static Market getUSDBitcoinMarket() {
        return getAllFiatMarkets().stream()
                .filter(e -> e.getQuoteCurrencyCode().equals("USD"))
                .findAny()
                .orElseThrow();
    }

    public static Market getXmrBtcMarket() {
        return new Market(CryptoAssetRepository.XMR.getCode(),
                CryptoAssetRepository.BITCOIN.getCode(),
                CryptoAssetRepository.XMR.getName(),
                CryptoAssetRepository.BITCOIN.getName()
        );
    }

    public static Market getBsqMarket() {
        return new Market("BSQ", "BTC", "BSQ", "Bitcoin");
    }

    public static List<Market> getXmrCryptoMarkets() {
        return List.of(new Market("XMR", "BTC", "Monero", "Bitcoin"));
    }

    public static List<Market> getMajorMarkets() {
        return Stream.concat(getMajorFiatMarkets().stream(), getMajorCryptoAssetMarkets().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMinorMarkets() {
        return getMinorFiatMarkets();
    }

    public static List<Market> getMinorFiatMarkets() {
        return getMinorFiatMarkets("BTC", "Bitcoin");
    }

    public static List<Market> getMinorFiatMarkets(String baseCurrencyCode, String baseCurrencyName) {
        return FiatCurrencyRepository.getMinorCurrencies().stream()
                .map(currency -> new Market(baseCurrencyCode, currency.getCode(), baseCurrencyName, currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getMajorFiatMarkets() {
        return getMajorFiatMarkets("BTC", "Bitcoin");
    }

    public static List<Market> getMajorFiatMarkets(String baseCurrencyCode, String baseCurrencyName) {
        return FiatCurrencyRepository.getMajorCurrencies().stream()
                .map(currency -> new Market(baseCurrencyCode, currency.getCode(), baseCurrencyName, currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllUnsortedFiatMarkets() {
        return FiatCurrencyRepository.getAllCurrencies().stream()
                .map(currency -> new Market("BTC", currency.getCode(), "Bitcoin", currency.getName()))
                .distinct()
                .collect(Collectors.toList());
    }


    public static List<Market> getMajorCryptoAssetMarkets() {
        return CryptoAssetRepository.getCryptoAssets().stream()
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllXmrMarkets() {
        List<Market> list = new ArrayList<>();
        list.add(getDefaultXmrFiatMarket());
        list.addAll(getMajorFiatMarkets("XMR", "Monero"));
        list.addAll(getMinorFiatMarkets("XMR", "Monero"));
        list.addAll(getXmrCryptoMarkets());
        return list.stream().distinct().collect(Collectors.toList());
    }

    public static List<Market> getAllCryptoAssetMarkets() {
        return CryptoAssetRepository.getCryptoAssets().stream()
                .filter(currency -> !currency.equals(CryptoAssetRepository.BITCOIN))
                .map(currency -> new Market(currency.getCode(), "BTC", currency.getName(), "Bitcoin"))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<Market> getAllNonXMRCryptoCurrencyMarkets() {
        List<CryptoAsset> allCurrencies = CryptoAssetRepository.getCryptoAssets();
        return allCurrencies.stream()
                .filter(currency -> !currency.equals(CryptoAssetRepository.BITCOIN))
                .filter(currency -> !currency.equals(CryptoAssetRepository.XMR))
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