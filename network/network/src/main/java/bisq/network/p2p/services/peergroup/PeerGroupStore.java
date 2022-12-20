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

package bisq.network.p2p.services.peergroup;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public final class PeerGroupStore implements PersistableStore<PeerGroupStore> {
    @Getter
    private final Set<Peer> persistedPeers = new CopyOnWriteArraySet<>();

    public PeerGroupStore() {
    }

    private PeerGroupStore(Set<Peer> persistedPeers) {
        this.persistedPeers.addAll(persistedPeers);
    }

    @Override
    public bisq.network.protobuf.PeerGroupStore toProto() {
        return bisq.network.protobuf.PeerGroupStore.newBuilder().addAllPersistedPeers(persistedPeers.stream()
                        .map(Peer::toProto)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static PeerGroupStore fromProto(bisq.network.protobuf.PeerGroupStore proto) {
        return new PeerGroupStore(proto.getPersistedPeersList().stream()
                .map(Peer::fromProto).collect(Collectors.toSet()));
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
        return new PeerGroupStore(persistedPeers);
    }

    @Override
    public void applyPersisted(PeerGroupStore persisted) {
        persistedPeers.clear();
        persistedPeers.addAll(persisted.getPersistedPeers());
    }
}