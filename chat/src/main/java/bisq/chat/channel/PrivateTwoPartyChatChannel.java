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

package bisq.chat.channel;

import bisq.chat.message.TwoPartyPrivateChatMessage;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class PrivateTwoPartyChatChannel extends PrivateChatChannel<TwoPartyPrivateChatMessage> {
    // Channel name must be deterministic, so we sort both userIds and use that order for the concatenated string.
    private static String createChannelName(String userId1, String userId2) {
        List<String> userIds = new ArrayList<>(List.of(userId1, userId2));
        Collections.sort(userIds);
        return userIds.get(0) + "." + userIds.get(1);
    }

    public PrivateTwoPartyChatChannel(UserProfile peer, UserIdentity myUserIdentity, ChannelDomain channelDomain) {
        this(channelDomain,
                createChannelName(peer.getId(), myUserIdentity.getId()),
                peer,
                myUserIdentity,
                new ArrayList<>(),
                ChannelNotificationType.ALL
        );
    }

    private PrivateTwoPartyChatChannel(ChannelDomain channelDomain,
                                       String channelName,
                                       UserProfile peer,
                                       UserIdentity myProfile,
                                       List<TwoPartyPrivateChatMessage> chatMessages,
                                       ChannelNotificationType channelNotificationType) {
        super(channelDomain, channelName, myProfile, chatMessages, channelNotificationType);

        addChannelMember(new ChannelMember(ChannelMember.Type.PEER, peer));
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        return getChannelBuilder().setPrivateTwoPartyChatChannel(bisq.chat.protobuf.PrivateTwoPartyChatChannel.newBuilder()
                        .setPeer(getPeer().toProto())
                        .setMyUserIdentity(myUserIdentity.toProto())
                        .addAllChatMessages(chatMessages.stream()
                                .map(TwoPartyPrivateChatMessage::toChatMessageProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static PrivateTwoPartyChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                       bisq.chat.protobuf.PrivateTwoPartyChatChannel proto) {
        PrivateTwoPartyChatChannel privateTwoPartyChatChannel = new PrivateTwoPartyChatChannel(ChannelDomain.fromProto(baseProto.getChannelDomain()),
                baseProto.getChannelName(),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(TwoPartyPrivateChatMessage::fromProto)
                        .collect(Collectors.toList()),
                ChannelNotificationType.fromProto(baseProto.getChannelNotificationType())
        );
        privateTwoPartyChatChannel.getSeenChatMessageIds().addAll(new HashSet<>(baseProto.getSeenChatMessageIdsList()));
        return privateTwoPartyChatChannel;
    }

    public UserProfile getPeer() {
        checkArgument(getPeers().size() == 1, "peers.size must be 1 at PrivateTwoPartyChatChannel");
        return getPeers().get(0);
    }

    @Override
    public void addChatMessage(TwoPartyPrivateChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void removeChatMessage(TwoPartyPrivateChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void removeChatMessages(Collection<TwoPartyPrivateChatMessage> messages) {
        chatMessages.removeAll(messages);
    }

    @Override
    public String getDisplayString() {
        return getPeer().getUserName() + "-" + myUserIdentity.getUserName();
    }
}