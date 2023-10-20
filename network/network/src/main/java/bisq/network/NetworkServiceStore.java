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
import bisq.network.p2p.node.AddressByTransportTypeMap;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
@Getter
public final class NetworkServiceStore implements PersistableStore<NetworkServiceStore> {
    private final Map<String, NetworkId> networkIdByNodeId = new ConcurrentHashMap<>();
    private final Set<AddressByTransportTypeMap> seedNodes = new CopyOnWriteArraySet<>();

    public NetworkServiceStore() {
    }

    public NetworkServiceStore(Map<String, NetworkId> networkIdByNodeId,
                               Set<AddressByTransportTypeMap> seedNodes) {
        this.networkIdByNodeId.putAll(networkIdByNodeId);
        this.seedNodes.addAll(seedNodes);
    }

    @Override
    public bisq.network.protobuf.NetworkServiceStore toProto() {
        return bisq.network.protobuf.NetworkServiceStore.newBuilder()
                .putAllNetworkIdByNodeId(networkIdByNodeId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toProto())))
                .addAllSeedNodes(seedNodes.stream()
                        .map(AddressByTransportTypeMap::toProto)
                        .collect(Collectors.toList()))
                .build();
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.NetworkServiceStore proto) {
        Map<String, NetworkId> networkIdByNodeId = proto.getNetworkIdByNodeIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> NetworkId.fromProto(e.getValue())));
        return new NetworkServiceStore(networkIdByNodeId,
                proto.getSeedNodesList().stream()
                        .map(AddressByTransportTypeMap::fromProto)
                        .collect(Collectors.toSet()));
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
        networkIdByNodeId.clear();
        networkIdByNodeId.putAll(persisted.getNetworkIdByNodeId());
        seedNodes.clear();
        seedNodes.addAll(persisted.getSeedNodes());
    }

    @Override
    public NetworkServiceStore getClone() {
        return new NetworkServiceStore(networkIdByNodeId, seedNodes);
    }
}