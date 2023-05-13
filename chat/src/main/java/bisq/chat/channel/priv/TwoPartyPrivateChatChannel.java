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

package bisq.chat.channel.priv;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.TwoPartyPrivateChatMessage;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class TwoPartyPrivateChatChannel extends PrivateChatChannel<TwoPartyPrivateChatMessage> {
    // Channel id must be deterministic, so we sort both userIds and use that order for the concatenated string.
    public static String createId(ChatChannelDomain ChatChannelDomain, String userProfileId1, String userProfileId2) {
        List<String> userIds = new ArrayList<>(List.of(userProfileId1, userProfileId2));
        Collections.sort(userIds);
        return ChatChannelDomain.name().toLowerCase() + "." + userIds.get(0) + "." + userIds.get(1);
    }

    @Getter
    private final UserProfile peer;

    public TwoPartyPrivateChatChannel(UserProfile peer,
                                      UserIdentity myUserIdentity,
                                      ChatChannelDomain chatChannelDomain) {
        this(createId(chatChannelDomain, peer.getId(), myUserIdentity.getId()),
                chatChannelDomain,
                peer,
                myUserIdentity,
                new ArrayList<>(),
                ChatChannelNotificationType.ALL
        );
    }

    private TwoPartyPrivateChatChannel(String id,
                                       ChatChannelDomain chatChannelDomain,
                                       UserProfile peer,
                                       UserIdentity myUserIdentity,
                                       List<TwoPartyPrivateChatMessage> chatMessages,
                                       ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, myUserIdentity, chatMessages, chatChannelNotificationType);
        this.peer = peer;

        userProfileIdsOfParticipants.add(peer.getId());
    }

    @Override
    public bisq.chat.protobuf.ChatChannel toProto() {
        return getChannelBuilder().setTwoPartyPrivateChatChannel(bisq.chat.protobuf.TwoPartyPrivateChatChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyUserIdentity(myUserIdentity.toProto())
                        .addAllChatMessages(chatMessages.stream()
                                .map(TwoPartyPrivateChatMessage::toChatMessageProto)
                                .collect(Collectors.toList())))
                .build();
    }

    public static TwoPartyPrivateChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                       bisq.chat.protobuf.TwoPartyPrivateChatChannel proto) {
        TwoPartyPrivateChatChannel twoPartyPrivateChatChannel = new TwoPartyPrivateChatChannel(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(TwoPartyPrivateChatMessage::fromProto)
                        .collect(Collectors.toList()),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType())
        );
        twoPartyPrivateChatChannel.getSeenChatMessageIds().addAll(new HashSet<>(baseProto.getSeenChatMessageIdsList()));
        return twoPartyPrivateChatChannel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
        return getPeer().getUserName();
    }
}