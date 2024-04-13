package bisq.common.proto.mocks;

import bisq.common.annotation.ExcludeForHash;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class ChildMockWithExcludedValue implements Child {
    @ExcludeForHash
    private final String childValue;

    public ChildMockWithExcludedValue(String childValue) {
        this.childValue = childValue;
    }

    @Override
    public bisq.common.test.protobuf.Child toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public bisq.common.test.protobuf.Child.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.common.test.protobuf.Child.newBuilder()
                .setChildValue(childValue);
    }
}