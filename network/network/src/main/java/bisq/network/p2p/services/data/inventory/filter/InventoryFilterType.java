package bisq.network.p2p.services.data.inventory.filter;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

public enum InventoryFilterType implements ProtoEnum {
    HASH_SET,
    MINI_SKETCH;

    @Override
    public bisq.network.protobuf.InventoryFilterType toProto() {
        return bisq.network.protobuf.InventoryFilterType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static InventoryFilterType fromProto(bisq.network.protobuf.InventoryFilterType proto) {
        return ProtobufUtils.enumFromProto(InventoryFilterType.class, proto.name(), HASH_SET);
    }
}
