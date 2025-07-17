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

public class CryptoAssetRepository {
    public static final CryptoAsset BITCOIN = new CryptoAsset("BTC", "Bitcoin");
    public static final CryptoAsset XMR = new CryptoAsset("XMR", "Monero");
    @Setter // Maybe we allow user to set their preferred default?
    @Getter
    private static CryptoAsset defaultCurrency = XMR;

    private static final List<CryptoAsset> CRYPTO_ASSETS = List.of(
            XMR,
            new CryptoAsset("BSQ", "BSQ"),
            new CryptoAsset("LTC", "Litecoin"),
            new CryptoAsset("ETH", "Ether"),
            new CryptoAsset("ETC", "Ether Classic"),
            new CryptoAsset("L-BTC", "Liquid Bitcoin"),
            new CryptoAsset("LN-BTC", "Lightning Network Bitcoin"),
            new CryptoAsset("GRIN", "Grin"),
            new CryptoAsset("ZEC", "Zcash"),
            new CryptoAsset("DOGE", "Dogecoin")
    );

    private static final Map<String, CryptoAsset> CRYPTO_ASSETS_BY_CODE = CRYPTO_ASSETS.stream()
            .collect(Collectors.toMap(Asset::getCode, e -> e));

    public static Optional<String> findName(String code) {
        return find(code).map(CryptoAsset::getName);
    }

    public static Optional<CryptoAsset> find(String code) {
        return Optional.ofNullable(CRYPTO_ASSETS_BY_CODE.get(code));
    }

    public static CryptoAsset findOrCreateCustom(String code) {
        return find(code).orElse(new CryptoAsset(code));
    }

    public static List<CryptoAsset> getCryptoAssets() {
        return CRYPTO_ASSETS;
    }
}
