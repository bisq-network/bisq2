package bisq.network.p2p.node.authorization;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.network.p2p.node.Feature;

import java.util.Optional;

public enum AuthorizationTokenType implements ProtoEnum {
    HASH_CASH,
    EQUI_HASH;

    public static Optional<AuthorizationTokenType> fromFeature(Feature feature) {
        return switch (feature) {
            case AUTHORIZATION_HASH_CASH -> Optional.of(AuthorizationTokenType.HASH_CASH);
            case AUTHORIZATION_EQUI_HASH -> Optional.of(AuthorizationTokenType.EQUI_HASH);
            default -> Optional.empty();
        };
    }

    @Override
    public bisq.network.protobuf.AuthorizationTokenType toProtoEnum() {
        return bisq.network.protobuf.AuthorizationTokenType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static AuthorizationTokenType fromProto(bisq.network.protobuf.AuthorizationTokenType proto) {
        return ProtobufUtils.enumFromProto(AuthorizationTokenType.class, proto.name(), HASH_CASH);
    }
}
