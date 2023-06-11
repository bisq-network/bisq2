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

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class FiatPaymentUtil {
    public static FiatPaymentMethod from(String name) {
        try {
            return new FiatPaymentMethod(FiatPaymentRail.valueOf(name));
        } catch (Throwable ignore) {
            return new FiatPaymentMethod(name);
        }
    }

    public static List<FiatPaymentRail> getFiatPaymentRails() {
        FiatPaymentRail[] values = FiatPaymentRail.values();
        return List.of(values);
    }

    public static List<FiatPaymentRail> getFiatPaymentRails(TradeProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
            case LIGHTNING_X:
                return getFiatPaymentRails();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
                throw new IllegalArgumentException("No paymentMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }

    public static List<FiatPaymentRail> getFiatPaymentRailsForCurrencyCode(String currencyCode) {
        return getFiatPaymentRails().stream()
                .filter(fiatPaymentRail -> {
                    if (currencyCode.equals("EUR") &&
                            (fiatPaymentRail == FiatPaymentRail.SWIFT ||
                                    fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK)) {
                        // For EUR, we don't add SWIFT and NATIONAL_BANK
                        return false;
                    }
                    // We add NATIONAL_BANK to all
                    if (fiatPaymentRail == FiatPaymentRail.NATIONAL_BANK) {
                        return true;
                    }
                    return new HashSet<>(fiatPaymentRail.getCurrencyCodes()).contains(currencyCode);
                })
                .collect(Collectors.toList());
    }

    public static List<String> getFiatPaymentRailNames(String currencyCode) {
        return getFiatPaymentRailsForCurrencyCode(currencyCode).stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}