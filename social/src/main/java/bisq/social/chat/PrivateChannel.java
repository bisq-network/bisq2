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

package bisq.social.chat;

import bisq.social.user.ChatUser;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true)
public class PrivateChannel extends Channel<PrivateChatMessage> {
    private static final String CHANNEL_DELIMITER = "@PC@";
    private final ChatUser peer;
    private final UserProfile myProfile;

    public PrivateChannel(String id, ChatUser peer, UserProfile myProfile) {
        this(id, peer, myProfile, NotificationSetting.ALL, new HashSet<>());
    }

    private PrivateChannel(String id,
                           ChatUser peer,
                           UserProfile myProfile,
                           NotificationSetting notificationSetting,
                           Set<PrivateChatMessage> chatMessages) {
        super(id, notificationSetting, chatMessages);
        this.peer = peer;
        this.myProfile = myProfile;
    }

    public bisq.social.protobuf.Channel toProto() {
        return getChannelBuilder().setPrivateChannel(bisq.social.protobuf.PrivateChannel.newBuilder()
                        .setPeer(peer.toProto())
                        .setMyProfile(myProfile.toProto()))
                .build();
    }

    public static PrivateChannel fromProto(bisq.social.protobuf.Channel baseProto,
                                           bisq.social.protobuf.PrivateChannel proto) {
        return new PrivateChannel(
                baseProto.getId(),
                ChatUser.fromProto(proto.getPeer()),
                UserProfile.fromProto(proto.getMyProfile()),
                NotificationSetting.fromProto(baseProto.getNotificationSetting()),
                baseProto.getChatMessagesList().stream()
                        .map(PrivateChatMessage::fromProto)
                        .collect(Collectors.toSet()));
    }

    @Override
    protected bisq.social.protobuf.ChatMessage getChatMessageProto(PrivateChatMessage chatMessage) {
        return chatMessage.toChatMessageProto();
    }

    public static UserProfile findMyProfileFromChannelId(String id, ChatUser peer, UserProfileService userProfileService) {
        String[] chatNames = id.split(CHANNEL_DELIMITER);
        if (chatNames.length != 2) {
            throw new RuntimeException("malformed channel id"); // TODO figure out how error handling works here
        }
        String peerName = peer.getProfileId();
        if (!peerName.equals(chatNames[0]) && !peerName.equals(chatNames[1])) {
            throw new RuntimeException("channel id and peer's profileId dont fit");
        }
        String myName = peerName.equals(chatNames[0]) ? chatNames[1] : chatNames[0];
        // now go through all my identities and get the one with the right Name
        // it should be ensured by the NameGenerator that  they are unique!
        return userProfileService.getPersistableStore().getUserProfiles().stream()
                .filter(up -> up.getProfileId().equals(myName))
                .findAny()
                .orElseThrow(); // TODO how to report errors
    }

    public static String createChannelId(ChatUser peer, UserProfile senderProfile) {
        String peerName = peer.getProfileId();
        String myName = senderProfile.getProfileId();
        if (peerName.compareTo(myName) < 0) {
            return peerName + CHANNEL_DELIMITER + myName;
        } else { // need to have an ordering here, otherwise there would be 2 channelIDs for the same participants
            return myName + CHANNEL_DELIMITER + peerName;
        }
    }

    public String getChannelName() {
        return peer.getProfileId();
    }
}