package bisq.network.p2p.services.confidential.ack;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import lombok.Getter;

@Getter
public enum MessageDeliveryStatus implements ProtoEnum {
    CONNECTING,
    SENT,
    ACK_RECEIVED(true),
    TRY_ADD_TO_MAILBOX,
    ADDED_TO_MAILBOX,
    MAILBOX_MSG_RECEIVED(true),
    FAILED;

    private final boolean received;

    MessageDeliveryStatus() {
        this(false);
    }

    MessageDeliveryStatus(boolean received) {
        this.received = received;
    }

    public boolean isPending() {
        return this == CONNECTING || this == SENT || this == TRY_ADD_TO_MAILBOX;
    }

    @Override
    public bisq.network.protobuf.MessageDeliveryStatus toProtoEnum() {
        return bisq.network.protobuf.MessageDeliveryStatus.valueOf(getProtobufEnumPrefix() + name());
    }

    public static MessageDeliveryStatus fromProto(bisq.network.protobuf.MessageDeliveryStatus proto) {
        return ProtobufUtils.enumFromProto(MessageDeliveryStatus.class, proto.name(), CONNECTING);
    }
}
