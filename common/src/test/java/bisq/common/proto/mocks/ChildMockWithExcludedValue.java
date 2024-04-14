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
    public bisq.common.test.protobuf.Child toProto(boolean serializeForHash) {
        return buildProto(serializeForHash);
    }

    @Override
    public bisq.common.test.protobuf.Child.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.test.protobuf.Child.newBuilder()
                .setChildValue(childValue);
    }
}
