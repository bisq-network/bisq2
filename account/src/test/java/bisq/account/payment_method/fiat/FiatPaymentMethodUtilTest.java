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

package bisq.account.payment_method.fiat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mental model under test:
 * - Bisq Easy (accountless offers) = ALL rails, including TELE_BIRR -> {@link #getPaymentMethods(String)}.
 * - Standard/classic account creation (desktop account wizard, MuSig, REST payment-accounts) = only
 *   account-backed rails -> {@link #getStandardAccountPaymentMethods(String)}.
 * <p>
 * TELE_BIRR must never disappear from the Bisq Easy list (that would defeat the whole feature), but must
 * never appear in the account-backed list (there is no classic account form for it).
 */
class FiatPaymentMethodUtilTest {

    @Test
    void bisqEasyListForEthiopianBirrIncludesTeleBirr() {
        List<FiatPaymentMethod> methods = FiatPaymentMethodUtil.getPaymentMethods("ETB");

        assertTrue(containsRail(methods, FiatPaymentRail.TELE_BIRR),
                "Bisq Easy fiat payment methods for ETB must keep TELE_BIRR available");
    }

    @Test
    void standardAccountListForEthiopianBirrExcludesTeleBirr() {
        List<FiatPaymentMethod> methods = FiatPaymentMethodUtil.getStandardAccountPaymentMethods("ETB");

        assertFalse(containsRail(methods, FiatPaymentRail.TELE_BIRR),
                "Standard (account-backed) payment methods must never include TELE_BIRR");
    }

    @Test
    void standardAccountListExcludesCustomAndCashApp() {
        List<FiatPaymentMethod> usdMethods = FiatPaymentMethodUtil.getStandardAccountPaymentMethods("USD");

        assertFalse(containsRail(usdMethods, FiatPaymentRail.CUSTOM));
        assertFalse(containsRail(usdMethods, FiatPaymentRail.CASH_APP));
    }

    @Test
    void standardAccountListStillContainsOrdinaryAccountBackedRails() {
        List<FiatPaymentMethod> eurMethods = FiatPaymentMethodUtil.getStandardAccountPaymentMethods("EUR");

        assertTrue(containsRail(eurMethods, FiatPaymentRail.SEPA),
                "SEPA is a standard account-backed rail and must remain available for EUR");
    }

    private static boolean containsRail(List<FiatPaymentMethod> methods, FiatPaymentRail rail) {
        return methods.stream().anyMatch(method -> method.getPaymentRail() == rail);
    }
}
