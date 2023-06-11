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

public class BitcoinPaymentUtil {
    public static BitcoinPaymentMethod from(String name) {
        try {
            return new BitcoinPaymentMethod(BitcoinPaymentRail.valueOf(name));
        } catch (Throwable ignore) {
            return new BitcoinPaymentMethod(name);
        }
    }

    public static List<BitcoinPaymentRail> getBitcoinPaymentRails() {
        return List.of(BitcoinPaymentRail.values());
    }

    public static List<BitcoinPaymentRail> getLNPaymentRails() {
        return List.of(BitcoinPaymentRail.LN);
    }

    public static List<BitcoinPaymentRail> getBitcoinPaymentRails(TradeProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
                return getBitcoinPaymentRails();
            case LIGHTNING_X:
                return getLNPaymentRails();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
                throw new IllegalArgumentException("No paymentMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }
}