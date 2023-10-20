package bisq.network.p2p.node;

import bisq.common.proto.Proto;
import bisq.network.p2p.node.transport.TransportType;
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

    public bisq.network.protobuf.AddressByTransportTypeMap toProto() {
        return bisq.network.protobuf.AddressByTransportTypeMap.newBuilder()
                .putAllAddressByTransportType(map.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(),
                                e -> e.getValue().toProto())))
                .build();
    }

    public static AddressByTransportTypeMap fromProto(bisq.network.protobuf.AddressByTransportTypeMap proto) {
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
