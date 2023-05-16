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

import bisq.common.observable.Observable;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public final class BisqEasyPaymentAccountStore implements PersistableStore<BisqEasyPaymentAccountStore> {
    final Map<String, BisqEasyPaymentAccount> bisqEasyPaymentAccountByName = new HashMap<>();
    final Observable<BisqEasyPaymentAccount> selectedBisqEasyPaymentAccount = new Observable<>();

    public BisqEasyPaymentAccountStore() {
        this(new HashMap<>(),
                Optional.empty());
    }

    public BisqEasyPaymentAccountStore(Map<String, BisqEasyPaymentAccount> bisqEasyPaymentAccountByName,
                                       Optional<BisqEasyPaymentAccount> selectedBisqEasyPaymentAccount) {
        this.bisqEasyPaymentAccountByName.clear();
        this.bisqEasyPaymentAccountByName.putAll(bisqEasyPaymentAccountByName);
        this.selectedBisqEasyPaymentAccount.set(selectedBisqEasyPaymentAccount.orElse(null));
    }

    @Override
    public bisq.account.protobuf.BisqEasyPaymentAccountStore toProto() {
        bisq.account.protobuf.BisqEasyPaymentAccountStore.Builder builder = bisq.account.protobuf.BisqEasyPaymentAccountStore.newBuilder()
                .putAllBisqEasyPaymentAccountByName(bisqEasyPaymentAccountByName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().toProto())));
        Optional.ofNullable(selectedBisqEasyPaymentAccount.get()).ifPresent(e -> builder.setSelectedBisqEasyPaymentAccount(e.toProto()));
        return builder.build();
    }

    public static BisqEasyPaymentAccountStore fromProto(bisq.account.protobuf.BisqEasyPaymentAccountStore proto) {
        return new BisqEasyPaymentAccountStore(
                proto.getBisqEasyPaymentAccountByNameMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> BisqEasyPaymentAccount.fromProto(e.getValue()))),
                proto.hasSelectedBisqEasyPaymentAccount() ?
                        Optional.of(BisqEasyPaymentAccount.fromProto(proto.getSelectedBisqEasyPaymentAccount())) :
                        Optional.empty());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.account.protobuf.BisqEasyPaymentAccountStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BisqEasyPaymentAccountStore getClone() {
        return new BisqEasyPaymentAccountStore(
                bisqEasyPaymentAccountByName,
                Optional.ofNullable(selectedBisqEasyPaymentAccount.get()));
    }

    @Override
    public void applyPersisted(BisqEasyPaymentAccountStore persisted) {
        bisqEasyPaymentAccountByName.clear();
        bisqEasyPaymentAccountByName.putAll(persisted.bisqEasyPaymentAccountByName);
        selectedBisqEasyPaymentAccount.set(persisted.selectedBisqEasyPaymentAccount.get());
    }
}