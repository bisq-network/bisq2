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

import bisq.common.validation.RegexValidation;

import java.util.regex.Pattern;

// The rule the daemon applies to the password of a new wallet; a letter of a script without case counts as both cases
public class WalletPasswordValidation implements RegexValidation {
    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[\\p{Ll}\\p{Lo}])(?=.*[\\p{Lu}\\p{Lo}])(?=.*\\p{Nd})(?=.*[^\\p{L}\\p{Nd}]).{8,}$");

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public String getI18nKey() {
        return "wallet.protectWallet.password.rules";
    }
}
