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

import bisq.common.asset.Asset;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodUtil {
    //todo not used yet
    public static PaymentMethod<? extends PaymentRail> getPaymentMethod(String name, String code) {
        if (Asset.isFiat(code)) {
            return FiatPaymentMethodUtil.getPaymentMethod(name);
        } else {
            if (code.equals("BTC")) {
                return BitcoinPaymentMethodUtil.getPaymentMethod(name);
            } else {
                return CryptoPaymentMethodUtil.getPaymentMethod(name, code);
            }
        }
    }

    public static PaymentRail getPaymentRail(String name, String code) {
        return getPaymentMethod(name, code).getPaymentRail();
    }

    public static List<String> getPaymentMethodNames(List<? extends PaymentMethod<?>> paymentMethods) {
        return paymentMethods.stream()
                .map(PaymentMethod::getPaymentRailName)
                .collect(Collectors.toList());
    }
}