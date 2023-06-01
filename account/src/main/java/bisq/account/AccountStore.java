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
import bisq.account.settlement.Settlement;
import bisq.common.observable.Observable;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class AccountStore implements PersistableStore<AccountStore> {
    private final Map<String, Account<?, ? extends Settlement<?>>> accountByName = new ConcurrentHashMap<>();
    private final Observable<Account<?, ? extends Settlement<?>>> selectedAccount = new Observable<>();

    public AccountStore() {
        this(new HashMap<>(), Optional.empty());
    }

    public AccountStore(Map<String, Account<?, ? extends Settlement<?>>> accountByName,
                        Optional<Account<?, ? extends Settlement<?>>> selectedAccount) {
        this.accountByName.putAll(accountByName);
        this.selectedAccount.set(selectedAccount.orElse(null));
    }

    @Override
    public bisq.account.protobuf.AccountStore toProto() {
        bisq.account.protobuf.AccountStore.Builder builder = bisq.account.protobuf.AccountStore.newBuilder()
                .putAllAccountByName(accountByName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().toProto())));
        Optional.ofNullable(selectedAccount.get()).ifPresent(e -> builder.setSelectedAccount(e.toProto()));
        return builder.build();
    }

    public static AccountStore fromProto(bisq.account.protobuf.AccountStore proto) {
        return new AccountStore(
                proto.getAccountByNameMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> Account.fromProto(e.getValue()))),
                proto.hasSelectedAccount() ?
                        Optional.of(Account.fromProto(proto.getSelectedAccount())) :
                        Optional.empty());
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
        return new AccountStore(accountByName, Optional.ofNullable(selectedAccount.get()));
    }

    @Override
    public void applyPersisted(AccountStore persisted) {
        accountByName.clear();
        accountByName.putAll(persisted.accountByName);
        selectedAccount.set(persisted.selectedAccount.get());
    }

    Map<String, Account<?, ? extends Settlement<?>>> getAccountByName() {
        return accountByName;
    }

    Observable<Account<?, ? extends Settlement<?>>> getSelectedAccount() {
        return selectedAccount;
    }
}