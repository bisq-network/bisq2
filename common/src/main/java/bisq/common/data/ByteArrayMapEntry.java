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

package bisq.common.data;

import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class ByteArrayMapEntry implements PersistableProto {
    private final ByteArray key;
    private final ByteArray value;

    public ByteArrayMapEntry(ByteArray key, ByteArray value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public bisq.common.protobuf.ByteArrayMapEntry toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.ByteArrayMapEntry.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.ByteArrayMapEntry.newBuilder()
                .setKey(key.toProto(serializeForHash))
                .setValue(value.toProto(serializeForHash));
    }

    public static ByteArrayMapEntry fromProto(bisq.common.protobuf.ByteArrayMapEntry proto) {
        return new ByteArrayMapEntry(ByteArray.fromProto(proto.getKey()),
                ByteArray.fromProto(proto.getValue()));
    }
}
