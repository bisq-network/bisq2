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
import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.dependencies.AccountsProvider;
import com.google.common.collect.ImmutableMap;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains payment-method and account selection state for the create-offer draft.
 * <p>
 * The create-offer specification allows selecting up to {@value #MAX_NUM_PAYMENT_METHODS}
 * payment methods. A method can only be selected with an account that is currently eligible for
 * the active market, and if the market leaves exactly one eligible account in total it is
 * auto-selected. The selected accounts also drive payment-rail based trade amount limits.
 */
@Slf4j
public class PaymentMethodSelection extends LifecycleScope {
    public static final int MAX_NUM_PAYMENT_METHODS = 4;
    public static final String MAX_PAYMENT_METHODS_REACHED = "maxPaymentMethodsReached";
    public static final String ACCOUNT_NOT_ELIGIBLE_FOR_MARKET = "accountNotEligibleForMarket";

    @Delegate
    private final CreateOfferPaymentMethodModel model;
    private final Set<Consumer<Map.Entry<PaymentMethod<?>, Account<?, ?>>>> methodAccountEntryListeners = new CopyOnWriteArraySet<>();
    private final MarketSelection marketSelection;
    private final AccountsProvider accountsProvider;
    private final Object draftLock;

    public PaymentMethodSelection(MarketSelection marketSelection, AccountsProvider accountsProvider, Object draftLock) {
        this.draftLock = checkNotNull(draftLock, "draftLock must not be null");
        this.marketSelection = checkNotNull(marketSelection, "marketUseCase must not be null");
        this.accountsProvider = checkNotNull(accountsProvider, "accountsProvider must not be null");
        this.model = new CreateOfferPaymentMethodModel();
    }

    @Override
    public void initialize() {
        addDisposable(marketSelection.marketObservable().addObserver(this::update));
    }


    /* --------------------------------------------------------------------- */
    // Update
    /* --------------------------------------------------------------------- */

    // If market changes we update the accounts by paymentMethod map, maybe remove the selected accountByPaymentMethodEntry
    // and maybe pre-select the accountByPaymentMethodEntry if only one account is present.
    private void update(Market market) {
        if (market == null) {
            return;
        }
        MarketAccounts marketAccounts = loadAccountsForMarket(market, accountsProvider);
        List<Account<?, ?>> accountsForMarket = marketAccounts.accountsForMarket();
        Map<PaymentMethod<?>, List<Account<?, ?>>> map = marketAccounts.accountsByPaymentMethod();
        if (!getAccountsByPaymentMethod().equals(map)) {
            model.clearAccountsByPaymentMethod();
            model.putAllAccountsByPaymentMethod(map);
        }

        // Remove payment methods which are not present in the eligible accounts
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = getAccountByPaymentMethod();
        findSelectedPaymentMethodsToRemove(selectedAccountByPaymentMethod, accountsForMarket)
                .forEach(model::removeAccountByPaymentMethod);

        // If we have only one, we pre-select
        selectedAccountByPaymentMethod = getAccountByPaymentMethod(); // read it again as it might have changed from remove call
        findAccountToAutoSelect(accountsForMarket, selectedAccountByPaymentMethod)
                .ifPresent(account -> {
                    PaymentMethod<?> paymentMethod = account.getPaymentMethod();
                    Map.Entry<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethodEntry = Map.entry(paymentMethod, account);
                    model.addAccountByPaymentMethodEntry(accountByPaymentMethodEntry);
                });
    }


    /* --------------------------------------------------------------------- */
    // User interaction
    /* --------------------------------------------------------------------- */

    public PaymentMethodSelectionResult evaluatePaymentMethodSelectionResult(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");

        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = getAccountsByPaymentMethod();
        PaymentMethodAccountSelection selection = findAccountsSelection(
                accountsByPaymentMethod,
                paymentMethod);
        if (selection.accountToAutoSelect().isPresent()) {
            Account<?, ?> account = selection.accountToAutoSelect().get();
            Map.Entry<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethodEntry = Map.entry(paymentMethod, account);
            return PaymentMethodSelectionResult.singleAccountSelected(accountByPaymentMethodEntry);
        }

        List<Account<?, ?>> accountsRequiringSelection = selection.accountsRequiringSelection();
        if (!accountsRequiringSelection.isEmpty()) {
            return PaymentMethodSelectionResult.accountSelectionRequired(accountsRequiringSelection);
        }

        return PaymentMethodSelectionResult.noAccountAvailable();
    }

    public void onAddAccountByPaymentMethodEntry(Map.Entry<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethodEntry) {
        checkNotNull(accountByPaymentMethodEntry, "accountByPaymentMethodEntry must not be null");
        synchronized (draftLock) {
            doAddAccountByPaymentMethodEntry(accountByPaymentMethodEntry);
        }
    }

    private void doAddAccountByPaymentMethodEntry(Map.Entry<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethodEntry) {
        PaymentMethod<?> paymentMethod = checkNotNull(accountByPaymentMethodEntry.getKey(), "paymentMethod must not be null");
        Account<?, ?> account = checkNotNull(accountByPaymentMethodEntry.getValue(), "account must not be null");
        checkArgument(account.getPaymentMethod().equals(paymentMethod),
                "PaymentMethod must be the same as in account");
        validateSelectedPaymentMethodLimit(paymentMethod);
        validateAccountEligibility(paymentMethod, account);
        model.addAccountByPaymentMethodEntry(accountByPaymentMethodEntry);
        methodAccountEntryListeners.forEach(listener -> listener.accept(accountByPaymentMethodEntry));
    }

    public void onDeselectPaymentMethod(PaymentMethod<?> paymentMethod) {
        checkNotNull(paymentMethod, "paymentMethod must not be null");
        synchronized (draftLock) {
            model.removeAccountByPaymentMethod(paymentMethod);
        }
    }


    public void addMethodAccountEntryListener(Consumer<Map.Entry<PaymentMethod<?>, Account<?, ?>>> listener) {
        checkNotNull(listener, "listener must not be null");
        methodAccountEntryListeners.add(listener);
    }

    public void removeMethodAccountEntryListener(Consumer<Map.Entry<PaymentMethod<?>, Account<?, ?>>> listener) {
        checkNotNull(listener, "listener must not be null");
        methodAccountEntryListeners.remove(listener);
    }


    /* --------------------------------------------------------------------- */
    // Account helpers
    /* --------------------------------------------------------------------- */

    private static MarketAccounts loadAccountsForMarket(Market market, AccountsProvider accountsProvider) {
        List<Account<?, ?>> accountsForMarket = checkNotNull(accountsProvider.findAccountsForMarket(market),
                "accountsForMarket must not be null");
        Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = accountsForMarket.stream()
                .collect(Collectors.groupingBy(Account::getPaymentMethod, Collectors.toList()));
        return new MarketAccounts(accountsForMarket, accountsByPaymentMethod);
    }

    private static Optional<Account<?, ?>> findAccountToAutoSelect(List<Account<?, ?>> accountsForMarket,
                                                                   ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        if (accountsForMarket.size() != 1) {
            return Optional.empty();
        }

        Account<?, ?> account = accountsForMarket.getFirst();
        Account<?, ?> existing = selectedAccountByPaymentMethod.get(account.getPaymentMethod());
        return account.equals(existing) ? Optional.empty() : Optional.of(account);
    }

    private static PaymentMethodAccountSelection findAccountsSelection(Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod,
                                                                       PaymentMethod<?> paymentMethod) {
        List<Account<?, ?>> accountsForPaymentMethod = accountsByPaymentMethod.get(paymentMethod);
        if (accountsForPaymentMethod == null || accountsForPaymentMethod.isEmpty()) {
            return PaymentMethodAccountSelection.noAccount();
        }

        if (accountsForPaymentMethod.size() == 1) {
            return PaymentMethodAccountSelection.singleAccount(accountsForPaymentMethod.getFirst());
        }

        return PaymentMethodAccountSelection.multipleAccounts(accountsForPaymentMethod);
    }


    private static List<? extends PaymentMethod<?>> findSelectedPaymentMethodsToRemove(ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod,
                                                                                       List<Account<?, ?>> accountsForMarket) {
        return selectedAccountByPaymentMethod.entrySet().stream()
                .filter(entry -> !accountsForMarket.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void validateSelectedPaymentMethodLimit(PaymentMethod<?> paymentMethod) {
        ImmutableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = getAccountByPaymentMethod();
        boolean isExistingPaymentMethod = selectedAccountByPaymentMethod.containsKey(paymentMethod);
        checkArgument(isExistingPaymentMethod || selectedAccountByPaymentMethod.size() < MAX_NUM_PAYMENT_METHODS,
                MAX_PAYMENT_METHODS_REACHED);
    }

    private void validateAccountEligibility(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        List<Account<?, ?>> accountsForPaymentMethod = getAccountsByPaymentMethod().get(paymentMethod);
        checkArgument(accountsForPaymentMethod != null && accountsForPaymentMethod.contains(account),
                ACCOUNT_NOT_ELIGIBLE_FOR_MARKET);
    }
}
