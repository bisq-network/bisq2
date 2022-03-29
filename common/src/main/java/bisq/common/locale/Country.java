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

public record Country(String code, String name, Region region) implements Proto {
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
