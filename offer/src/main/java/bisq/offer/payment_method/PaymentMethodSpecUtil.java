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

package bisq.offer.payment_method;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodSpecUtil {
    public static List<BitcoinPaymentMethodSpec> createBitcoinPaymentMethodSpecs(List<BitcoinPaymentMethod> paymentMethods) {
        return paymentMethods.stream()
                .map(BitcoinPaymentMethodSpec::new)
                .collect(Collectors.toList());
    }

    public static List<BitcoinPaymentMethodSpec> createBitcoinMainChainPaymentMethodSpec() {
        return createBitcoinPaymentMethodSpecs(List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)));
    }

    public static List<FiatPaymentMethodSpec> createFiatPaymentMethodSpecs(List<FiatPaymentMethod> paymentMethods) {
        return paymentMethods.stream()
                .map(FiatPaymentMethodSpec::new)
                .collect(Collectors.toList());
    }

    public static <M extends PaymentMethod<?>, T extends PaymentMethodSpec<M>> List<M> getPaymentMethods(Collection<T> paymentMethodSpecs) {
        return paymentMethodSpecs.stream()
                .map(PaymentMethodSpec::getPaymentMethod)
                .collect(Collectors.toList());
    }

    public static List<String> getPaymentMethodNames(Collection<? extends PaymentMethodSpec<?>> paymentMethodSpecs) {
        return paymentMethodSpecs.stream().map(PaymentMethodSpec::getPaymentMethodName).collect(Collectors.toList());
    }
}