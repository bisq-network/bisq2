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

package bisq.offer.mu_sig.use_case.create_offer.payment_method;

import bisq.account.accounts.Account;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public record PaymentMethodAccountSelection(Optional<Account<?, ?>> accountToAutoSelect,
                                            List<Account<?, ?>> accountsRequiringSelection) {
    public PaymentMethodAccountSelection {
        checkNotNull(accountToAutoSelect, "accountToAutoSelect must not be null");
        checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
        accountsRequiringSelection = List.copyOf(accountsRequiringSelection);
    }

    public static PaymentMethodAccountSelection noAccount() {
        return new PaymentMethodAccountSelection(Optional.empty(), List.of());
    }

    public static PaymentMethodAccountSelection singleAccount(Account<?, ?> accountToAutoSelect) {
        checkNotNull(accountToAutoSelect, "accountToAutoSelect must not be null");
        return new PaymentMethodAccountSelection(Optional.of(accountToAutoSelect), List.of());
    }

    public static PaymentMethodAccountSelection multipleAccounts(List<Account<?, ?>> accountsRequiringSelection) {
        checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
        return new PaymentMethodAccountSelection(Optional.empty(), accountsRequiringSelection);
    }
}
