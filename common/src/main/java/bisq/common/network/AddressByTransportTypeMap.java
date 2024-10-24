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

package bisq.common.network;

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Wrapper for a (sorted) TreeMap for the address by transport type map and for convenient protobuf methods.
 */
@Slf4j
@EqualsAndHashCode
@ToString
@Getter
public final class AddressByTransportTypeMap implements Map<TransportType, Address>, NetworkProto {
    // We use a TreeMap to get deterministic sorting.
    private final TreeMap<TransportType, Address> map = new TreeMap<>();

    public AddressByTransportTypeMap() {
    }

    public AddressByTransportTypeMap(AddressByTransportTypeMap map) {
        this(map.getMap());
    }

    public AddressByTransportTypeMap(Map<TransportType, Address> map) {
        this.map.putAll(map);

        verify();
    }

    @Override
    public void verify() {
        checkArgument(map.size() <= TransportType.values().length,
                "map size must not be larger than TransportType.values().length");
        checkArgument(!map.isEmpty(), "map must not be empty");
    }

    @Override
    public bisq.common.protobuf.AddressByTransportTypeMap toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.AddressByTransportTypeMap.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.AddressByTransportTypeMap.newBuilder()
                .putAllAddressByTransportType(map.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(),
                                e -> e.getValue().toProto(serializeForHash))));
    }

    public static AddressByTransportTypeMap fromProto(bisq.common.protobuf.AddressByTransportTypeMap proto) {
        Map<TransportType, Address> map = proto.getAddressByTransportTypeMap().entrySet().stream()
                .collect(Collectors.toMap(e -> Enum.valueOf(TransportType.class, e.getKey()),
                        e -> Address.fromProto(e.getValue())));
        return new AddressByTransportTypeMap(map);
    }

    // Delegates

    @Override
    public Address put(TransportType key, Address value) {
        return map.put(key, value);
    }

    @Override
    public Address remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends TransportType, ? extends Address> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Address get(Object key) {
        return map.get(key);
    }

    @Override
    public Set<TransportType> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Address> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<TransportType, Address>> entrySet() {
        return map.entrySet();
    }
}

