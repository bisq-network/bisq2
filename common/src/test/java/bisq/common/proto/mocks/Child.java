package bisq.common.proto.mocks;

import bisq.common.proto.Proto;

public interface Child extends Proto {
    String getChildValue();

    bisq.common.test.protobuf.Child toProto(boolean serializeForHash);

    bisq.common.test.protobuf.Child.Builder getBuilder(boolean serializeForHash);
}
