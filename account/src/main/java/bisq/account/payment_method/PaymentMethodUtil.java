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
import bisq.common.currency.TradeCurrency;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodUtil {
    public static List<String> getPaymentMethodNames(List<? extends PaymentMethod<?>> paymentMethods) {
        return paymentMethods.stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toList());
    }

    public static List<? extends PaymentRail> getPaymentRails(TradeProtocolType protocolType, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatPaymentRailUtil.getPaymentRails(protocolType);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinPaymentMethodUtil.getPaymentRails(protocolType);
            } else {
                return CryptoPaymentMethodUtil.getPaymentRails(protocolType);
            }
        }
    }

    public static PaymentMethod<? extends PaymentRail> getPaymentMethod(String name, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatPaymentMethodUtil.getPaymentMethod(name);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinPaymentMethodUtil.getPaymentMethod(name);
            } else {
                return CryptoPaymentMethodUtil.getPaymentMethod(name, currencyCode);
            }
        }
    }

    public static PaymentRail getPaymentRail(String name, String currencyCode) {
        return getPaymentMethod(name, currencyCode).getPaymentRail();
    }

}