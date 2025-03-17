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

package bisq.bonded_roles.security_manager.alert;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum AlertType implements ProtoEnum {
    INFO,
    WARN,
    EMERGENCY,
    BAN,
    BANNED_ACCOUNT_DATA;

    @Override
    public bisq.bonded_roles.protobuf.AlertType toProtoEnum() {
        return bisq.bonded_roles.protobuf.AlertType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static AlertType fromProto(bisq.bonded_roles.protobuf.AlertType proto) {
        return ProtobufUtils.enumFromProto(AlertType.class, proto.name(), INFO);
    }

    public static boolean isMessageAlert(AlertType alertType) {
        return alertType == AlertType.INFO ||
                alertType == AlertType.WARN ||
                alertType == AlertType.EMERGENCY;
    }
}
