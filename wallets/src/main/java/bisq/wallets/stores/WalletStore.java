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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

public class WalletStore implements PersistableStore<WalletStore> {

    @Getter
    private BitcoinWalletStore bitcoinWalletStore = new BitcoinWalletStore();
    @Getter
    private LiquidWalletStore liquidWalletStore = new LiquidWalletStore();

    public WalletStore() {
    }

    public WalletStore(BitcoinWalletStore bitcoinWalletStore, LiquidWalletStore liquidWalletStore) {
        this.bitcoinWalletStore = bitcoinWalletStore;
        this.liquidWalletStore = liquidWalletStore;
    }

    @Override
    public bisq.wallets.protobuf.WalletStore toProto() {
        return bisq.wallets.protobuf.WalletStore.newBuilder()
                .setBitcoinWalletStore(bitcoinWalletStore.toProto())
                .setLiquidWalletStore(liquidWalletStore.toProto())
                .build();
    }

    public static WalletStore fromProto(bisq.wallets.protobuf.WalletStore proto) {
        return new WalletStore(BitcoinWalletStore.fromProto(proto.getBitcoinWalletStore()),
                LiquidWalletStore.fromProto(proto.getLiquidWalletStore())
        );
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
        return new WalletStore(bitcoinWalletStore, liquidWalletStore);
    }

    @Override
    public void applyPersisted(WalletStore persisted) {
        bitcoinWalletStore = persisted.bitcoinWalletStore;
        liquidWalletStore = persisted.liquidWalletStore;
    }
}
