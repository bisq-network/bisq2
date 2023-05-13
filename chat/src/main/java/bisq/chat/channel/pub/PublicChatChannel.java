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

package bisq.chat.channel.pub;

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.PublicChatMessage;
import bisq.common.observable.collection.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PublicChatChannel<M extends PublicChatMessage> extends ChatChannel<M> {
    // Transient because we do not persist the messages as they are persisted in the P2P data store.
    protected transient final ObservableSet<M> chatMessages = new ObservableSet<>();

    public PublicChatChannel(String id,
                             ChatChannelDomain chatChannelDomain,
                             ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, chatChannelNotificationType);
    }

    @Override
    public void addChatMessage(M chatMessage) {
        chatMessages.add(chatMessage);

        userProfileIdsOfParticipants.add(chatMessage.getAuthorUserProfileId());
    }


    public void removeChatMessage(M chatMessage) {
        chatMessages.remove(chatMessage);

        // If no more message of that author we remove them from the userProfileIdsOfParticipants
        if (chatMessages.stream().noneMatch(message -> message.getAuthorUserProfileId().equals(chatMessage.getAuthorUserProfileId()))) {
            userProfileIdsOfParticipants.remove(chatMessage.getAuthorUserProfileId());
        }
    }

    public void removeChatMessages(Collection<M> messages) {
        chatMessages.removeAll(messages);
    }
}
