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

package bisq.bonded_roles;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.ToString;

@ToString
public enum BondedRoleType implements ProtoEnum {
    MEDIATOR,
    ARBITRATOR,
    MODERATOR,
    SECURITY_MANAGER,
    RELEASE_MANAGER,

    SEED_NODE(true),
    ORACLE_NODE(true),
    EXPLORER_NODE(true),
    MARKET_PRICE_NODE(true);

    @Getter
    private final boolean isNode;

    BondedRoleType() {
        this(false);
    }

    BondedRoleType(boolean isNode) {
        this.isNode = isNode;
    }

    public boolean isRole() {
        return !isNode;
    }

    @Override
    public bisq.bonded_roles.protobuf.BondedRoleType toProtoEnum() {
        return bisq.bonded_roles.protobuf.BondedRoleType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static BondedRoleType fromProto(bisq.bonded_roles.protobuf.BondedRoleType proto) {
        return ProtobufUtils.enumFromProto(BondedRoleType.class, proto.name(), MEDIATOR);
    }

    public String getDisplayString() {
        return Res.get("user.bondedRoles.type." + name());
    }
}
