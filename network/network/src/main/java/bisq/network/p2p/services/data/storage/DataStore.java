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
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
@ToString
public final class DataStore<T extends DataRequest> implements PersistableStore<DataStore<T>> {
    @Getter(AccessLevel.PUBLIC)
    private final Map<ByteArray, T> map = new ConcurrentHashMap<>();
    // By storage key: the sequence number of the add request which the stored removal replaced in this store, directly
    // or through the removal which it replaced. The store had checked that add request, so the number also records that
    // the store held the removed data. A removal without number is one which the store accepted without entry, or which
    // a node of an earlier version stored (see docs/specifications/network/storage-request-ordering.md). An entry is
    // persisted with the entry of map which has the same key. No peer gets it.
    @Getter(AccessLevel.PUBLIC)
    private final Map<ByteArray, Integer> replacedAddSequenceNrs = new ConcurrentHashMap<>();

    DataStore(Map<ByteArray, T> map) {
        this(map, Map.of());
    }

    DataStore(Map<ByteArray, T> map, Map<ByteArray, Integer> replacedAddSequenceNrs) {
        this.map.putAll(map);
        this.replacedAddSequenceNrs.putAll(replacedAddSequenceNrs);
    }

    @Override
    public bisq.network.protobuf.DataStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.DataStore.Builder getBuilder(boolean serializeForHash) {
        // Protobuf map do not support bytes as key
        List<bisq.network.protobuf.DataStore.MapEntry> mapEntries = map.entrySet().stream()
                .map(e -> {
                    var builder = bisq.network.protobuf.DataStore.MapEntry.newBuilder()
                            .setKey(e.getKey().toProto(serializeForHash))
                            .setValue(e.getValue().toProto(serializeForHash).getDataRequest());
                    Optional.ofNullable(replacedAddSequenceNrs.get(e.getKey()))
                            .ifPresent(builder::setReplacedAddSequenceNr);
                    return builder.build();
                })
                .collect(Collectors.toList());
        return bisq.network.protobuf.DataStore.newBuilder().addAllMapEntries(mapEntries);
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.DataStore proto)
            throws UnresolvableProtobufException {
        try {
            List<PersistedEntry> entries = ProtobufUtils.fromProtoStreamSkippingInvalid(proto.getMapEntriesList(),
                            e -> new PersistedEntry(ByteArray.fromProto(e.getKey()),
                                    DataRequest.fromProto(e.getValue()),
                                    // 0 if the removal replaced no add request
                                    e.getReplacedAddSequenceNr() > 0
                                            ? Optional.of(e.getReplacedAddSequenceNr())
                                            : Optional.empty()))
                    .toList();
            Map<ByteArray, DataRequest> map = entries.stream()
                    .collect(Collectors.toMap(PersistedEntry::key, PersistedEntry::request));
            // Only for the entries which could be read
            Map<ByteArray, Integer> replacedAddSequenceNrs = entries.stream()
                    .filter(entry -> entry.replacedAddSequenceNr().isPresent())
                    .collect(Collectors.toMap(PersistedEntry::key,
                            entry -> entry.replacedAddSequenceNr().orElseThrow()));
            return new DataStore<>(map, replacedAddSequenceNrs);
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
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
        replacedAddSequenceNrs.clear();
        replacedAddSequenceNrs.putAll(persisted.getReplacedAddSequenceNrs());
    }

    @Override
    public DataStore<T> getClone() {
        return new DataStore<>(Map.copyOf(map), Map.copyOf(replacedAddSequenceNrs));
    }

    // This is only temporary to not risk that we get an exception if a client mutates
    // the map which is a not valid case, but we the mobile release we prefer to avoid the risk to run into an exception.
    // For main and for a major mobile update we should remove that.
    public DataStore<T> getMutableClone() {
        return new DataStore<>(new HashMap<>(map), new HashMap<>(replacedAddSequenceNrs));
    }

    private record PersistedEntry(ByteArray key, DataRequest request, Optional<Integer> replacedAddSequenceNr) {
    }
}