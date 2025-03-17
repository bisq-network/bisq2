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

package bisq.network;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public final class NetworkServiceStore implements PersistableStore<NetworkServiceStore> {
    private final Set<AddressByTransportTypeMap> seedNodes = new CopyOnWriteArraySet<>();
    @Deprecated(since = "2.1.0") // Moved to NetworkIdStore
    private final Map<String, NetworkId> networkIdByTag = new ConcurrentHashMap<>();

    public NetworkServiceStore() {
    }

    public NetworkServiceStore(Set<AddressByTransportTypeMap> seedNodes, Map<String, NetworkId> networkIdByTag) {
        this.seedNodes.addAll(seedNodes);
        this.networkIdByTag.putAll(networkIdByTag);
    }

    @Override
    public bisq.network.protobuf.NetworkServiceStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.NetworkServiceStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.NetworkServiceStore.newBuilder()
                .addAllSeedNodes(seedNodes.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .putAllNetworkIdByTag(networkIdByTag.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))));
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.NetworkServiceStore proto) {
        Set<AddressByTransportTypeMap> seeds = proto.getSeedNodesList().stream()
                .map(AddressByTransportTypeMap::fromProto)
                .collect(Collectors.toSet());
        var networkIdByTag = proto.getNetworkIdByTagMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> NetworkId.fromProto(e.getValue())));
        return new NetworkServiceStore(seeds,
                networkIdByTag);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.NetworkServiceStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(NetworkServiceStore persisted) {
        seedNodes.clear();
        seedNodes.addAll(persisted.getSeedNodes());
        networkIdByTag.clear();
        networkIdByTag.putAll(persisted.getNetworkIdByTag());
    }

    @Override
    public NetworkServiceStore getClone() {
        return new NetworkServiceStore(new HashSet<>(seedNodes), new HashMap<>(networkIdByTag));
    }

    Set<AddressByTransportTypeMap> getSeedNodes() {
        return seedNodes;
    }

    @Deprecated(since = "2.1.0")
    Map<String, NetworkId> getNetworkIdByTag() {
        return networkIdByTag;
    }
}