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

package bisq.chat.channel.pub;

import bisq.chat.ChatDomain;
import bisq.chat.channel.ChannelNotificationType;
import bisq.chat.channel.PublicChannel;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PublicModeratedChannel extends PublicChannel<PublicModeratedChatMessage> {
    private final String displayName;
    private final String description;
    private final String channelAdminId;
    private final List<String> channelModeratorIds;

    public PublicModeratedChannel(ChatDomain chatDomain, String channelName) {
        this(chatDomain,
                channelName,
                Res.get(chatDomain.name().toLowerCase() + "." + channelName + ".name"),
                Res.get(chatDomain.name().toLowerCase() + "." + channelName + ".description"),
                "",
                new ArrayList<>(),
                ChannelNotificationType.MENTION);
    }

    private PublicModeratedChannel(ChatDomain chatDomain,
                                   String channelName,
                                   String displayName,
                                   String description,
                                   String channelAdminId,
                                   List<String> channelModeratorIds,
                                   ChannelNotificationType channelNotificationType
    ) {
        super(chatDomain, channelName, channelNotificationType);

        this.displayName = displayName;
        this.description = description;
        this.channelAdminId = channelAdminId;
        this.channelModeratorIds = channelModeratorIds;
        // We need to sort deterministically as the data is used in the proof of work check
        this.channelModeratorIds.sort(Comparator.comparing((String e) -> e));
    }

    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder()
                .setPublicModeratedChannel(bisq.chat.protobuf.PublicModeratedChannel.newBuilder()
                        .setChannelName(displayName)
                        .setDescription(description)
                        .setChannelAdminId(channelAdminId)
                        .addAllChannelModeratorIds(channelModeratorIds))
                .build();
    }

    public static PublicModeratedChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                   bisq.chat.protobuf.PublicModeratedChannel proto) {
        return new PublicModeratedChannel(
                ChatDomain.fromProto(baseProto.getChatDomain()),
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                proto.getChannelAdminId(),
                new ArrayList<>(proto.getChannelModeratorIdsList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
    }

    @Override
    public void addChatMessage(PublicModeratedChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PublicModeratedChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PublicModeratedChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return displayName;
    }
}