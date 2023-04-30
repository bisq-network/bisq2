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

package bisq.wallets.elementsd;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.wallets.json_rpc.RpcConfig;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LiquidWalletStore implements PersistableStore<LiquidWalletStore> {
    @Getter
    @Setter
    private Optional<RpcConfig> rpcConfig = Optional.empty();
    @Getter
    private final ObservableArray<String> walletAddresses = new ObservableArray<>();

    public LiquidWalletStore() {
    }

    private LiquidWalletStore(Optional<RpcConfig> rpcConfig, List<String> walletAddresses) {
        this.rpcConfig = rpcConfig;
        this.walletAddresses.addAll(walletAddresses);
    }

    @Override
    public bisq.wallets.protobuf.LiquidWalletStore toProto() {
        bisq.wallets.protobuf.LiquidWalletStore.Builder builder =
                bisq.wallets.protobuf.LiquidWalletStore.newBuilder()
                        .addAllWalletAddresses(walletAddresses);

        rpcConfig.ifPresent(config -> builder.setRpcConfig(config.toProto()));
        return builder.build();
    }

    public static LiquidWalletStore fromProto(bisq.wallets.protobuf.LiquidWalletStore proto) {
        Optional<RpcConfig> rpcConfig = proto.hasRpcConfig() ? Optional.of(RpcConfig.fromProto(proto.getRpcConfig()))
                : Optional.empty();
        return new LiquidWalletStore(rpcConfig, new ArrayList<>(proto.getWalletAddressesList()));
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
        return new LiquidWalletStore(rpcConfig, walletAddresses);
    }

    @Override
    public void applyPersisted(LiquidWalletStore persisted) {
        rpcConfig = persisted.rpcConfig;
        walletAddresses.clear();
        walletAddresses.addAll(persisted.getWalletAddresses());
    }
}
