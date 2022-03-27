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

package bisq.wallets.stores;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class WalletStore implements PersistableStore<WalletStore> {
    @Getter
    private final ObservableSet<String> receiveAddresses = new ObservableSet<>();

    public WalletStore() {
    }

    private WalletStore(Set<String> receiveAddresses) {
        this.receiveAddresses.addAll(receiveAddresses);
    }

    @Override
    public bisq.wallets.protobuf.WalletStore toProto() {
        return bisq.wallets.protobuf.WalletStore.newBuilder()
                .addAllReceiveAddresses(receiveAddresses)
                .build();
    }

    public static WalletStore fromProto(bisq.wallets.protobuf.WalletStore proto) {
        return new WalletStore(new HashSet<>(proto.getReceiveAddressesList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.wallets.protobuf.WalletStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public WalletStore getClone() {
        return new WalletStore(receiveAddresses);
    }

    @Override
    public void applyPersisted(WalletStore persisted) {
        receiveAddresses.clear();
        receiveAddresses.addAll(persisted.getReceiveAddresses());
    }
}
