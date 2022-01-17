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

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

// TODO not used yet. Will require more work on the chatUser and chatIdentity management. 
@Getter
public class PublicChannel extends Channel {
    private final String channelName;

    // user can change their chatIdentity in a PublicChannel
    @Nullable
    @Setter
    private ChatIdentity chatIdentity;
    // Can be empty
    private final Set<ChatPeer> chatPeers = new HashSet<>();

    public PublicChannel(String id, String channelName) {
        super(id);

        this.channelName = channelName;
    }

    public Optional<ChatIdentity> getChatIdentity() {
        return Optional.ofNullable(chatIdentity);
    }
}