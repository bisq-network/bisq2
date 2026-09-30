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

package bisq.wallet.receive_address;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public final class ReceiveAddressStore implements PersistableStore<ReceiveAddressStore> {
        private final ObservableSet<ReceiveAddressEntry> receiveAddressEntries = new ObservableSet<>();
        
        private ReceiveAddressStore(Set<ReceiveAddressEntry> receiveAddressEntries) {
            this.receiveAddressEntries.setAll(receiveAddressEntries);
        }

    @Override
    public synchronized bisq.wallet.protobuf.ReceiveAddressStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.wallet.protobuf.ReceiveAddressStore.newBuilder()
                .addAllReceiveAddressEntries(receiveAddressEntries.stream()
                        .sorted(Comparator.comparing(ReceiveAddressEntry::getAddress))
                        .map(e -> e.toProto(serializeForHash))
                        .toList());
    }

    @Override
    public bisq.wallet.protobuf.ReceiveAddressStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static ReceiveAddressStore fromProto(bisq.wallet.protobuf.ReceiveAddressStore proto) {
        return new ReceiveAddressStore(new HashSet<>(proto.getReceiveAddressEntriesList().stream()
                .map(ReceiveAddressEntry::fromProto)
                .collect(Collectors.toSet())));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.wallet.protobuf.ReceiveAddressStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public synchronized ReceiveAddressStore getClone() {
        return new ReceiveAddressStore(Set.copyOf(receiveAddressEntries));
    }

    @Override
    public synchronized void applyPersisted(ReceiveAddressStore persisted) {
        receiveAddressEntries.setAll(persisted.getReceiveAddressEntries());
    }

    synchronized boolean addReceiveAddressEntry(ReceiveAddressEntry receiveAddressEntry) {
        boolean didNotExist = receiveAddressEntries.stream()
                .noneMatch(e -> e.getAddress().equals(receiveAddressEntry.getAddress()));
        if (didNotExist) {
            receiveAddressEntries.add(receiveAddressEntry);
        }
        return didNotExist;
    }

    synchronized boolean updateReceiveAddressEntry(ReceiveAddressEntry updated) {
        Optional<ReceiveAddressEntry> existing = receiveAddressEntries.stream()
                .filter(e -> e.getAddress().equals(updated.getAddress()))
                .findFirst();
        boolean receiveAddressExists = existing.isPresent();
        if (receiveAddressExists) {
            receiveAddressEntries.remove(existing.get());
            receiveAddressEntries.add(updated);
        }
        return receiveAddressExists;
    }

    ReadOnlyObservableSet<ReceiveAddressEntry> getReceiveAddressEntries() {
        return receiveAddressEntries;
    }
}
