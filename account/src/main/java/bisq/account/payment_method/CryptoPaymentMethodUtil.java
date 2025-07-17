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

import bisq.common.asset.CryptoAssetRepository;

import java.util.List;
import java.util.stream.Collectors;

public class CryptoPaymentMethodUtil {

    public static List<CryptoPaymentMethod> getAllCryptoPaymentMethods() {
        return CryptoAssetRepository.getCryptoAssets().stream()
                .map(currency -> new CryptoPaymentMethod(CryptoPaymentRail.NATIVE_CHAIN, currency.getCode()))
                .collect(Collectors.toList());
    }

    public static CryptoPaymentMethod getPaymentMethod(String cryptoPaymentRailName, String currencyCode) {
        try {
            CryptoPaymentRail cryptoPaymentRail = CryptoPaymentRail.valueOf(cryptoPaymentRailName);
            CryptoPaymentMethod cryptoPaymentMethod = new CryptoPaymentMethod(cryptoPaymentRail, currencyCode);
            if (!cryptoPaymentMethod.isCustomPaymentMethod()) {
                return cryptoPaymentMethod;
            }
        } catch (Throwable ignore) {
        }
        return CryptoPaymentMethod.fromCustomName(cryptoPaymentRailName, currencyCode);
    }

    public static List<CryptoPaymentRail> getPaymentRails() {
        return List.of(CryptoPaymentRail.values());
    }
}