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

package bisq.chat.priv;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChatChannel<M extends PrivateChatMessage<?>> extends ChatChannel<M> {
    @Getter
    protected final UserIdentity myUserIdentity;
    // We persist the messages as they are NOT persisted in the P2P data store.
    @Getter
    protected final ObservableSet<M> chatMessages = new ObservableSet<>();

    private final transient Set<String> authorIdsSentLeaveMessage = new HashSet<>();

    public PrivateChatChannel(String id,
                              ChatChannelDomain chatChannelDomain,
                              UserIdentity myUserIdentity,
                              Set<M> chatMessages,
                              ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, chatChannelNotificationType);

        this.myUserIdentity = myUserIdentity;

        chatMessages.forEach(this::addChatMessage);
    }

    @Override
    public boolean addChatMessage(M chatMessage) {
        boolean changed = super.addChatMessage(chatMessage);
        if (changed) {
            // We might get normal message and leave message out of order (e.g. leave before normal msg).
            // For that case we check if we have any leave message of that author already received.
            // We do not support joining the same channel after leaving.
            boolean isLeaveMessage = chatMessage.getChatMessageType() == ChatMessageType.LEAVE;
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            if (isLeaveMessage) {
                authorIdsSentLeaveMessage.add(authorUserProfileId);
            }
            if (isLeaveMessage || authorIdsSentLeaveMessage.contains(authorUserProfileId)) {
                userProfileIdsOfActiveParticipants.remove(authorUserProfileId);
            } else {
                userProfileIdsOfActiveParticipants.add(authorUserProfileId);
            }
        }
        return changed;
    }

    // Called when removing expired messages. We do not support deleting private messages
    public boolean removeChatMessage(M chatMessage) {
        return super.removeChatMessage(chatMessage);
    }
}
