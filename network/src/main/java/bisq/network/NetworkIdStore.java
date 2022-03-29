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
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class NetworkIdStore implements PersistableStore<NetworkIdStore> {
    @Getter
    private final Map<String, NetworkId> networkIdByNodeId = new ConcurrentHashMap<>();

    public NetworkIdStore() {
    }

    public NetworkIdStore(Map<String, NetworkId> networkIdByNodeId) {
        this.networkIdByNodeId.putAll(networkIdByNodeId);
    }

    @Override
    public bisq.network.protobuf.NetworkIdStore toProto() {
        return bisq.network.protobuf.NetworkIdStore.newBuilder()
                .putAllNetworkIdByNodeId(networkIdByNodeId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toProto())))
                .build();
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.NetworkIdStore proto) {
        return new NetworkIdStore(proto.getNetworkIdByNodeIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> NetworkId.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.NetworkIdStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(NetworkIdStore persisted) {
        networkIdByNodeId.clear();
        networkIdByNodeId.putAll(persisted.getNetworkIdByNodeId());
    }

    @Override
    public NetworkIdStore getClone() {
        return new NetworkIdStore(networkIdByNodeId);
    }
}