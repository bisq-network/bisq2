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


import bisq.account.accountage.AccountAgeWitnessService;
import bisq.account.accounts.Account;
import bisq.account.accounts.RevolutAccount;
import bisq.account.accounts.SepaAccount;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.common.locale.CountryRepository;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class AccountService implements PersistenceClient<AccountStore> {
    @Getter
    private final AccountStore persistableStore = new AccountStore();
    @Getter
    private final Persistence<AccountStore> persistence;
    private final AccountAgeWitnessService accountAgeWitnessService;

    public AccountService(NetworkService networkService, PersistenceService persistenceService, IdentityService identityService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        accountAgeWitnessService = new AccountAgeWitnessService(networkService, identityService);
    }

    public void addAccount(Account<? extends SettlementMethod> account) {
        List<Account<? extends SettlementMethod>> accounts = persistableStore.getAccounts();
        if (accounts.contains(account)) return;
        accounts.add(account);
        persist();
    }

    public List<Account<? extends SettlementMethod>> getAccounts() {
        return persistableStore.getAccounts();
    }

    public List<Account<? extends SettlementMethod>> getMatchingAccounts(SwapProtocolType protocolTyp,
                                                                         String currencyCode) {
        var settlementMethods = new HashSet<>(SettlementMethod.from(protocolTyp, currencyCode));
        return persistableStore.getAccounts().stream()
                .filter(account -> settlementMethods.contains(account.getSettlementMethod()))
                .filter(account -> account.getTradeCurrencyCodes().contains(currencyCode))
                .collect(Collectors.toList());
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        addDummyAccounts();
        return accountAgeWitnessService.initialize();
    }

    private void addDummyAccounts() {
        log.info("add dummy accounts");
        if (getAccounts().isEmpty()) {
            SepaAccount john_smith = new SepaAccount("SEPA-account-1",
                    "John Smith",
                    "iban_1234",
                    "bic_1234",
                    CountryRepository.getDefaultCountry());
            addAccount(john_smith);
            addAccount(new SepaAccount("SEPA-account-2",
                    "Mary Smith",
                    "iban_5678",
                    "bic_5678",
                    CountryRepository.getDefaultCountry()));
            addAccount(new RevolutAccount("revolut-account", "john@gmail.com"));
        }
    }
}
