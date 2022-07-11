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

package bisq.chat.channels;

import bisq.chat.messages.PrivateDiscussionChatMessage;
import bisq.common.observable.ObservableSet;
import bisq.chat.ChannelNotificationType;
import bisq.user.profile.UserProfile;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateDiscussionChannel extends Channel<PrivateDiscussionChatMessage> implements PrivateChannel {
    private final UserProfile peer;
    private final UserIdentity myProfile;

    // We persist the messages as they are NOT persisted in the P2P data store.
    private final ObservableSet<PrivateDiscussionChatMessage> chatMessages = new ObservableSet<>();

    public PrivateDiscussionChannel(UserProfile peer, UserIdentity myProfile) {
        this(PrivateChannel.createChannelId(peer.getId(), myProfile.getId()),
                peer,
                myProfile,
                ChannelNotificationType.ALL,
                new HashSet<>());
    }

    public PrivateDiscussionChannel(String id, UserProfile peer, UserIdentity myProfile) {
        this(id, peer, myProfile, ChannelNotificationType.ALL, new HashSet<>());
    }

    private PrivateDiscussionChannel(String id,
                                     UserProfile peer,
                                     UserIdentity myProfile,
                                     ChannelNotificationType channelNotificationType,
                                     Set<PrivateDiscussionChatMessage> chatMessages) {
        super(id, channelNotificationType);
        this.peer = peer;
        this.myProfile = myProfile;
        this.chatMessages.addAll(chatMessages);
    }

    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateDiscussionChannel(bisq.chat.protobuf.PrivateDiscussionChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream().map(this::getChatMessageProto).collect(Collectors.toList())))
                .build();
    }

    public static PrivateDiscussionChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                     bisq.chat.protobuf.PrivateDiscussionChannel proto) {
        return new PrivateDiscussionChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()),
                proto.getChatMessagesList().stream()
                        .map(PrivateDiscussionChatMessage::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    protected bisq.chat.protobuf.ChatMessage getChatMessageProto(PrivateDiscussionChatMessage chatMessage) {
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