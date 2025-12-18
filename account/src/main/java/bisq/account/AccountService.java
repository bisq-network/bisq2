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

package bisq.account;


import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodUtil;
import bisq.account.payment_method.PaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class AccountService extends RateLimitedPersistenceClient<AccountStore> implements Service {

    private final AccountStore persistableStore = new AccountStore();
    private final Persistence<AccountStore> persistence;

    public AccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Collection<Account<?, ? extends PaymentMethod<?>>> getAccounts() {
        return getAccountByNameMap().values();
    }

    public ObservableHashMap<String, Account<?, ? extends PaymentMethod<?>>> getAccountByNameMap() {
        return persistableStore.getAccountByName();
    }

    public boolean hasAccounts() {
        return !getAccountByNameMap().isEmpty();
    }

    public void addPaymentAccount(Account<?, ? extends PaymentMethod<?>> account) {
        getAccountByNameMap().put(account.getAccountName(), account);
        persist();
    }

    public void removePaymentAccount(Account<?, ? extends PaymentMethod<?>> account) {
        getAccountByNameMap().remove(account.getAccountName());
        getSelectedAccount().ifPresent(selected -> {
            String selectedAccountName = selected.getAccountName();
            String accountName = account.getAccountName();
            if (selectedAccountName.equals(accountName)) {
                setSelectedAccount(null);
            }
        });
        persist();
    }

    public void updatePaymentAccount(String accountName,
                                     Account<?, ? extends PaymentMethod<?>> updatedAccount) {
        Map<String, Account<?, ? extends PaymentMethod<?>>> accountByNameMap = getAccountByNameMap();
        String updatedAccountName = updatedAccount.getAccountName();

        if (!accountName.equals(updatedAccountName) && accountByNameMap.containsKey(updatedAccountName)) {
            throw new IllegalArgumentException("Account name already exists: " + updatedAccountName);
        }

        if (accountByNameMap.remove(accountName) == null) {
            throw new IllegalArgumentException("Account not found: " + accountName);
        }

        // Update in-memory state atomically
        accountByNameMap.put(updatedAccount.getAccountName(), updatedAccount);

        // Update selected account reference if needed
        getSelectedAccount().ifPresent(selected -> {
            String selectedAccountName = selected.getAccountName();
            if (selectedAccountName.equals(accountName)) {
                selectedAccountAsObservable().set(updatedAccount);
            }
        });

        persist();
    }

    public Optional<Account<?, ? extends PaymentMethod<?>>> findAccount(String name) {
        return Optional.ofNullable(getAccountByNameMap().get(name));
    }

    public Observable<Account<?, ? extends PaymentMethod<?>>> selectedAccountAsObservable() {
        return persistableStore.getSelectedAccount();
    }

    public Optional<Account<?, ? extends PaymentMethod<?>>> getSelectedAccount() {
        return Optional.ofNullable(selectedAccountAsObservable().get());
    }

    public void setSelectedAccount(Account<?, ? extends PaymentMethod<?>> account) {
        selectedAccountAsObservable().set(account);
        persist();
    }

    public List<Account<?, ? extends PaymentMethod<?>>> getMatchingAccounts(TradeProtocolType protocolTyp,
                                                                            String currencyCode) {
        Set<? extends PaymentRail> paymentMethods =
                new HashSet<>(PaymentMethodUtil.getPaymentRails(protocolTyp, currencyCode));
        return persistableStore.getAccountByName().values().stream()
                .filter(account -> paymentMethods.contains(account.getPaymentMethod().getPaymentRail()))
                .filter(account -> account.getTradeCurrencyCodes().contains(currencyCode))
                .collect(Collectors.toList());
    }
}
