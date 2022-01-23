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
import bisq.persistence.Persistable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@EqualsAndHashCode
@ToString
public class AccountModel implements Persistable<AccountModel> {
    private final List<Account> accounts = new CopyOnWriteArrayList<>();

    public AccountModel() {
    }

    private AccountModel(List<Account> accounts) {
        this.accounts.addAll(accounts);
    }

    @Override
    public AccountModel getClone() {
        return new AccountModel(accounts);
    }

    @Override
    public void applyPersisted(AccountModel persisted) {
        accounts.clear();
        accounts.addAll(persisted.accounts);
    }
}