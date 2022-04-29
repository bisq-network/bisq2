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
import bisq.social.user.ChatUserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PublicDiscussionChannel extends Channel<PublicDiscussionChatMessage> implements PublicChannel {

    public enum ChannelId{
        BISQ_ID,
        BITCOIN_ID,
        MONERO_ID,
        PRICE_ID,
        ECONOMY_ID,
        OFF_TOPIC_ID

    }
    private final String channelName;
    private final String description;
    private final ChatUserProfile channelAdmin;
    private final Set<ChatUserProfile> channelModerators;
    private transient final ObservableSet<PublicDiscussionChatMessage> chatMessages = new ObservableSet<>();

    public PublicDiscussionChannel(String id,
                                   String channelName,
                                   String description,
                                   ChatUserProfile channelAdmin,
                                   Set<ChatUserProfile> channelModerators
    ) {
        this(id, channelName,
                description,
                channelAdmin,
                channelModerators,
                NotificationSetting.MENTION
        );
    }

    private PublicDiscussionChannel(String id,
                                    String channelName,
                                    String description,
                                    ChatUserProfile channelAdmin,
                                    Set<ChatUserProfile> channelModerators,
                                    NotificationSetting notificationSetting) {
        super(id, notificationSetting);

        this.channelName = channelName;
        this.description = description;
        this.channelAdmin = channelAdmin;
        this.channelModerators = channelModerators;
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder()
                .setPublicDiscussionChannel(bisq.social.protobuf.PublicDiscussionChannel.newBuilder()
                        .setChannelName(channelName)
                        .setDescription(description)
                        .setChannelAdmin(channelAdmin.toProto())
                        .addAllChannelModerators(channelModerators.stream()
                                .map(ChatUserProfile::toProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static PublicDiscussionChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                                    bisq.social.protobuf.PublicDiscussionChannel proto) {
        return new PublicDiscussionChannel(
                baseProto.getId(),
                proto.getChannelName(),
                proto.getDescription(),
                ChatUserProfile.fromProto(proto.getChannelAdmin()),
                proto.getChannelModeratorsList().stream().map(ChatUserProfile::fromProto).collect(Collectors.toSet()),
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