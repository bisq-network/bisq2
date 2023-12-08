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

package bisq.network.common;

import bisq.common.proto.Proto;
import bisq.common.util.NetworkUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Wrapper for a (sorted) TreeMap for the address by transport type map and for convenient protobuf methods.
 */
@EqualsAndHashCode
@ToString
@Getter
public class AddressByTransportTypeMap implements Map<TransportType, Address>, Proto {
    public static AddressByTransportTypeMap from(Set<TransportType> supportedTransportTypes,
                                                 Map<TransportType, Integer> defaultPorts,
                                                 boolean isForDefaultId,
                                                 String onionAddress) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();

        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        int torPort = isTorSupported && isForDefaultId ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) :
                NetworkUtils.selectRandomPort();

        if (isForDefaultId) {
            if (supportedTransportTypes.contains(TransportType.CLEAR)) {
                int port = defaultPorts.getOrDefault(TransportType.CLEAR, NetworkUtils.findFreeSystemPort());
                Address address = Address.localHost(port);
                addressByTransportTypeMap.put(TransportType.CLEAR, address);
            }

            if (isTorSupported) {
                Address address = new Address(onionAddress, torPort);
                addressByTransportTypeMap.put(TransportType.TOR, address);
            }
        } else {
            if (supportedTransportTypes.contains(TransportType.CLEAR)) {
                int port = NetworkUtils.findFreeSystemPort();
                Address address = Address.localHost(port);
                addressByTransportTypeMap.put(TransportType.CLEAR, address);
            }

            if (isTorSupported) {
                Address address = new Address(onionAddress, torPort);
                addressByTransportTypeMap.put(TransportType.TOR, address);
            }
        }

        return addressByTransportTypeMap;
    }

    // We use a TreeMap to get deterministic sorting.
    private final TreeMap<TransportType, Address> map = new TreeMap<>();

    public AddressByTransportTypeMap() {
    }

    public AddressByTransportTypeMap(Map<TransportType, Address> map) {
        this.map.putAll(map);
    }

    public AddressByTransportTypeMap(AddressByTransportTypeMap map) {
        this.map.putAll(map);
    }

    public bisq.network.common.protobuf.AddressByTransportTypeMap toProto() {
        return bisq.network.common.protobuf.AddressByTransportTypeMap.newBuilder()
                .putAllAddressByTransportType(map.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(),
                                e -> e.getValue().toProto())))
                .build();
    }

    public static AddressByTransportTypeMap fromProto(bisq.network.common.protobuf.AddressByTransportTypeMap proto) {
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

