package bisq.common.proto.mocks;

import bisq.common.proto.Proto;

public interface Parent extends Proto {
    String getParentValue();

    Child getChild();

    bisq.common.test.protobuf.Parent toProto(boolean serializeForHash);

    bisq.common.test.protobuf.Parent.Builder getBuilder(boolean serializeForHash);

    @Override
    default bisq.common.test.protobuf.Parent toProto() {
        return buildProto(false);
    }
}
