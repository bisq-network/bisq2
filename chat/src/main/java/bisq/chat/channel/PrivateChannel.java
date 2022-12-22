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
import bisq.common.data.ByteArray;
import bisq.common.data.Pair;
import bisq.common.observable.ObservableArray;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChannel<T extends PrivateChatMessage> extends Channel<T> {

    public static String createChannelId(Pair<String, String> userIds) {
        String userId1 = userIds.getFirst();
        String userId2 = userIds.getSecond();
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "-" + userId2;
        } else {
            return userId2 + "-" + userId1;
        }
    }

    protected final UserIdentity myUserIdentity;

    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableArray<T> chatMessages = new ObservableArray<>();

    public PrivateChannel(String id,
                          UserIdentity myUserIdentity,
                          List<T> chatMessages,
                          ChannelNotificationType channelNotificationType) {
        super(id, channelNotificationType);

        this.myUserIdentity = myUserIdentity;
        this.chatMessages.addAll(chatMessages);
        this.chatMessages.sort(Comparator.comparing((T e) -> new ByteArray(e.serialize())));
    }

    public abstract UserProfile getPeer();
}
