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
import bisq.account.bisq1_import.ImportBisq1AccountService;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.timestamp.AccountTimestampService;
import bisq.bonded_roles.BondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.network.NetworkService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.user.UserService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class AccountService extends RateLimitedPersistenceClient<AccountStore> implements Service {

    private final AccountStore persistableStore = new AccountStore();
    private final Persistence<AccountStore> persistence;
    private final NetworkService networkService;
    private final ImportBisq1AccountService importBisq1AccountService;
    private final AccountTimestampService accountTimestampService;

    public AccountService(PersistenceService persistenceService,
                          NetworkService networkService,
                          UserService userService,
                          BondedRolesService bondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        importBisq1AccountService = new ImportBisq1AccountService();
        accountTimestampService = new AccountTimestampService(networkService, userService, bondedRolesService);
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */
    @Override
    public CompletableFuture<Boolean> initialize() {
        return accountTimestampService.initialize()
                .thenApply(result -> {
                    persistableStore.getAccountByName().addObserver(new HashMapObserver<String, Account<? extends PaymentMethod<?>, ?>>() {
                        @Override
                        public void put(String key, Account<? extends PaymentMethod<?>, ?> account) {
                            try {
                                accountTimestampService.handleAddedAccount(account);
                            } catch (Exception e) {
                                log.error("handleAddedAccount failed", e);
                            }
                        }
                    });
                    return result;
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return accountTimestampService.shutdown();
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public boolean addPaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
        var previous = persistableStore.getAccountByName().putIfAbsent(account.getAccountName(), account);
        if (previous == null) {
            persist();
            return true;
        } else {
            log.warn("There is already an entry with key {}. We ignore the addPaymentAccount call.", account.getAccountName());
            return false;
        }
    }

    public void removePaymentAccount(Account<? extends PaymentMethod<?>, ?> account) {
        persistableStore.getAccountByName().remove(account.getAccountName());
        findSelectedAccount().ifPresent(a -> {
            if (a.equals(account)) {
                setSelectedAccount(null);
            }
        });
        persist();
    }

    public void importBisq1AccountData(String json) {
        importBisq1AccountService.parseAccounts(json)
                .forEach(this::addPaymentAccount);
    }

    public void setSelectedAccount(@Nullable Account<? extends PaymentMethod<?>, ?> account) {
        if (persistableStore.getSelectedAccount().set(account)) {
            persist();
        }
    }


    /* --------------------------------------------------------------------- */
    // Getters
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservableMap<String, Account<? extends PaymentMethod<?>, ?>> getAccountByNameMap() {
        return persistableStore.getAccountByName();
    }

    public Collection<Account<? extends PaymentMethod<?>, ?>> getAccounts() {
        return Set.copyOf(getAccountByNameMap().values());
    }

    public Set<Account<? extends PaymentMethod<?>, ?>> getAccounts(PaymentMethod<?> paymentMethod) {
        return getAccountByNameMap().values().stream()
                .filter(account -> account.getPaymentMethod().equals(paymentMethod))
                .collect(Collectors.toSet());
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(AccountPayload<?> accountPayload) {
        return getAccountByNameMap().values().stream()
                .filter(account -> account.getAccountPayload().equals(accountPayload))
                .findAny();
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> findAccount(String name) {
        return Optional.ofNullable(getAccountByNameMap().get(name));
    }


    public ReadOnlyObservable<Account<? extends PaymentMethod<?>, ?>> selectedAccountAsObservable() {
        return persistableStore.getSelectedAccount();
    }

    public Optional<Account<? extends PaymentMethod<?>, ?>> findSelectedAccount() {
        return Optional.ofNullable(selectedAccountAsObservable().get());
    }

    public boolean hasAccounts() {
        return !hasNoAccounts();
    }

    public boolean hasNoAccounts() {
        return persistableStore.getAccountByName().isEmpty();
    }



    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

}
