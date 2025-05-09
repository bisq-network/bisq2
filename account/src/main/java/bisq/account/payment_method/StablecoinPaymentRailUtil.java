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
import java.util.stream.Collectors;

public class StablecoinPaymentRailUtil {

    public static List<StablecoinPaymentRail> getPaymentRails() {
        return List.of(StablecoinPaymentRail.values());
    }

    public static List<StablecoinPaymentRail> getPaymentRails(TradeProtocolType protocolType) {
        return switch (protocolType) {
            case BISQ_EASY, MU_SIG, BISQ_LIGHTNING -> getPaymentRails();
            case MONERO_SWAP, LIQUID_SWAP, BSQ_SWAP ->
                    throw new UnsupportedOperationException("No paymentMethods for that protocolType");
            default -> throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        };
    }

    public static List<StablecoinPaymentRail> getPaymentRails(String currencyCode) {
        return getPaymentRails().stream()
                .filter(paymentRail -> paymentRail.supportsCurrency(currencyCode))
                .collect(Collectors.toList());
    }
}