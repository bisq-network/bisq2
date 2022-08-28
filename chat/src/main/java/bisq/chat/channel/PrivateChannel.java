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

import bisq.chat.message.PrivateChatMessage;
import bisq.common.observable.ObservableSet;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChannel<T extends PrivateChatMessage> extends Channel<T> {
    protected final UserProfile peer;
    protected final UserIdentity myUserIdentity;

    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableSet<T> chatMessages = new ObservableSet<>();

    public PrivateChannel(String id,
                          UserProfile peer,
                          UserIdentity myUserIdentity,
                          Set<T> chatMessages,
                          ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);
        this.peer = peer;
        this.myUserIdentity = myUserIdentity;
        this.chatMessages.addAll(chatMessages);
    }

    public static String createChannelId(String peersId, String myId) {
        // Need to have an ordering here, otherwise there would be 2 channelIds for the same participants
        if (peersId.compareTo(myId) < 0) {
            return peersId + "-" + myId;
        } else {
            return myId + "-" + peersId;
        }
    }
}
