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

import bisq.chat.ChannelNotificationType;
import bisq.chat.messages.PrivateChatMessage;
import bisq.common.observable.ObservableSet;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.Getter;

import java.util.Set;

@Getter
public abstract class PrivateChannel<T extends PrivateChatMessage> extends Channel<T> {
    private static final String CHANNEL_DELIMITER = "-";
    
    protected final UserProfile peer;
    protected final UserIdentity myProfile;

    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableSet<T> chatMessages = new ObservableSet<>();

    public PrivateChannel(String id,
                          UserProfile peer,
                          UserIdentity myProfile,
                          Set<T> chatMessages,
                          ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);
        this.peer = peer;
        this.myProfile = myProfile;
        this.chatMessages.addAll(chatMessages);
    }

    public static String createChannelId(String peersId, String myId) {
        // Need to have an ordering here, otherwise there would be 2 channelIds for the same participants
        if (peersId.compareTo(myId) < 0) {
            return peersId + CHANNEL_DELIMITER + myId;
        } else {
            return myId + CHANNEL_DELIMITER + peersId;
        }
    }
}
