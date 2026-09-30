package bisq.network.p2p.node;

import bisq.common.proto.ProtoEnum;

import java.util.Set;

/**
 * Features are used to signal which features the node supports. Those might be variations of different implementations
 * like HashCash or EquiHash algorithms for proof of work, or different implementations for requesting inventory data.
 */
public enum Feature implements ProtoEnum {
    // Replaced by INVENTORY_HASH_SET_V2. Still advertised, so that nodes of earlier versions can request inventory.
    @Deprecated(since = "2.1.14")
    INVENTORY_HASH_SET,
    INVENTORY_MINI_SKETCH,
    AUTHORIZATION_HASH_CASH,
    AUTHORIZATION_EQUI_HASH,
    AUTHORIZATION_HASH_CASH_V2,
    // Inventory hash set filter with storage key V2 (see docs/specifications/network/inventory-hash-set-filter.md)
    INVENTORY_HASH_SET_V2;

    public static final Set<Feature> DEFAULT_FEATURES = Set.of(INVENTORY_HASH_SET, AUTHORIZATION_HASH_CASH);

    @Override
    public bisq.network.protobuf.Feature toProtoEnum() {
        return bisq.network.protobuf.Feature.valueOf(getProtobufEnumPrefix() + name());
    }

    // fromProto is not used
}
