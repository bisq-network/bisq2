package bisq.network.p2p.node;

import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.message.NetworkMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class CloseConnectionMessage implements NetworkMessage {
    private final CloseReason closeReason;

    public CloseConnectionMessage(CloseReason closeReason) {
        this.closeReason = closeReason;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        var builder = bisq.network.protobuf.CloseConnectionMessage.newBuilder()
                .setCloseReason(closeReason.name());
        return getNetworkMessageBuilder().setCloseConnectionMessage(builder).build();
    }

    public static CloseConnectionMessage fromProto(bisq.network.protobuf.CloseConnectionMessage proto) {
        return new CloseConnectionMessage(ProtobufUtils.enumFromProto(CloseReason.class,
                proto.getCloseReason()));
    }
}
