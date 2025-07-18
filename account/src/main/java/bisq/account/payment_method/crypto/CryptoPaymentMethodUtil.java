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

package bisq.account.payment_method.crypto;

import bisq.common.asset.CryptoAssetRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CryptoPaymentMethodUtil {
    private static final Map<String, CryptoPaymentRail> CRYPTO_PAYMENT_RAIL_BY_CODE = Map.of(
            "XMR", CryptoPaymentRail.NATIVE_CHAIN,
            "BSQ", CryptoPaymentRail.SMART_CONTRACT,
            "LTC", CryptoPaymentRail.NATIVE_CHAIN,
            "ETH", CryptoPaymentRail.NATIVE_CHAIN,
            "ETC", CryptoPaymentRail.NATIVE_CHAIN,
            "L-BTC", CryptoPaymentRail.SIDECHAIN,
            "LN-BTC", CryptoPaymentRail.LAYER_2,
            "GRIN", CryptoPaymentRail.NATIVE_CHAIN,
            "ZEC", CryptoPaymentRail.NATIVE_CHAIN,
            "DOGE", CryptoPaymentRail.NATIVE_CHAIN
    );

    private static final List<CryptoPaymentMethod> ALL_CRYPTO_PAYMENT_METHODS = new ArrayList<>();

    public static List<CryptoPaymentMethod> getPaymentMethods() {
        return CryptoAssetRepository.getCryptoAssets().stream()
                .map(asset -> new CryptoPaymentMethod(asset.getCode()))
                .collect(Collectors.toList());
    }

    public static CryptoPaymentMethod getPaymentMethod(String cryptoPaymentRailName, String code) {
        try {
            CryptoPaymentRail cryptoPaymentRail = CryptoPaymentRail.valueOf(cryptoPaymentRailName);
            return new CryptoPaymentMethod(cryptoPaymentRail, code);
        } catch (Throwable ignore) {
            return CryptoPaymentMethod.fromCustomName(cryptoPaymentRailName, code);
        }
    }

    public static List<CryptoPaymentRail> getPaymentRails() {
        return List.of(CryptoPaymentRail.values());
    }

    public static CryptoPaymentRail getCryptoPaymentRail(String code) {
        return CRYPTO_PAYMENT_RAIL_BY_CODE.getOrDefault(code, CryptoPaymentRail.UNDEFINED);
    }

}