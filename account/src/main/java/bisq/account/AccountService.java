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


import bisq.account.settlement.Account;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;

import java.util.List;

public class AccountService implements PersistenceClient<AccountModel> {

    private final AccountModel accountModel = new AccountModel();

    @Getter
    private final Persistence<AccountModel> persistence;

    public AccountService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, "db", accountModel);
    }

    @Override
    public void applyPersisted(AccountModel persisted) {
        synchronized (accountModel) {
            accountModel.applyPersisted(persisted);
        }
    }

    @Override
    public AccountModel getClone() {
        synchronized (accountModel) {
            return accountModel.getClone();
        }
    }

    public void addAccount(Account account) {
        List<Account> accounts = accountModel.getAccounts();
        if(accounts.contains(account)) return;
        accounts.add(account);
        persist();
    }
}
