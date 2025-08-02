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
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class AccountService implements PersistenceClient<AccountStore>, Service {

    private final AccountStore persistableStore = new AccountStore();
    private final Persistence<AccountStore> persistence;
    private final transient ObservableSet<Account<? extends PaymentMethod<?>, ?>> accounts = new ObservableSet<>();

    public AccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    @Override
    public void onPersistedApplied(AccountStore persisted) {
        accounts.setAll(persisted.getAccountByName().values());
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

    public Map<String, Account<? extends PaymentMethod<?>, ?>> getAccountByNameMap() {
        return persistableStore.getAccountByName();
    }

    public boolean hasAccounts() {
        return !getAccountByNameMap().isEmpty();
    }

    public void addPaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
        getAccountByNameMap().put(account.getAccountName(), account);
        accounts.add(account);
        persist();
    }

    public void removePaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
        getAccountByNameMap().remove(account.getAccountName());
        accounts.remove(account);
        getSelectedAccount().ifPresent(s -> {
            if (s.equals(account)) {
                setSelectedAccount(null);
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
        selectedAccountAsObservable().set(account);
        persist();
    }

    public Set<Account<? extends PaymentMethod<?>, ?>> getAccounts(PaymentMethod<?> paymentMethod) {
        return accounts.stream()
                .filter(account -> account.getPaymentMethod().equals(paymentMethod))
                .collect(Collectors.toSet());
    }
}
