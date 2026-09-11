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

package bisq.offer.mu_sig.use_case.take_offer.payment_method;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The taker's accounts as seen through an offer: the eligible accounts, grouped by payment
 * method, and the accounts of offered methods that failed the AccountOption compatibility
 * check together with the reason.
 */
public record OfferAccounts(List<Account<?, ?>> accountsForMarket,
                            Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod,
                            Map<PaymentMethod<?>, List<AccountCompatibilityMismatch>> incompatibleAccountsByPaymentMethod) {
    public OfferAccounts {
        checkNotNull(accountsForMarket, "accountsForMarket must not be null");
        checkNotNull(accountsByPaymentMethod, "accountsByPaymentMethod must not be null");
        checkNotNull(incompatibleAccountsByPaymentMethod, "incompatibleAccountsByPaymentMethod must not be null");
    }
}
