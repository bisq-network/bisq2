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
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.SettlementMethod;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AccountService implements PersistenceClient<AccountStore> {
    private final AccountStore accountStore = new AccountStore();
    @Getter
    private final Persistence<AccountStore> persistence;

    public AccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, "db", accountStore);
    }

    @Override
    public void applyPersisted(AccountStore persisted) {
        synchronized (accountStore) {
            accountStore.applyPersisted(persisted);
        }
    }

    @Override
    public AccountStore getClone() {
        synchronized (accountStore) {
            return accountStore.getClone();
        }
    }

    public void addAccount(Account<? extends SettlementMethod> account) {
        List<Account<? extends SettlementMethod>> accounts = accountStore.getAccounts();
        if (accounts.contains(account)) return;
        accounts.add(account);
        persist();
    }

    public List<Account<? extends SettlementMethod>> getAccounts() {
        return accountStore.getAccounts();
    }

    public List<Account<? extends SettlementMethod>> getMatchingAccounts(SwapProtocolType protocolTyp,
                                                                         String currencyCode) {
        var settlementMethods = new HashSet<>(SettlementMethod.from(protocolTyp, currencyCode));
        return accountStore.getAccounts().stream()
                .filter(account -> settlementMethods.contains(account.getSettlementMethod()))
                .filter(account -> account.getTradeCurrencyCodes().contains(currencyCode))
                .collect(Collectors.toList());
    }
}
