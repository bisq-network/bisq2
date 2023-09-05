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

package bisq.chat;

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;

public enum ChatChannelDomain implements ProtoEnum {
    BISQ_EASY_OFFERBOOK,
    BISQ_EASY_OPEN_TRADES,
    BISQ_EASY_PRIVATE_CHAT,
    DISCUSSION,
    EVENTS,
    SUPPORT;

    private static final String protobufPrefix = StringUtils.capitalizeAll(ChatChannelDomain.class.getSimpleName()) + "_";;

    @Override
    public bisq.chat.protobuf.ChatChannelDomain toProto() {
        return bisq.chat.protobuf.ChatChannelDomain.valueOf(protobufPrefix + name());
    }

    public static ChatChannelDomain fromProto(bisq.chat.protobuf.ChatChannelDomain proto) {
        return ProtobufUtils.enumFromProto(ChatChannelDomain.class, proto.name(), BISQ_EASY_OFFERBOOK);
    }

    public String getDisplayString() {
        return Res.get("chat.channelDomain." + name());
    }
}
