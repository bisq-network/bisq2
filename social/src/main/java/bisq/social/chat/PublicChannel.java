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

import bisq.social.userprofile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

// TODO not used yet. Will require more work on the chatUser and chatIdentity management. 
@Getter
@ToString
@EqualsAndHashCode
public class PublicChannel extends Channel {
    private final String channelName;
    private final UserProfile channelOwner;

    // user can change their chatIdentity in a PublicChannel
  //  @Nullable
  //  @Setter
   // private ChatIdentity chatIdentity;
    // Can be empty
  //  private final Set<ChatPeer> chatPeers = new HashSet<>();

    public PublicChannel(String id, String channelName, UserProfile channelOwner) {
        super(id);

        this.channelName = channelName;
        this.channelOwner = channelOwner;
    }

 //   public Optional<ChatIdentity> getChatIdentity() {
      //  return Optional.ofNullable(chatIdentity);
   // }
}