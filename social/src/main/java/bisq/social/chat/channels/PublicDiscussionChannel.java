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

package bisq.social.chat.channels;

import bisq.common.observable.ObservableSet;
import bisq.social.chat.NotificationSetting;
import bisq.social.chat.messages.PublicDiscussionChatMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PublicDiscussionChannel extends Channel<PublicDiscussionChatMessage> implements PublicChannel {

    public enum ChannelId {
        BISQ_ID,
        BITCOIN_ID,
        MONERO_ID,
        PRICE_ID,
        ECONOMY_ID,
        OFF_TOPIC_ID

    }

    private final String channelName;
    private final String description;
    private final String channelAdminId;
    private final Set<String> channelModeratorIds;
    
    // We do not persist the messages as they are persisted in the P2P data store.
    private transient final ObservableSet<PublicDiscussionChatMessage> chatMessages = new ObservableSet<>();

    public PublicDiscussionChannel(String id,
                                   String channelName,
                                   String description,
                                   String channelAdminId,
                                   Set<String> channelModeratorIds
    ) {
        this(id, channelName,
                description,
                channelAdminId,
                channelModeratorIds,
                NotificationSetting.MENTION
        );
    }

    private PublicDiscussionChannel(String id,
                                    String channelName,
                                    String description,
                                    String channelAdminId,
                                    Set<String> channelModeratorIds,
                                    NotificationSetting notificationSetting) {
        super(id, notificationSetting);

        this.channelName = channelName;
        this.description = description;
        this.channelAdminId = channelAdminId;
        this.channelModeratorIds = channelModeratorIds;
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder()
                .setPublicDiscussionChannel(bisq.social.protobuf.PublicDiscussionChannel.newBuilder()
                        .setChannelName(channelName)
                        .setDescription(description)
                        .setChannelAdminId(channelAdminId)
                        .addAllChannelModeratorIds(channelModeratorIds))
                .build();
    }

    public static PublicDiscussionChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                                    bisq.social.protobuf.PublicDiscussionChannel proto) {
        return new PublicDiscussionChannel(
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                proto.getChannelAdminId(),
                new HashSet<>(proto.getChannelModeratorIdsList()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()));
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PublicDiscussionChatMessage chatMessage) {
        return chatMessage.toProto();
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
}