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

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.PrivateChatMessage;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.identity.UserIdentity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PrivateChatChannel<M extends PrivateChatMessage> extends ChatChannel<M> {
    protected final UserIdentity myUserIdentity;
    // We persist the messages as they are NOT persisted in the P2P data store.
    protected final ObservableSet<M> chatMessages = new ObservableSet<>();

    public PrivateChatChannel(String id,
                              ChatChannelDomain chatChannelDomain,
                              UserIdentity myUserIdentity,
                              List<M> chatMessages,
                              ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, chatChannelNotificationType);

        this.myUserIdentity = myUserIdentity;
        this.chatMessages.addAll(chatMessages);

        userProfileIdsOfParticipants.add(myUserIdentity.getUserProfile().getId());
    }
}
