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

package bisq.chat.notifications;

import bisq.common.proto.ProtoEnum;
import bisq.common.proto.ProtobufUtils;
import bisq.settings.ChatNotificationType;

public enum ChatChannelNotificationType implements ProtoEnum {
    GLOBAL_DEFAULT, // As defined in settings
    ALL,
    MENTION,
    OFF;

    @Override
    public bisq.chat.protobuf.ChatChannelNotificationType toProtoEnum() {
        return bisq.chat.protobuf.ChatChannelNotificationType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static ChatChannelNotificationType fromProto(bisq.chat.protobuf.ChatChannelNotificationType proto) {
        return ProtobufUtils.enumFromProto(ChatChannelNotificationType.class, proto.name(), GLOBAL_DEFAULT);
    }

    public static ChatChannelNotificationType fromChatNotificationType(ChatNotificationType chatNotificationType) {
        return switch (chatNotificationType) {
            case ALL -> ChatChannelNotificationType.ALL;
            case MENTION -> ChatChannelNotificationType.MENTION;
            default -> ChatChannelNotificationType.OFF;
        };
    }
}
