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

package bisq.offer.mu_sig.draft;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.common.market.Market;
import bisq.offer.mu_sig.draft.dependencies.AccountsProvider;
import com.google.common.collect.ImmutableMap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulates payment-method/account selection rules for a market.
 * <p>
 * Design: keeps all account eligibility, grouping, stale-selection cleanup, and rail-restriction
 * lookups in one pure service so workflow orchestration stays deterministic and readable.
 */
class PaymentMethodSelectionService {
    private final AccountsProvider accountsProvider;

    PaymentMethodSelectionService(AccountsProvider accountsProvider) {
        this.accountsProvider = checkNotNull(accountsProvider, "accountsProvider must not be null");
    }

    /* --------------------------------------------------------------------- */
    // Account loading and grouping
    /* --------------------------------------------------------------------- */

    MarketAccounts loadAccountsForMarket(Market market) {
        checkNotNull(market, "market must not be null");
        List<Account<?, ?>> accountsForMarket = checkNotNull(accountsProvider.findAccountsForMarket(market),
                "accountsForMarket must not be null");
        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = accountsForMarket.stream()
                .collect(Collectors.groupingBy(Account::getPaymentMethod, Collectors.toList()));
        return new MarketAccounts(accountsForMarket, accountsByPaymentMethod);
    }

    /* --------------------------------------------------------------------- */
    // Selection
    /* --------------------------------------------------------------------- */

    List<? extends PaymentMethod<?>> findSelectedPaymentMethodsToRemove(ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                                                        List<Account<?, ?>> accountsForMarket) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        checkNotNull(accountsForMarket, "accountsForMarket must not be null");
        return selectedAccountByPaymentMethod.entrySet().stream()
                .filter(entry -> !accountsForMarket.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    Optional<Account<?, ?>> findAccountToAutoSelect(List<Account<?, ?>> accountsForMarket,
                                                    ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(accountsForMarket, "accountsForMarket must not be null");
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");

        if (accountsForMarket.size() != 1) {
            return Optional.empty();
        }

        Account<?, ?> account = accountsForMarket.getFirst();
        Account<?, ?> existing = selectedAccountByPaymentMethod.get(account.getPaymentMethod());
        return account.equals(existing) ? Optional.empty() : Optional.of(account);
    }

    /* --------------------------------------------------------------------- */
    // Restriction lookup
    /* --------------------------------------------------------------------- */

    PaymentRail findMostRestrictiveSelectedPaymentRail(ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        return selectedAccountByPaymentMethod.values().stream()
                .map(Account::getPaymentMethod)
                .map(PaymentMethod::getPaymentRail)
                .min(Comparator.comparing(TradeAmountLimits::getMaxTradeLimitInUsd))
                .orElse(null);
    }

    /**
     * Snapshot of all market-eligible accounts plus a list of accounts keyed by payment method.
     */
    record MarketAccounts(List<Account<?, ?>> accountsForMarket,
                          Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod) {
        MarketAccounts {
            checkNotNull(accountsForMarket, "accountsForMarket must not be null");
            checkNotNull(accountsByPaymentMethod, "accountsByPaymentMethod must not be null");
        }
    }
}
