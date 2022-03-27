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
import bisq.account.settlement.SettlementMethod;
import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@EqualsAndHashCode
@ToString
public class AccountStore implements PersistableStore<AccountStore> {
    private final List<Account<? extends SettlementMethod>> accounts = new CopyOnWriteArrayList<>();

    public AccountStore() {
    }

    private AccountStore(List<Account<? extends SettlementMethod>> accounts) {
        this.accounts.addAll(accounts);
    }

    @Override
    public AccountStore getClone() {
        return new AccountStore(accounts);
    }

    @Override
    public void applyPersisted(AccountStore persisted) {
        accounts.clear();
        accounts.addAll(persisted.accounts);
    }

    @Override
    public Message toProto() {
        return null;
    }
}