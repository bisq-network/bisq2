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
import bisq.social.chat.messages.PrivateDiscussionChatMessage;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PrivateDiscussionChannel extends Channel<PrivateDiscussionChatMessage> implements PrivateChannel {
    private final ChatUser peer;
    private final ChatUserIdentity myProfile;
    private final ObservableSet<PrivateDiscussionChatMessage> chatMessages = new ObservableSet<>();

    public PrivateDiscussionChannel(ChatUser peer, ChatUserIdentity myProfile) {
        this(PrivateChannel.createChannelId(peer.getNym(), myProfile.getProfileId()),
                peer,
                myProfile,
                NotificationSetting.ALL,
                new HashSet<>());
    }

    public PrivateDiscussionChannel(String id, ChatUser peer, ChatUserIdentity myProfile) {
        this(id, peer, myProfile, NotificationSetting.ALL, new HashSet<>());
    }

    private PrivateDiscussionChannel(String id,
                                     ChatUser peer,
                                     ChatUserIdentity myProfile,
                                     NotificationSetting notificationSetting,
                                     Set<PrivateDiscussionChatMessage> chatMessages) {
        super(id, notificationSetting);
        this.peer = peer;
        this.myProfile = myProfile;
        this.chatMessages.addAll(chatMessages);
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateDiscussionChannel(bisq.social.protobuf.PrivateDiscussionChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyChatUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList())))
                .build();
    }

    public static PrivateDiscussionChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                                     bisq.social.protobuf.PrivateDiscussionChannel proto) {
        return new PrivateDiscussionChannel(
                baseProto.getId(),
                ChatUser.fromProto(proto.getPeer()),
                ChatUserIdentity.fromProto(proto.getMyChatUserIdentity()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()),
                proto.getChatMessagesList().stream()
                        .map(PrivateDiscussionChatMessage::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PrivateDiscussionChatMessage chatMessage) {
        return chatMessage.toChatMessageProto();
    }

    @Override
    public void addChatMessage(PrivateDiscussionChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(PrivateDiscussionChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<PrivateDiscussionChatMessage> removeMessages) {
        chatMessages.removeAll(removeMessages);
    }

    @Override
    public String getDisplayString() {
        return peer.getUserName();
    }
}