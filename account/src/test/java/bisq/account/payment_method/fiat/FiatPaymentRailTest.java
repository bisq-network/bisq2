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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the centralized "does this rail support a standard (classic)
 * payment account" predicate.
 * <p>
 * TELE_BIRR is a Bisq Easy-only rail (no classic account form). Before this predicate
 * existed, the account-creation / MuSig / REST consumers each hand-filtered CUSTOM and
 * CASH_APP, and only one of the three call sites was updated to also exclude TELE_BIRR
 * when it was introduced — leaving the REST payment-accounts endpoint (which the mobile
 * client depends on) throwing when it encountered TELE_BIRR.
 */
class FiatPaymentRailTest {

    @Test
    void teleBirrDoesNotSupportStandardAccountCreation() {
        assertFalse(FiatPaymentRail.supportsStandardAccountCreation(FiatPaymentRail.TELE_BIRR),
                "TELE_BIRR has no classic account form and must be excluded from standard account creation");
    }

    @Test
    void cashAppDoesNotSupportStandardAccountCreation() {
        assertFalse(FiatPaymentRail.supportsStandardAccountCreation(FiatPaymentRail.CASH_APP));
    }

    @Test
    void customDoesNotSupportStandardAccountCreation() {
        assertFalse(FiatPaymentRail.supportsStandardAccountCreation(FiatPaymentRail.CUSTOM));
    }

    @Test
    void ordinaryRailSupportsStandardAccountCreation() {
        assertTrue(FiatPaymentRail.supportsStandardAccountCreation(FiatPaymentRail.SEPA));
    }

    @ParameterizedTest
    @EnumSource(value = FiatPaymentRail.class, names = {"CASH_APP", "CUSTOM", "TELE_BIRR"}, mode = EnumSource.Mode.EXCLUDE)
    void everyRailOtherThanTheAccountlessOnesSupportsStandardAccountCreation(FiatPaymentRail rail) {
        assertTrue(FiatPaymentRail.supportsStandardAccountCreation(rail),
                rail + " should support standard account creation");
    }
}
