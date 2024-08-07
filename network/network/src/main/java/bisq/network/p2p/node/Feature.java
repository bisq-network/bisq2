package bisq.network.p2p.node;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

/**
 * Features are used to signal which features the node supports. Those might be variations of different implementations
 * like HashCash or EquiHash algorithms for proof of work, or different implementations for requesting inventory data.
 */
public enum Feature implements ProtoEnum {
    INVENTORY_HASH_SET,
    INVENTORY_MINI_SKETCH,
    AUTHORIZATION_HASH_CASH,
    AUTHORIZATION_EQUI_HASH;

    @Override
    public bisq.network.protobuf.Feature toProtoEnum() {
        return bisq.network.protobuf.Feature.valueOf(getProtobufEnumPrefix() + name());
    }

    // Not used. Feature is used as list in Capability thus we use ProtobufUtils.fromProtoEnumList.
    // Still keep fromProto for following convention and potential future usage.
    public static Feature fromProto(bisq.network.protobuf.Feature proto) {
        return ProtobufUtils.enumFromProto(Feature.class, proto.name());
    }
}
