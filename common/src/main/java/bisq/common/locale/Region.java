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

package bisq.common.locale;

import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class Region implements PersistableProto {
    private final String code;
    private final String name;

    public Region(String code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public bisq.common.protobuf.Region toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.common.protobuf.Region.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.Region.newBuilder()
                .setCode(code)
                .setName(name);
    }

    public static Region fromProto(bisq.common.protobuf.Region proto) {
        return new Region(proto.getCode(), proto.getName());
    }
}
