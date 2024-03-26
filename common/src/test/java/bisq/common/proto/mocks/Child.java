package bisq.common.proto.mocks;

import bisq.common.proto.Proto;

public interface Child extends Proto {
    String getChildValue();

    bisq.common.test.protobuf.Child toProto(boolean ignoreAnnotation);

    bisq.common.test.protobuf.Child.Builder getBuilder(boolean ignoreAnnotation);


}
