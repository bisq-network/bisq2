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

package bisq.wallet;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class WalletStore implements PersistableStore<WalletStore> {
    private boolean walletExists;

    private WalletStore(boolean walletExists) {
        this.walletExists = walletExists;
    }

    @Override
    public synchronized bisq.wallet.protobuf.WalletStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.wallet.protobuf.WalletStore.newBuilder().setWalletExists(walletExists);
    }

    @Override
    public bisq.wallet.protobuf.WalletStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static WalletStore fromProto(bisq.wallet.protobuf.WalletStore proto) {
        return new WalletStore(proto.getWalletExists());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.wallet.protobuf.WalletStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public synchronized WalletStore getClone() {
        return new WalletStore(walletExists);
    }

    @Override
    public synchronized void applyPersisted(WalletStore persisted) {
        walletExists = persisted.isWalletExists();
    }

    synchronized boolean isWalletExists() {
        return walletExists;
    }

    synchronized boolean setWalletExists() {
        boolean changed = !walletExists;
        walletExists = true;
        return changed;
    }
}
