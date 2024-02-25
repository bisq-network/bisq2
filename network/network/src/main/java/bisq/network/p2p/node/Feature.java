package bisq.network.p2p.node;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

/**
 * Features are used to signal which features the node supports. Those might be variations of different implementations
 * like HashCash or EquiHash algorithms for proof of work, or different implementations for requesting inventory data.
 */
public enum Feature implements ProtoEnum {
    INVENTORY_HASH_SET,
    INVENTORY_MINI_SKETCH,
    AUTHORIZATION_TOKEN_HASH_CASH,
    AUTHORIZATION_TOKEN_EQUI_HASH,
    AUTHORIZATION_TOKEN_BURNED_BSQ;

    @Override
    public bisq.network.protobuf.Feature toProto() {
        return bisq.network.protobuf.Feature.valueOf(getProtobufEnumPrefix() + name());
    }

    public static Feature fromProto(bisq.network.protobuf.Feature proto) {
        return ProtobufUtils.enumFromProto(Feature.class, proto.name());
    }
}
