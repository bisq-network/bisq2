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

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class Country implements Proto {
    private final String code;
    private final String name;
    private final Region region;

    public Country(String code, String name, Region region) {
        this.code = code;
        this.name = name;
        this.region = region;
    }

    public bisq.common.protobuf.Country toProto() {
        return bisq.common.protobuf.Country.newBuilder()
                .setCode(code)
                .setCode(name)
                .setRegion(region.toProto())
                .build();
    }

    public static Country fromProto(bisq.common.protobuf.Country proto) {
        return new Country(proto.getCode(), proto.getName(), Region.fromProto(proto.getRegion()));
    }
}
