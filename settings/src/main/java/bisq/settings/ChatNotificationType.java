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

package bisq.settings;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;

public enum ChatNotificationType implements ProtoEnum {
    ALL,
    MENTION,
    OFF;

    @Override
    public bisq.settings.protobuf.ChatNotificationType toProtoEnum() {
        return bisq.settings.protobuf.ChatNotificationType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static ChatNotificationType fromProto(bisq.settings.protobuf.ChatNotificationType proto) {
        return ProtobufUtils.enumFromProto(ChatNotificationType.class, proto.name(), ALL);
    }
}
