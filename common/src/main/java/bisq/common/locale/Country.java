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
public final class Country implements PersistableProto {
    private final String code;
    private final String name;
    private final Region region;

    /**
     * @param code   Uppercase ISO 3166 2-letter code or a UN M.49 3-digit code.
     * @param name   Name of the county
     * @param region Region of the country
     */
    public Country(String code, String name, Region region) {
        this.code = code;
        this.name = name;
        this.region = region;
    }

    @Override
    public bisq.common.protobuf.Country.Builder getBuilder(boolean serializeForHash) {
        return bisq.common.protobuf.Country.newBuilder()
                .setCode(code)
                .setName(name)
                .setRegion(region.toProto(serializeForHash));
    }

    @Override
    public bisq.common.protobuf.Country toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Country fromProto(bisq.common.protobuf.Country proto) {
        return new Country(proto.getCode(), proto.getName(), Region.fromProto(proto.getRegion()));
    }
}
