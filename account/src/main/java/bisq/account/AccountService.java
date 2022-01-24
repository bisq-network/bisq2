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
import bisq.account.protocol.ProtocolSwapSettlementMapping;
import bisq.account.protocol.SwapProtocolType;
import bisq.account.settlement.Settlement;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void addAccount(Account<? extends Settlement.Method> account) {
        List<Account<? extends Settlement.Method>> accounts = accountStore.getAccounts();
        if (accounts.contains(account)) return;
        accounts.add(account);
        persist();
    }

    public List<Account<? extends Settlement.Method>> getAccounts() {
        return accountStore.getAccounts();
    }

    public List<Account<? extends Settlement.Method>> getMatchingAccounts(SwapProtocolType protocolTyp, String baseCurrencyCode) {
        Set<? extends Settlement.Method> settlementMethods = ProtocolSwapSettlementMapping.getSettlementMethods(protocolTyp,
                baseCurrencyCode);
       return accountStore.getAccounts().stream().filter(account -> settlementMethods.contains(account.getSettlementMethod())).collect(Collectors.toList());
    }
}
