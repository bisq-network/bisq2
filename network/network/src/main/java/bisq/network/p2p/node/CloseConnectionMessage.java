package bisq.network.p2p.node;

import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public final class CloseConnectionMessage implements EnvelopePayloadMessage {
    private final CloseReason closeReason;

    public CloseConnectionMessage(CloseReason closeReason) {
        this.closeReason = closeReason;
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto() {
        var builder = bisq.network.protobuf.CloseConnectionMessage.newBuilder()
                .setCloseReason(closeReason.name());
        return getNetworkMessageBuilder().setCloseConnectionMessage(builder).build();
    }

    public static CloseConnectionMessage fromProto(bisq.network.protobuf.CloseConnectionMessage proto) {
        return new CloseConnectionMessage(ProtobufUtils.enumFromProto(CloseReason.class,
                proto.getCloseReason()));
    }
}
