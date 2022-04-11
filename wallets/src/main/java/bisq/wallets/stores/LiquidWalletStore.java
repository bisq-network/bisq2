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

import java.util.HashSet;
import java.util.Set;

public class LiquidWalletStore implements PersistableStore<LiquidWalletStore> {
    @Getter
    private final ObservableSet<String> receiveAddresses = new ObservableSet<>();

    public LiquidWalletStore() {
    }

    private LiquidWalletStore(Set<String> receiveAddresses) {
        this.receiveAddresses.addAll(receiveAddresses);
    }

    @Override
    public bisq.wallets.protobuf.LiquidWalletStore toProto() {
        return bisq.wallets.protobuf.LiquidWalletStore.newBuilder()
                .addAllReceiveAddresses(receiveAddresses)
                .build();
    }

    public static LiquidWalletStore fromProto(bisq.wallets.protobuf.LiquidWalletStore proto) {
        return new LiquidWalletStore(new HashSet<>(proto.getReceiveAddressesList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.wallets.protobuf.LiquidWalletStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public LiquidWalletStore getClone() {
        return new LiquidWalletStore(receiveAddresses);
    }

    @Override
    public void applyPersisted(LiquidWalletStore persisted) {
        receiveAddresses.clear();
        receiveAddresses.addAll(persisted.getReceiveAddresses());
    }
}
