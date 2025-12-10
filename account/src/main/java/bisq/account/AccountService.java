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
import bisq.account.accounts.AccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class AccountService extends RateLimitedPersistenceClient<AccountStore> implements Service {

    private final AccountStore persistableStore = new AccountStore();
    private final Persistence<AccountStore> persistence;
    private final transient ObservableSet<Account<? extends PaymentMethod<?>, ?>> accounts = new ObservableSet<>();

    public AccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> shutdown() {
        accounts.clear();
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Collection<Account<? extends PaymentMethod<?>, ?>> getAccounts() {
        return getAccountByNameMap().values();
    }

    public ObservableHashMap<String, Account<? extends PaymentMethod<?>, ?>> getAccountByNameMap() {
        return persistableStore.getAccountByName();
    }

    public boolean hasAccounts() {
        return !getAccountByNameMap().isEmpty();
    }

    public void addPaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
        getAccountByNameMap().put(account.getAccountName(), account);
        persist();
    }

    public void removePaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
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

    /**
     * Updates an existing payment account by first removing the old account and then adding the updated one.
     * This allows for updating account details including renaming the account.
     *
     * @param existingAccountName The current name of the account to be updated. This name is used to identify
     *                            and remove the account before adding the updated version.
     * @param updatedAccount      The updated account object containing new account data, which may include a new account name.
     * @throws IllegalArgumentException if the new account name already exists (when renaming) or if the existing account is not found.
     */
    public void updatePaymentAccount(String existingAccountName,
                                     Account<? extends PaymentMethod<?>, ?> updatedAccount) {
        Map<String, Account<? extends PaymentMethod<?>, ?>> accountByNameMap = getAccountByNameMap();
        String updatedAccountName = updatedAccount.getAccountName();

        if (!existingAccountName.equals(updatedAccountName) && accountByNameMap.containsKey(updatedAccountName)) {
            throw new IllegalArgumentException("The account name '" + updatedAccountName + "' already exists. Please choose a different name.");
        }

        if (accountByNameMap.remove(existingAccountName) == null) {
            throw new IllegalArgumentException("Account not found: " + existingAccountName);
        }

        // Update in-memory state atomically
        accountByNameMap.put(updatedAccountName, updatedAccount);

        // Update selected account reference if needed
        getSelectedAccount().ifPresent(selected -> {
            String selectedAccountName = selected.getAccountName();
            if (selectedAccountName.equals(existingAccountName)) {
                selectedAccountAsObservable().set(updatedAccount);
            }
        });

        persist();
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(String name) {
        return Optional.ofNullable(getAccountByNameMap().get(name));
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(AccountPayload<?> accountPayload) {
        return getAccountByNameMap().values().stream()
                .filter(account -> account.getAccountPayload().equals(accountPayload))
                .findAny();
    }

    public Observable<Account<? extends PaymentMethod<?>, ?>> selectedAccountAsObservable() {
        return persistableStore.getSelectedAccount();
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> getSelectedAccount() {
        return Optional.ofNullable(selectedAccountAsObservable().get());
    }

    //todo do we need that?
    public void setSelectedAccount(Account<? extends PaymentMethod<?>, ?> account) {
        if (selectedAccountAsObservable().set(account)) {
            persist();
        }
    }

    public Set<Account<? extends PaymentMethod<?>, ?>> getAccounts(PaymentMethod<?> paymentMethod) {
        return accounts.stream()
                .filter(account -> account.getPaymentMethod().equals(paymentMethod))
                .collect(Collectors.toSet());
    }
}
