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

package bisq.rest_api.proto;

import bisq.common.proto.NetworkProto;
import bisq.common.proto.Proto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
@Schema(name = "AddressDetails")
public final class AddressDetails implements NetworkProto {
    private final String address;
    private final String bondedRoleType;
    private final String nickNameOrBondUserName;

    public AddressDetails(String address, String bondedRoleType, String nickNameOrBondUserName) {
        this.address = address;
        this.bondedRoleType = bondedRoleType;
        this.nickNameOrBondUserName = nickNameOrBondUserName;
    }

    @Override
    public void verify() {
        checkArgument(address != null && address.length() > 0);
        checkArgument(bondedRoleType != null && bondedRoleType.length() > 0);
        checkArgument(nickNameOrBondUserName != null && nickNameOrBondUserName.length() > 0);
    }

    @Override
    public bisq.rest_api.protobuf.AddressDetails toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.rest_api.protobuf.AddressDetails.Builder getBuilder(boolean serializeForHash) {
        return bisq.rest_api.protobuf.AddressDetails.newBuilder()
                .setAddress(this.address)
                .setBondedRoleType(this.bondedRoleType)
                .setNickNameOrBondUserName(this.nickNameOrBondUserName);
    }

    public static AddressDetails fromProto(bisq.rest_api.protobuf.AddressDetails proto) {
        return new AddressDetails(proto.getAddress(),
                proto.getBondedRoleType(),
                proto.getNickNameOrBondUserName());
    }
}
