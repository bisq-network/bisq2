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

import bisq.account.payment_method.PaymentMethod;
import bisq.i18n.Res;
import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentMethodSpecFormatter {
    public static String fromPaymentMethodSpecs(List<? extends PaymentMethodSpec<?>> paymentMethodSpecs) {
        return fromPaymentMethodSpecs(paymentMethodSpecs, true);
    }

    public static String fromPaymentMethodSpecs(List<? extends PaymentMethodSpec<?>> paymentMethodSpecs, boolean useShortDisplayString) {
        return toCommaSeparatedString(paymentMethodSpecs.stream()
                .map(PaymentMethodSpec::getPaymentMethod)
                .map(method -> useShortDisplayString ? method.getShortDisplayString() : method.getDisplayString())
                .collect(Collectors.toList()));
    }

    public static String fromPaymentMethods(List<? extends PaymentMethod<?>> paymentMethods) {
        return fromPaymentMethods(paymentMethods, true);
    }

    public static String fromPaymentMethods(List<? extends PaymentMethod<?>> paymentMethods, boolean useShortDisplayString) {
        return toCommaSeparatedString(paymentMethods.stream()
                .map(method -> useShortDisplayString ? method.getShortDisplayString() : method.getDisplayString())
                .collect(Collectors.toList()));
    }

    /**
     * @param paymentMethodNames The names of the payment methods or custom payment method names entered by the user.
     * @return A sorted list of the display strings from the paymentMethodNames.
     * If no translation string is found we use the provided paymentMethodName (e.g. for custom paymentMethodNames)
     */
    private static List<String> toDisplayStrings(Collection<String> paymentMethodNames) {
        return toDisplayStrings(paymentMethodNames, true);
    }

    private static List<String> toDisplayStrings(Collection<String> paymentMethodNames, boolean useShortDisplayString) {
        return paymentMethodNames.stream()
                .map(name -> {
                    String shortName = name + "_SHORT";
                    if (useShortDisplayString && Res.has(shortName)) {
                        return Res.get(shortName);
                    } else if (Res.has(name)) {
                        return Res.get(name);
                    } else {
                        return name;
                    }
                })
                .sorted()
                .collect(Collectors.toList());
    }

    private static String toCommaSeparatedString(List<String> paymentMethodsAsDisplayStrings) {
        return Joiner.on(", ").join(paymentMethodsAsDisplayStrings);
    }
}