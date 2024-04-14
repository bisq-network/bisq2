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

package bisq.network.p2p.services.data.storage;

import bisq.common.data.ByteArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ToString
public final class DataStore<T extends DataRequest> implements PersistableStore<DataStore<T>> {
    @Getter
    private final Map<ByteArray, T> map = new ConcurrentHashMap<>();

    public DataStore() {
    }

    public DataStore(Map<ByteArray, T> map) {
        this.map.putAll(map);
    }

    @Override
    public bisq.network.protobuf.DataStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.DataStore.Builder getBuilder(boolean serializeForHash) {
        // Protobuf map do not support bytes as key
        List<bisq.network.protobuf.DataStore.MapEntry> mapEntries = map.entrySet().stream()
                .map(e -> bisq.network.protobuf.DataStore.MapEntry.newBuilder()
                        .setKey(e.getKey().toProto(serializeForHash))
                        .setValue(e.getValue().toProto(serializeForHash).getDataRequest())
                        .build())
                .collect(Collectors.toList());
        return bisq.network.protobuf.DataStore.newBuilder().addAllMapEntries(mapEntries);
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.DataStore proto) {
        return new DataStore<>(proto.getMapEntriesList().stream()
                .collect(Collectors.toMap(e -> ByteArray.fromProto(e.getKey()), e -> DataRequest.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.DataStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(DataStore<T> persisted) {
        map.clear();
        map.putAll(persisted.getMap());
    }

    @Override
    public DataStore<T> getClone() {
        return new DataStore<>(new HashMap<>(map));
    }
}