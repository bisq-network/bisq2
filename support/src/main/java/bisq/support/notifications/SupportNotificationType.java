package bisq.support.notifications;
import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.common.proto.UnresolvableProtobufEnumException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SupportNotificationType implements ProtoEnum {
    MODERATOR_NOTIFICATION("MODERATOR_NOTIFICATION"),
    BISQ_EASY_MEDIATOR_NOTIFICATION("BISQ_EASY_MEDIATOR_NOTIFICATION"),
    MU_SIG_MEDIATOR_NOTIFICATION("MU_SIG_MEDIATOR_NOTIFICATION");

    private final String name;

    @Override
    public bisq.support.protobuf.SupportNotificationType toProtoEnum() {
        return bisq.support.protobuf.SupportNotificationType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static SupportNotificationType fromProtoEnum(bisq.support.protobuf.SupportNotificationType proto) {
        try {
            return ProtobufUtils.enumFromProto(SupportNotificationType.class, proto.name());
        } catch (UnresolvableProtobufEnumException e) {
            throw new RuntimeException(e);
        }
    }
}
