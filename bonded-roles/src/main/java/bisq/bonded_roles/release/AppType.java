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

package bisq.bonded_roles.release;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;

public enum AppType implements ProtoEnum {
    DESKTOP,
    MOBILE_NODE,
    MOBILE_CLIENT;

    @Override
    public bisq.bonded_roles.protobuf.AppType toProtoEnum() {
        return bisq.bonded_roles.protobuf.AppType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static AppType fromProto(bisq.bonded_roles.protobuf.AppType proto) {
        return ProtobufUtils.enumFromProto(AppType.class, proto.name(), AppType.DESKTOP);
    }

    public String getDisplayString() {
        return Res.get("authorizedRole.releaseManager.appType."+name());
    }
}