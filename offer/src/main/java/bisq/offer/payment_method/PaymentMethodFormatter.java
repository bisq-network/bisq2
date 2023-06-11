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

import bisq.i18n.Res;
import bisq.offer.Offer;
import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodFormatter {
    /**
     * @param paymentMethodNames The names of the payment methods or custom payment method names entered by the user.
     * @return A comma separated list of the display strings of the paymentMethodNames
     */
    public static String formatPaymentMethodNames(List<String> paymentMethodNames) {
        return toCommaSeparatedString(toDisplayStrings(paymentMethodNames));
    }

    public static String formatQuoteSidePaymentMethods(Offer offer) {
        return toCommaSeparatedString(toDisplayStrings(offer.getQuoteSidePaymentMethodSpecs()));
    }

    private static List<String> toDisplayStrings(List<PaymentMethodSpec> paymentMethodSpecs) {
        return toDisplayStrings(PaymentMethodUtil.toPaymentMethodNames(paymentMethodSpecs));
    }

    /**
     * @param paymentMethodNames The names of the payment methods or custom payment method names entered by the user.
     * @return A sorted list of the display strings from the paymentMethodNames.
     * If no translation string is found we use the provided paymentMethodName (e.g. for custom paymentMethodNames)
     */
    private static List<String> toDisplayStrings(Collection<String> paymentMethodNames) {
        return paymentMethodNames.stream()
                .map(paymentMethodName -> {
                    if (Res.has(paymentMethodName)) {
                        return Res.get(paymentMethodName);
                    } else {
                        return paymentMethodName;
                    }
                })
                .sorted()
                .collect(Collectors.toList());
    }

    private static String toCommaSeparatedString(List<String> paymentMethodsAsDisplayStrings) {
        return Joiner.on(", ").join(paymentMethodsAsDisplayStrings);
    }
}