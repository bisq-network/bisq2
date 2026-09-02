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
import bisq.account.payment_method.PaymentRail;
import bisq.common.market.Market;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.MarketAccounts;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodAccountSelection;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class TakeOfferPaymentMethodService {
    @Getter(AccessLevel.PACKAGE)
    @Delegate
    private final TakeOfferPaymentMethodModel model;
    private final PaymentMethodSelectionService paymentMethodSelectionService;

    public enum PaymentMethodSelectionStatus {
        NO_ACCOUNT_AVAILABLE,
        SINGLE_ACCOUNT_SELECTED,
        ACCOUNT_SELECTION_REQUIRED
    }

    public record PaymentMethodSelectionResult(PaymentMethodSelectionStatus status,
                                               List<Account<?, ?>> accountsRequiringSelection) {
        public PaymentMethodSelectionResult {
            checkNotNull(status, "status must not be null");
            checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
            accountsRequiringSelection = List.copyOf(accountsRequiringSelection);
        }

        public static PaymentMethodSelectionResult noAccountAvailable() {
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.NO_ACCOUNT_AVAILABLE, List.of());
        }

        public static PaymentMethodSelectionResult singleAccountSelected() {
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.SINGLE_ACCOUNT_SELECTED, List.of());
        }

        public static PaymentMethodSelectionResult accountSelectionRequired(List<Account<?, ?>> accountsRequiringSelection) {
            checkNotNull(accountsRequiringSelection, "accountsRequiringSelection must not be null");
            return new PaymentMethodSelectionResult(PaymentMethodSelectionStatus.ACCOUNT_SELECTION_REQUIRED, accountsRequiringSelection);
        }
    }

    public TakeOfferPaymentMethodService(PaymentMethodSelectionService paymentMethodSelectionService) {
        this.model = new TakeOfferPaymentMethodModel();
        this.paymentMethodSelectionService = checkNotNull(paymentMethodSelectionService, "paymentMethodSelectionService must not be null");
    }

    public void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        model.putAccountsByPaymentMethod(paymentMethod, account);
    }

    public void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        model.removeAccountsByPaymentMethod(paymentMethod);
    }

    public void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        model.putAllAccountsByPaymentMethod(selectedAccountByPaymentMethod);
    }

    public void clearAccountsByPaymentMethod() {
        model.clearAccountsByPaymentMethod();
    }

    public void putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        checkNotNull(account, "account must not be null");
        putSelectedAccountByPaymentMethod(paymentMethod, account, true);
    }

    public void removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        removeSelectedAccountByPaymentMethod(paymentMethod, true);
    }

    public void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        checkNotNull(selectedAccountByPaymentMethod, "selectedAccountByPaymentMethod must not be null");
        putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod, true);
    }

    public void clearSelectedAccountByPaymentMethod() {
        clearSelectedAccountByPaymentMethod(true);
    }

    public PaymentMethodSelectionResult onPaymentMethodSelected(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");

        PaymentMethodAccountSelection selection = paymentMethodSelectionService.findAccountsSelection(
                getAccountsByPaymentMethod(),
                paymentMethod);
        if (selection.accountToAutoSelect().isPresent()) {
            putSelectedAccountByPaymentMethod(paymentMethod, selection.accountToAutoSelect().get());
            return PaymentMethodSelectionResult.singleAccountSelected();
        }

        if (!selection.accountsRequiringSelection().isEmpty()) {
            return PaymentMethodSelectionResult.accountSelectionRequired(selection.accountsRequiringSelection());
        }

        return PaymentMethodSelectionResult.noAccountAvailable();
    }

    /* --------------------------------------------------------------------- */
    // Package scope helpers used by workflow/state engine callbacks
    /* --------------------------------------------------------------------- */

    public void updatePaymentMethods(Market market) {
        MarketAccounts marketAccounts = paymentMethodSelectionService.loadAccountsForMarket(market);
        List<Account<?, ?>> accountsForMarket = marketAccounts.accountsForMarket();
        Map<PaymentMethod<?>, List<Account<?, ?>>> map = marketAccounts.accountsByPaymentMethod();
        if (!getAccountsByPaymentMethod().equals(map)) {
            clearAccountsByPaymentMethod();
            putAllAccountsByPaymentMethod(map);
        }

        boolean selectedAccountsChanged = false;

        // Remove payment methods which are not present in the eligible accounts
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = getSelectedAccountByPaymentMethod();
        List<? extends PaymentMethod<?>> paymentMethodsToRemove = paymentMethodSelectionService.findSelectedPaymentMethodsToRemove(selectedAccountByPaymentMethod,
                accountsForMarket);
        if (!paymentMethodsToRemove.isEmpty()) {
            selectedAccountsChanged = true;
            paymentMethodsToRemove.forEach(paymentMethod -> removeSelectedAccountByPaymentMethod(paymentMethod, false));
        }

        // If we have only one, we pre-select
        Optional<Account<?, ?>> accountToAutoSelect = paymentMethodSelectionService.findAccountToAutoSelect(accountsForMarket,
                getSelectedAccountByPaymentMethod());
        if (accountToAutoSelect.isPresent()) {
            Account<?, ?> account = accountToAutoSelect.get();
            selectedAccountsChanged |= putSelectedAccountByPaymentMethod(account.getPaymentMethod(), account, false);
        }

        if (selectedAccountsChanged) {
        }
    }

    public PaymentRail getSelectedPaymentRail() {
        return paymentMethodSelectionService.findMostRestrictiveSelectedPaymentRail(getSelectedAccountByPaymentMethod());
    }

    private boolean putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                      Account<?, ?> account,
                                                      boolean recalculateTradeAmountConstraints) {
        Account<?, ?> existing = getSelectedAccountByPaymentMethod().get(paymentMethod);
        if (account.equals(existing)) {
            return false;
        }
        model.putSelectedAccountByPaymentMethod(paymentMethod, account);
        if (recalculateTradeAmountConstraints) {
        }
        return true;
    }

    private boolean removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod,
                                                         boolean recalculateTradeAmountConstraints) {
        if (!getSelectedAccountByPaymentMethod().containsKey(paymentMethod)) {
            return false;
        }
        model.removeSelectedAccountByPaymentMethod(paymentMethod);
        if (recalculateTradeAmountConstraints) {
        }
        return true;
    }

    private boolean putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                                         boolean recalculateTradeAmountConstraints) {
        if (selectedAccountByPaymentMethod.isEmpty()) {
            return clearSelectedAccountByPaymentMethod(recalculateTradeAmountConstraints);
        }
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> existing = getSelectedAccountByPaymentMethod();
        boolean changed = selectedAccountByPaymentMethod.entrySet().stream()
                .anyMatch(entry -> !entry.getValue().equals(existing.get(entry.getKey())));
        if (!changed) {
            return false;
        }
        model.putAllSelectedAccountByPaymentMethod(selectedAccountByPaymentMethod);
        if (recalculateTradeAmountConstraints) {
        }
        return true;
    }

    private boolean clearSelectedAccountByPaymentMethod(boolean recalculateTradeAmountConstraints) {
        if (getSelectedAccountByPaymentMethod().isEmpty()) {
            return false;
        }
        model.clearSelectedAccountByPaymentMethod();
        if (recalculateTradeAmountConstraints) {
        }
        return true;
    }
}
