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

package bisq.chat.discuss.pub;

import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PublicChannel;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicDiscussionChannel extends PublicChannel<PublicDiscussionChatMessage> {
    private final String channelName;
    private final String description;
    private final String channelAdminId;
    private final Set<String> channelModeratorIds;

    public PublicDiscussionChannel(String id) {
        this(id,
                Res.get("discussion." + id + ".name"),
                Res.get("discussion." + id + ".description"),
                "",
                new HashSet<>(),
                ChannelNotificationType.MENTION);
    }

    public PublicDiscussionChannel(String id,
                                   String channelName,
                                   String description,
                                   String channelAdminId,
                                   Set<String> channelModeratorIds) {
        this(id,
                channelName,
                description,
                channelAdminId,
                channelModeratorIds,
                ChannelNotificationType.MENTION);
    }

    private PublicDiscussionChannel(String id,
                                    String channelName,
                                    String description,
                                    String channelAdminId,
                                    Set<String> channelModeratorIds,
                                    ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);

        this.channelName = channelName;
        this.description = description;
        this.channelAdminId = channelAdminId;
        this.channelModeratorIds = channelModeratorIds;
    }

    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder()
                .setPublicDiscussionChannel(bisq.chat.protobuf.PublicDiscussionChannel.newBuilder()
                        .setChannelName(channelName)
                        .setDescription(description)
                        .setChannelAdminId(channelAdminId)
                        .addAllChannelModeratorIds(channelModeratorIds))
                .build();
    }

    public static PublicDiscussionChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                    bisq.chat.protobuf.PublicDiscussionChannel proto) {
        return new PublicDiscussionChannel(
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                proto.getChannelAdminId(),
                new HashSet<>(proto.getChannelModeratorIdsList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
    }

    @Override
    public void addChatMessage(PublicDiscussionChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PublicDiscussionChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PublicDiscussionChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return channelName;
    }
}