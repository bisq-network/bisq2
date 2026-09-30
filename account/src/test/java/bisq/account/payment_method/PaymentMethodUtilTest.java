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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the currency-agnostic dispatcher used by the MuSig create-offer flow and the account wizard.
 * The MuSig picker calls {@link PaymentMethodUtil#getStandardAccountPaymentMethods(String)}, so an
 * accountless-only rail like TELE_BIRR must not leak through here (selecting it would dead-end in the
 * no-account/create-account overlay). The full {@link PaymentMethodUtil#getPaymentMethods(String)} list
 * that Bisq Easy uses must still keep it.
 */
class PaymentMethodUtilTest {

    @Test
    void standardAccountListForEthiopianBirrExcludesTeleBirr() {
        List<PaymentMethod<?>> methods = PaymentMethodUtil.getStandardAccountPaymentMethods("ETB");

        assertFalse(containsRail(methods, "TELE_BIRR"),
                "MuSig/account-backed payment methods for ETB must never include TELE_BIRR");
    }

    @Test
    void bisqEasyListForEthiopianBirrIncludesTeleBirr() {
        List<PaymentMethod<?>> methods = PaymentMethodUtil.getPaymentMethods("ETB");

        assertTrue(containsRail(methods, "TELE_BIRR"),
                "Bisq Easy payment methods for ETB must keep TELE_BIRR available");
    }

    private static boolean containsRail(List<PaymentMethod<?>> methods, String railName) {
        return methods.stream().anyMatch(method -> method.getPaymentRailName().equals(railName));
    }
}
