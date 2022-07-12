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

package bisq.chat.channel.priv.discuss;

import bisq.chat.ChannelNotificationType;
import bisq.chat.channel.priv.PrivateChannel;
import bisq.chat.message.PrivateDiscussionChatMessage;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateDiscussionChannel extends PrivateChannel<PrivateDiscussionChatMessage> {

    public PrivateDiscussionChannel(UserProfile peer, UserIdentity myProfile) {
        this(PrivateChannel.createChannelId(peer.getId(), myProfile.getId()),
                peer,
                myProfile,
                new HashSet<>(),
                ChannelNotificationType.ALL);
    }

    private PrivateDiscussionChannel(String id,
                                     UserProfile peer,
                                     UserIdentity myProfile,
                                     Set<PrivateDiscussionChatMessage> chatMessages,
                                     ChannelNotificationType channelNotificationType) {
        super(id, peer, myProfile, chatMessages, channelNotificationType);
    }

    @Override
    public bisq.chat.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateDiscussionChannel(bisq.chat.protobuf.PrivateDiscussionChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myProfile.toProto())
                        .addAllChatMessages(chatMessages.stream()
                                .map(PrivateDiscussionChatMessage::toChatMessageProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static PrivateDiscussionChannel fromProto(bisq.chat.protobuf.Channel baseProto,
                                                     bisq.chat.protobuf.PrivateDiscussionChannel proto) {
        return new PrivateDiscussionChannel(
                baseProto.getId(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(PrivateDiscussionChatMessage::fromProto)
                        .collect(Collectors.toSet()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType()));
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
        return peer.getUserName() + "-" + myProfile.getUserName();
    }
}