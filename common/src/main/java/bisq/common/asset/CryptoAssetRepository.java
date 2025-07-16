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

package bisq.common.asset;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CryptoAssetRepository {
    public static final CryptoAsset BITCOIN = new CryptoAsset("BTC", "Bitcoin");
    public static final CryptoAsset XMR = new CryptoAsset("XMR", "Monero");
    @Setter // Maybe we allow user to set their preferred default?
    @Getter
    private static CryptoAsset defaultCurrency = XMR;

    @Getter
    private static final List<CryptoAsset> MAJOR_CURRENCIES = List.of(
            XMR,
            new CryptoAsset("ETH", "Ethereum"),
            new CryptoAsset("LTC", "Litecoin")
    );
    @Getter
    private static final List<CryptoAsset> MINOR_CURRENCIES = List.of(
            new CryptoAsset("GRIN", "Grin"),
            new CryptoAsset("ZEC", "Zcash")
    );

    @Getter
    private static final Map<String, CryptoAsset> MAJOR_CURRENCIES_BY_CODE = MAJOR_CURRENCIES.stream()
            .collect(Collectors.toMap(Asset::getCode, e -> e));
    @Getter
    private static final Map<String, CryptoAsset> MINOR_CURRENCIES_BY_CODE = MINOR_CURRENCIES.stream()
            .collect(Collectors.toMap(Asset::getCode, e -> e));
    @Getter
    private static final Map<String, CryptoAsset> ALL_CURRENCIES_BY_CODE = Stream.concat(
                    MAJOR_CURRENCIES_BY_CODE.entrySet().stream(),
                    MINOR_CURRENCIES_BY_CODE.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    @Getter
    private static final List<CryptoAsset> ALL_CURRENCIES = Stream.concat(
                    MAJOR_CURRENCIES.stream(),
                    MINOR_CURRENCIES.stream())
            .toList();

    public static Optional<String> findName(String code) {
        return find(code).map(CryptoAsset::getName);
    }

    public static Optional<CryptoAsset> find(String code) {
        return Optional.ofNullable(ALL_CURRENCIES_BY_CODE.get(code));
    }

    public static CryptoAsset findOrCreateCustom(String code) {
        return find(code).orElse(new CryptoAsset(code));
    }

    public static List<CryptoAsset> getMajorCurrencies() {
        return MAJOR_CURRENCIES;
    }

    public static List<CryptoAsset> getMinorCurrencies() {
        return MINOR_CURRENCIES;
    }

    public static List<CryptoAsset> getAllCurrencies() {
        return ALL_CURRENCIES;
    }
}
