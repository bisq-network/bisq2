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

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.offer.Offer;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class PaymentMethodUtil {
    public static List<PaymentMethodSpec> createBitcoinPaymentMethodSpecs(List<BitcoinPaymentRail> bitcoinPaymentMethods) {
        return bitcoinPaymentMethods.stream()
                .map(bitcoinPaymentMethod -> new BitcoinPaymentMethodSpec(bitcoinPaymentMethod.name()))
                .collect(Collectors.toList());
    }

    public static List<PaymentMethodSpec> createBitcoinMainChainPaymentMethodSpec() {
        return createBitcoinPaymentMethodSpecs(List.of(BitcoinPaymentRail.MAIN_CHAIN));
    }

    public static List<PaymentMethodSpec> createFiatPaymentMethodSpecs(List<String> paymentMethodNames) {
        checkArgument(!paymentMethodNames.isEmpty());
        return paymentMethodNames.stream()
                .map(FiatPaymentMethodSpec::new)
                .collect(Collectors.toList());
    }


    public static List<String> toPaymentMethodNames(Collection<PaymentMethodSpec> paymentMethodSpecs) {
        return paymentMethodSpecs.stream()
                .map(PaymentMethodSpec::getPaymentMethodName)
                .sorted()
                .collect(Collectors.toList());
    }


    public static List<String> getBaseSidePaymentMethodNames(Offer offer) {
        return toPaymentMethodNames(offer.getBaseSidePaymentMethodSpecs());
    }

    public static List<String> getQuoteSidePaymentMethodNames(Offer offer) {
        return toPaymentMethodNames(offer.getQuoteSidePaymentMethodSpecs());
    }
}