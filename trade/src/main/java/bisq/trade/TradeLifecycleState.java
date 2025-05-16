package bisq.trade;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum TradeLifecycleState implements ProtoEnum {
    ACTIVE,     // In progress, or completed but user hasn't moved it
    HISTORICAL, // Finished and moved to history view
    FAILED;     // Concluded with an FSM error

    @Override
    public bisq.trade.protobuf.TradeLifecycleState toProtoEnum() {
        return bisq.trade.protobuf.TradeLifecycleState.valueOf(getProtobufEnumPrefix() + name());
    }


    public static TradeLifecycleState fromProto(bisq.trade.protobuf.TradeLifecycleState proto) {
        return ProtobufUtils.enumFromProto(TradeLifecycleState.class, proto.name(), ACTIVE);
    }

}
