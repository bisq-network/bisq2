package bisq.network.p2p.node.authorization;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

/**
 * Features are used to signal which features the node supports. Those might be variations of different implementations
 * like HashCash or EquiHash algorithms for proof of work, or different implementations for requesting inventory data.
 */
public enum AuthorizationType implements ProtoEnum {
    HASH_CASH,
    EQUI_HASH,
    BURNED_BSQ;

    @Override
    public bisq.network.protobuf.Feature toProto() {
        return bisq.network.protobuf.Feature.valueOf(getProtobufEnumPrefix() + name());
    }

    public static AuthorizationType fromProto(bisq.network.protobuf.Feature proto) {
        return ProtobufUtils.enumFromProto(AuthorizationType.class, proto.name());
    }
}
