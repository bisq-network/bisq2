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

import bisq.account.protocol_type.TradeProtocolType;

import java.util.List;

public class CryptoPaymentMethodUtil {
    public static CryptoPaymentMethod getPaymentMethod(String name, String currencyCode) {
        try {
            CryptoPaymentRail cryptoPaymentRail = CryptoPaymentRail.valueOf(name);
            CryptoPaymentMethod cryptoPaymentMethod = CryptoPaymentMethod.fromPaymentRail(cryptoPaymentRail, currencyCode);
            if (!cryptoPaymentMethod.isCustomPaymentMethod()) {
                return cryptoPaymentMethod;
            }
        } catch (Throwable ignore) {
        }
        return CryptoPaymentMethod.fromCustomName(name, currencyCode);
    }

    public static List<CryptoPaymentRail> getPaymentRails() {
        return List.of(CryptoPaymentRail.values());
    }

    public static List<CryptoPaymentRail> getPaymentRails(TradeProtocolType protocolType) {
        return switch (protocolType) {
            case BISQ_EASY -> throw new IllegalArgumentException("No support for CryptoPaymentMethods for BISQ_EASY");
            case BISQ_MU_SIG, LIGHTNING_ESCROW -> getPaymentRails();
            case MONERO_SWAP -> List.of(CryptoPaymentRail.MONERO);
            case LIQUID_SWAP -> List.of(CryptoPaymentRail.LIQUID);
            case BSQ_SWAP -> List.of(CryptoPaymentRail.BSQ);
            default -> throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        };
    }
}