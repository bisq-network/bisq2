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

package bisq.account.bisqeasy;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BisqEasyPaymentAccountService implements PersistenceClient<BisqEasyPaymentAccountStore>, Service {

    @Getter
    private final BisqEasyPaymentAccountStore persistableStore = new BisqEasyPaymentAccountStore();
    @Getter
    private final Persistence<BisqEasyPaymentAccountStore> persistence;

    @Getter
    private transient final ObservableSet<BisqEasyPaymentAccount> accounts = new ObservableSet<>();

    public BisqEasyPaymentAccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PersistenceClient
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPersistedApplied(BisqEasyPaymentAccountStore persisted) {
        accounts.setAll(new HashSet<>(getAccountByNameMap().values()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Map<String, BisqEasyPaymentAccount> getAccountByNameMap() {
        return persistableStore.bisqEasyPaymentAccountByName;
    }

    public boolean hasAccounts() {
        return !getAccountByNameMap().isEmpty();
    }

    public void addPaymentAccount(BisqEasyPaymentAccount account) {
        getAccountByNameMap().put(account.getName(), account);
        accounts.add(account);
        persist();
    }

    public void removePaymentAccount(BisqEasyPaymentAccount account) {
        getAccountByNameMap().remove(account.getName());
        accounts.remove(account);
        if (account.equals(getSelectedAccount())) {
            setSelectedAccount(null);
        }
        persist();
    }

    public Optional<BisqEasyPaymentAccount> findAccount(String name) {
        return Optional.ofNullable(getAccountByNameMap().get(name));
    }

    public Observable<BisqEasyPaymentAccount> selectedAccountAsObservable() {
        return persistableStore.selectedBisqEasyPaymentAccount;
    }

    @Nullable
    public BisqEasyPaymentAccount getSelectedAccount() {
        return persistableStore.selectedBisqEasyPaymentAccount.get();
    }

    public void setSelectedAccount(BisqEasyPaymentAccount account) {
        selectedAccountAsObservable().set(account);
        persist();
    }
}