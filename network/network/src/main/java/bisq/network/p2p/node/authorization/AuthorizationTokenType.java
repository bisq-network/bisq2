package bisq.network.p2p.node.authorization;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.network.p2p.node.Feature;

import java.util.Optional;

public enum AuthorizationTokenType implements ProtoEnum {
    HASH_CASH,
    @Deprecated(since = "2.1.2")
    EQUI_HASH,
    HASH_CASH_V2;

    public static Optional<AuthorizationTokenType> fromFeature(Feature feature) {
        switch (feature) {
            case AUTHORIZATION_HASH_CASH:
                return Optional.of(AuthorizationTokenType.HASH_CASH);
            case AUTHORIZATION_EQUI_HASH:
                return Optional.of(AuthorizationTokenType.EQUI_HASH);
            case AUTHORIZATION_HASH_CASH_V2:
                return Optional.of(AuthorizationTokenType.HASH_CASH_V2);
            default:
                return Optional.empty();
        }
    }

    @Override
    public bisq.network.protobuf.AuthorizationTokenType toProtoEnum() {
        return bisq.network.protobuf.AuthorizationTokenType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static AuthorizationTokenType fromProto(bisq.network.protobuf.AuthorizationTokenType proto) {
        return ProtobufUtils.enumFromProto(AuthorizationTokenType.class, proto.name(), HASH_CASH);
    }
}
