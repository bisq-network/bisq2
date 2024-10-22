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

package bisq.network.p2p.services.peer_group;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.network.Address;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public final class PeerGroupStore implements PersistableStore<PeerGroupStore> {
    private final Map<Address, Peer> persistedPeersByAddress = new ConcurrentHashMap<>();

    public PeerGroupStore() {
    }

    private PeerGroupStore(Map<Address, Peer> persistedPeersByAddress) {
        this.persistedPeersByAddress.putAll(persistedPeersByAddress);
    }

    @Override
    public bisq.network.protobuf.PeerGroupStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.PeerGroupStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.PeerGroupStore.newBuilder().addAllPersistedPeers(persistedPeersByAddress.values().stream()
                .map(peer -> peer.toProto(serializeForHash))
                .collect(Collectors.toSet()));
    }

    public static PeerGroupStore fromProto(bisq.network.protobuf.PeerGroupStore proto) {
        Map<Address, Peer> persistedPeersById = proto.getPersistedPeersList().stream()
                .map(Peer::fromProto)
                .collect(Collectors.toMap(Peer::getAddress, e -> e));
        return new PeerGroupStore(persistedPeersById);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.PeerGroupStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public PeerGroupStore getClone() {
        return new PeerGroupStore(new HashMap<>(persistedPeersByAddress));
    }

    @Override
    public void applyPersisted(PeerGroupStore persisted) {
        persistedPeersByAddress.clear();
        persistedPeersByAddress.putAll(persisted.getPersistedPeersByAddress());
    }

    Map<Address, Peer> getPersistedPeersByAddress() {
        return persistedPeersByAddress;
    }
}