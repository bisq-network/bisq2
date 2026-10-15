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
import bisq.account.payment_method.PaymentMethod;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public record PaymentMethodSelectionResult(PaymentMethodSelectionStatus status,
                                           List<Account<?, ?>> accountsRequiringSelection,
                                           Optional<Map.Entry<PaymentMethod<?>, Account<?, ?>>> methodAccountEntry) {
    public PaymentMethodSelectionResult {
        checkNotNull(status, "status must not be null");
        checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
        checkNotNull(methodAccountEntry, "methodAccountEntry must not be null");
        accountsRequiringSelection = List.copyOf(accountsRequiringSelection);
    }

    public static PaymentMethodSelectionResult noAccountAvailable() {
        return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.NO_ACCOUNT_AVAILABLE, List.of(), Optional.empty());
    }

    public static PaymentMethodSelectionResult singleAccountSelected(Map.Entry<PaymentMethod<?>, Account<?, ?>> methodAccountEntry) {
        checkNotNull(methodAccountEntry, "methodAccountEntry must not be null");
        return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.SINGLE_ACCOUNT_SELECTED, List.of(), Optional.of(methodAccountEntry));
    }

    public static PaymentMethodSelectionResult accountSelectionRequired(List<Account<?, ?>> accountsRequiringSelection) {
        checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
        return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.ACCOUNT_SELECTION_REQUIRED, accountsRequiringSelection, Optional.empty());
    }
}
