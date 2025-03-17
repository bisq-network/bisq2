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

//todo rename to not conflict with chat.ChatMessageType
public enum ChatMessageType implements ProtoEnum {
    ALL,
    OFFER,
    TEXT;

    @Override
    public bisq.settings.protobuf.ChatMessageType toProtoEnum() {
        return bisq.settings.protobuf.ChatMessageType.valueOf(getProtobufEnumPrefix() + name());
    }

    public static ChatMessageType fromProto(bisq.settings.protobuf.ChatMessageType proto) {
        return ProtobufUtils.enumFromProto(ChatMessageType.class, proto.name(), ALL);
    }
}
