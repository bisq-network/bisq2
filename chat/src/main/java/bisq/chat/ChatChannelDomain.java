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
import bisq.common.proto.ProtobufUtils;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public enum ChatChannelDomain implements ProtoEnum {
    BISQ_EASY_OFFERBOOK,
    BISQ_EASY_OPEN_TRADES,
    DISCUSSION,
    SUPPORT,

    @Deprecated BISQ_EASY_PRIVATE_CHAT(DISCUSSION), // Dropped in 2.1.1, all messages will fall back on domain DISCUSSION,
    @Deprecated EVENTS(DISCUSSION); // Dropped in 2.1.1, all messages will fall back on domain DISCUSSION

    @Getter
    private final Optional<ChatChannelDomain> fallback;

    ChatChannelDomain() {
        this.fallback = Optional.empty();
    }

    ChatChannelDomain(ChatChannelDomain fallback) {
        this.fallback = Optional.of(fallback);
    }

    public ChatChannelDomain migrate() {
        return fallback.orElse(this);
    }

    @Override
    public bisq.chat.protobuf.ChatChannelDomain toProtoEnum() {
        return bisq.chat.protobuf.ChatChannelDomain.valueOf(getProtobufEnumPrefix() + name());
    }

    public boolean isDeprecated() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> field.getName().equals(name()))
                .peek(field -> field.setAccessible(true))
                .anyMatch(field -> field.isAnnotationPresent(Deprecated.class));
    }

    public static ChatChannelDomain fromProto(bisq.chat.protobuf.ChatChannelDomain proto) {
        return ProtobufUtils.enumFromProto(ChatChannelDomain.class, proto.name(), BISQ_EASY_OFFERBOOK);
    }

    public String getDisplayString() {
        return Res.get("chat.channelDomain." + name());
    }
}
