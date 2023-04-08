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

package bisq.wallets.bitcoind;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.wallets.json_rpc.RpcConfig;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public final class BitcoinWalletStore implements PersistableStore<BitcoinWalletStore> {
    @Getter
    @Setter
    private Optional<RpcConfig> rpcConfig = Optional.empty();
    @Getter
    private final ObservableSet<String> receiveAddresses = new ObservableSet<>();

    public BitcoinWalletStore() {
    }

    private BitcoinWalletStore(Optional<RpcConfig> rpcConfig, Set<String> receiveAddresses) {
        this.rpcConfig = rpcConfig;
        this.receiveAddresses.addAll(receiveAddresses);
    }

    @Override
    public bisq.wallets.protobuf.BitcoinWalletStore toProto() {
        bisq.wallets.protobuf.BitcoinWalletStore.Builder builder =
                bisq.wallets.protobuf.BitcoinWalletStore.newBuilder()
                        .addAllReceiveAddresses(receiveAddresses);

        rpcConfig.ifPresent(config -> builder.setRpcConfig(config.toProto()));
        return builder.build();
    }

    public static BitcoinWalletStore fromProto(bisq.wallets.protobuf.BitcoinWalletStore proto) {
        Optional<RpcConfig> rpcConfig = proto.hasRpcConfig() ? Optional.of(RpcConfig.fromProto(proto.getRpcConfig()))
                : Optional.empty();
        return new BitcoinWalletStore(rpcConfig, new HashSet<>(proto.getReceiveAddressesList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.wallets.protobuf.BitcoinWalletStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public BitcoinWalletStore getClone() {
        return new BitcoinWalletStore(rpcConfig, receiveAddresses);
    }

    @Override
    public void applyPersisted(BitcoinWalletStore persisted) {
        rpcConfig = persisted.rpcConfig;
        receiveAddresses.clear();
        receiveAddresses.addAll(persisted.getReceiveAddresses());
    }
}
