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
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode
@ToString
public final class AccountStore implements PersistableStore<AccountStore> {
    private final List<Account<? extends SettlementMethod>> accounts = new CopyOnWriteArrayList<>();

    public AccountStore() {
    }

    private AccountStore(List<Account<? extends SettlementMethod>> accounts) {
        this.accounts.addAll(accounts);
    }

    @Override
    public bisq.account.protobuf.AccountStore toProto() {
        return bisq.account.protobuf.AccountStore.newBuilder()
                .addAllAccounts(accounts.stream().map(Account::toProto).collect(Collectors.toSet()))
                .build();
    }

    public static PersistableStore<?> fromProto(bisq.account.protobuf.AccountStore proto) {
        return new AccountStore(proto.getAccountsList().stream()
                .map(Account::fromProto)
                .collect(Collectors.toList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.account.protobuf.AccountStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
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
}