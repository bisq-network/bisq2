package bisq.common.proto;

import bisq.common.encoding.Hex;
import bisq.common.proto.mocks.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
public class ExcludeForHashTest {

    @Test
    public void testExcludeForHash() {
        if (new Date().before(Proto.ACTIVATE_EXCLUDE_FOR_HASH_DATE)) {
            return;
        }

        String serialized, serializeNonExcluded;
        Child child;
        Parent parent;

        // No annotations
        child = new ChildMock("childValue");
        parent = new ParentMock("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertEquals(serialized, serializeNonExcluded);

        // ParentMockWithExcludedValue
        child = new ChildMock("childValue");
        parent = new ParentMockWithExcludedValue("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertNotEquals(serialized, serializeNonExcluded);

        // If annotated field is set to default value (empty string) we get same results
        parent = new ParentMockWithExcludedValue("", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertEquals(serialized, serializeNonExcluded);

        // ParentMockWithExcludedChild
        child = new ChildMock("childValue");
        parent = new ParentMockWithExcludedChild("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertNotEquals(serialized, serializeNonExcluded);

        // ParentMockWithExcludedChild and ChildMockWithExcludedValue
        child = new ChildMockWithExcludedValue("childValue");
        parent = new ParentMockWithExcludedChild("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertNotEquals(serialized, serializeNonExcluded);


        // If child is excluded it does not matter what actual child impl is
        child = new ChildMockWithExcludedValue("childValue");
        parent = new ParentMockWithExcludedChild("parentValue", child);
        var serialized1 = Hex.encode(parent.serializeForHash());

        child = new ChildMock("childValue");
        parent = new ParentMockWithExcludedChild("parentValue", child);
        var serialized2 = Hex.encode(parent.serializeForHash());
        assertEquals(serialized1, serialized2);


        // ChildMockWithExcludedValue
        child = new ChildMockWithExcludedValue("childValue");
        parent = new ParentMock("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertNotEquals(serialized, serializeNonExcluded);


        // ChildMockWithExcludedValue and ParentMockWithExcludedValue
        child = new ChildMockWithExcludedValue("childValue");
        parent = new ParentMockWithExcludedValue("parentValue", child);
        serialized = Hex.encode(parent.serialize());
        serializeNonExcluded = Hex.encode(parent.serializeForHash());
        assertNotEquals(serialized, serializeNonExcluded);
    }
}
