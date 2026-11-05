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

package bisq.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletPasswordValidationTest {
    private final WalletPasswordValidation validation = new WalletPasswordValidation();

    @Test
    void passwordsTheDaemonAccepts() {
        assertTrue(validation.isValid("Wallet-Pa55!"));
        assertTrue(validation.isValid("Bisq-Wallet-2026!"));
        assertTrue(validation.isValid("a1!bcdefG"));
        // A script without case counts as both lowercase and uppercase
        assertTrue(validation.isValid("密码密码密码1!"));
    }

    @Test
    void passwordsTheDaemonRefuses() {
        assertFalse(validation.isValid("walletpass1"));
        assertFalse(validation.isValid("Short1!"));
        assertFalse(validation.isValid("ALLUPPER1!"));
        assertFalse(validation.isValid("alllower1!"));
        assertFalse(validation.isValid("NoDigits!!"));
        assertFalse(validation.isValid("NoSpecial11"));
        assertFalse(validation.isValid(""));
    }
}
