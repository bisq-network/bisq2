package bisq.network.p2p.node;

import bisq.common.proto.ProtobufUtils;
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

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setCloseConnectionMessage(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.CloseConnectionMessage toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.CloseConnectionMessage.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.CloseConnectionMessage.newBuilder().setCloseReason(closeReason.name());
    }

    public static CloseConnectionMessage fromProto(bisq.network.protobuf.CloseConnectionMessage proto) {
        return new CloseConnectionMessage(ProtobufUtils.enumFromProto(CloseReason.class,
                proto.getCloseReason()));
    }

    @Override
    public double getCostFactor() {
        return 0.05;
    }
}
