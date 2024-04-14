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

package bisq.common.proto.mocks;

import bisq.common.annotation.ExcludeForHash;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public final class ParentMockWithExcludedChild implements Parent {
    private final String parentValue;
    @ExcludeForHash
    private final Child child;

    public ParentMockWithExcludedChild(String parentValue, Child child) {
        this.parentValue = parentValue;
        this.child = child;
    }

    @Override
    public bisq.common.test.protobuf.Parent toProto(boolean serializeForHash) {
        return buildProto(serializeForHash);
    }

    @Override
    public bisq.common.test.protobuf.Parent.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.test.protobuf.Parent.newBuilder()
                .setParentValue(parentValue)
                .setChild(child.toProto(serializeForHash));
    }
}
