/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.proto;

import bisq.common.annotation.ExcludeForHash;
import bisq.common.test.protobuf.Child;
import bisq.common.test.protobuf.Parent;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExcludeForHashFieldsTest {

    @Test
    public void classWithoutAnnotationsExcludesNothing() {
        assertTrue(ExcludeForHashFields.get(NoExclusions.class, 0).isEmpty());
    }

    @Test
    public void fieldWithoutVersionsIsExcludedInEveryVersion() {
        assertEquals(Set.of("child"), ExcludeForHashFields.get(Versioned.class, 0));
        assertEquals(Set.of("child"), ExcludeForHashFields.get(Versioned.class, 3));
    }

    @Test
    public void fieldWithVersionsIsExcludedOnlyInThoseVersions() {
        assertEquals(Set.of("child", "parentValue"), ExcludeForHashFields.get(Versioned.class, 1));
        assertEquals(Set.of("child", "parentValue"), ExcludeForHashFields.get(Versioned.class, 2));
    }

    @Test
    public void fieldsOfProtoSuperclassesAreIncluded() {
        assertEquals(Set.of("parentValue"), ExcludeForHashFields.get(Subclass.class, 0));
    }

    @Test
    public void repeatedLookupsReturnTheCachedSet() {
        assertSame(ExcludeForHashFields.get(Versioned.class, 1), ExcludeForHashFields.get(Versioned.class, 1));
    }

    @Test
    public void serializeForHashExcludesByTheVersionOfEachInstance() throws InvalidProtocolBufferException {
        Parent version0 = Parent.parseFrom(new Versioned(0).serializeForHash());
        Parent version1 = Parent.parseFrom(new Versioned(1).serializeForHash());

        assertEquals("parentValue", version0.getParentValue());
        assertEquals("", version1.getParentValue());
        assertFalse(version0.hasChild());
        assertFalse(version1.hasChild());
    }

    private static final class NoExclusions implements Proto {
        private final String parentValue = "parentValue";

        @Override
        public Parent.Builder getBuilder(boolean serializeForHash) {
            return Parent.newBuilder().setParentValue(parentValue);
        }
    }

    private static final class Versioned implements Proto {
        private final int version;
        @ExcludeForHash(excludeOnlyInVersions = {1, 2})
        private final String parentValue = "parentValue";
        @ExcludeForHash
        private final String child = "childValue";

        private Versioned(int version) {
            this.version = version;
        }

        @Override
        public int getVersion() {
            return version;
        }

        @Override
        public Parent.Builder getBuilder(boolean serializeForHash) {
            return Parent.newBuilder()
                    .setParentValue(parentValue)
                    .setChild(Child.newBuilder().setChildValue(child));
        }
    }

    private static abstract class ProtoBase implements Proto {
        @ExcludeForHash
        private final String parentValue = "parentValue";
    }

    private static final class Subclass extends ProtoBase {
        @Override
        public Parent.Builder getBuilder(boolean serializeForHash) {
            return Parent.newBuilder();
        }
    }
}
