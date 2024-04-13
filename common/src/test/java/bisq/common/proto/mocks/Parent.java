package bisq.common.proto.mocks;

import bisq.common.proto.Proto;

public interface Parent extends Proto {
    String getParentValue();

    Child getChild();

    bisq.common.test.protobuf.Parent toProto(boolean ignoreAnnotation);

    bisq.common.test.protobuf.Parent.Builder getBuilder(boolean ignoreAnnotation);

    @Override
    default bisq.common.test.protobuf.Parent toProto() {
        return buildProto(true);
    }
}
