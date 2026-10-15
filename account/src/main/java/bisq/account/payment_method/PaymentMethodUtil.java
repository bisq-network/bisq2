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

import bisq.account.payment_method.crypto.CryptoPaymentMethodUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethodUtil;
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

    public static List<PaymentMethod<?>> getPaymentMethods(String code) {
        if (Asset.isFiat(code)) {
            return FiatPaymentMethodUtil.getPaymentMethods(code).stream()
                    .map(pm -> (PaymentMethod<?>) pm)
                    .collect(Collectors.toList());
        } else if (Asset.isAltcoin(code)) {
            return CryptoPaymentMethodUtil.getPaymentMethods(code).stream()
                    .map(pm -> (PaymentMethod<?>) pm)
                    .collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException("getPaymentMethods only supports fiat and altcoins. CurrencyCode: " + code);
        }
    }

    /**
     * Standard, account-backed payment methods for the given currency. For fiat this is the subset of
     * {@link #getPaymentMethods(String)} that can back a classic payment account (excludes accountless-only
     * rails like TELE_BIRR); for altcoins every crypto payment method is account-backed, so the list is the
     * same as {@link #getPaymentMethods(String)}.
     * <p>
     * Use this for the account-creation wizard, the MuSig offer flow, and the REST payment-accounts API. Do
     * NOT use it for the Bisq Easy offerbook — that must keep listing accountless-only rails.
     */
    public static List<PaymentMethod<?>> getStandardAccountPaymentMethods(String code) {
        if (Asset.isFiat(code)) {
            return FiatPaymentMethodUtil.getStandardAccountPaymentMethods(code).stream()
                    .map(pm -> (PaymentMethod<?>) pm)
                    .collect(Collectors.toList());
        } else if (Asset.isAltcoin(code)) {
            return CryptoPaymentMethodUtil.getPaymentMethods(code).stream()
                    .map(pm -> (PaymentMethod<?>) pm)
                    .collect(Collectors.toList());
        } else {
            throw new UnsupportedOperationException("getStandardAccountPaymentMethods only supports fiat and altcoins. CurrencyCode: " + code);
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
