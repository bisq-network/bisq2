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
public final class StringLongPair implements PersistableProto {
    private final String key;
    private final Long value;

    public StringLongPair(String key, Long value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public bisq.common.protobuf.StringLongPair toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.StringLongPair.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.StringLongPair.newBuilder()
                .setKey(key)
                .setValue(value);
    }

    public static StringLongPair fromProto(bisq.common.protobuf.StringLongPair proto) {
        return new StringLongPair(proto.getKey(), proto.getValue());
    }
}
