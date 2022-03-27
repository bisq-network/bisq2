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

import bisq.security.DigestUtil;
import bisq.social.user.ChatUser;
import bisq.social.user.UserNameGenerator;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class PrivateChannel extends Channel<PrivateChatMessage> {
    private final ChatUser peer;
    private final UserProfile senderProfile;

    public PrivateChannel(String id, ChatUser peer,UserProfile senderProfile) {
        super(id);
        this.peer = peer;
        this.senderProfile = senderProfile;
    }

    public static UserProfile findSenderProfileFromChannelId(String id, ChatUser peer, UserProfileService userProfileService) {
        String[] chatNames = id.split("@PC@");
        if (chatNames == null || chatNames.length != 2) {
            throw new RuntimeException("malformed channel id"); // TODO figure out how error handling works here
        }
        String peerName = peer.getUserName();
        if (!peerName.equals(chatNames[0]) && !peerName.equals(chatNames[1])) {
            throw new RuntimeException("channel id and peer's userName dont fit");
        }
        String myName = peerName.equals(chatNames[0]) ? chatNames[1] : chatNames[0];
        // now go through all my identities and get the one with the right Name
        // it should be ensurd by the NameGenerator that  they are unique!

        return userProfileService.getPersistableStore().getUserProfiles().stream()
                .filter(up->up.userName().equals(myName))
                .findAny()
                .orElseThrow(); // TODO how to report errors
    }

    public static String createChannelId(ChatUser peer,UserProfile senderProfile) {
        String peerName = peer.getUserName();
        String myName = senderProfile.userName();
        String channelId;
        if (peerName.compareTo(myName) < 0) {
            return peerName + "@PC@" + myName;
        } else { // need to have an ordering here, otherwise there would be 2 channelIDs for the same participants
            return myName + "@PC@" + peerName;
        }
    }

    public String getChannelName() {
        return peer.getUserName() + " - " + senderProfile.userName();
    }
}