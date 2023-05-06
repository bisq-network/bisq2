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
import bisq.chat.message.ChatMessage;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.PublicChatMessage;
import bisq.common.data.Pair;
import bisq.common.observable.collection.ObservableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class PublicChatChannel<M extends PublicChatMessage> extends ChatChannel<M> {
    // Transient because we do not persist the messages as they are persisted in the P2P data store.
    protected transient final ObservableSet<M> chatMessages = new ObservableSet<>();

    public PublicChatChannel(ChatChannelDomain chatChannelDomain, String channelName, ChatChannelNotificationType chatChannelNotificationType) {
        super(chatChannelDomain, channelName, chatChannelNotificationType);
    }

    @Override
    public void addChatMessage(M chatMessage) {
        chatMessages.add(chatMessage);
    }

    public void removeChatMessage(M chatMessage) {
        chatMessages.remove(chatMessage);
    }

    public void removeChatMessages(Collection<M> messages) {
        chatMessages.removeAll(messages);
    }

    //todo
    @Override
    public Set<String> getMembers() {
        Map<String, List<ChatMessage>> chatMessagesByAuthor = new HashMap<>();
        getChatMessages().forEach(chatMessage -> {
            String authorId = chatMessage.getAuthorId();
            chatMessagesByAuthor.putIfAbsent(authorId, new ArrayList<>());
            chatMessagesByAuthor.get(authorId).add(chatMessage);
        });

        return chatMessagesByAuthor.entrySet().stream()
                .map(entry -> {
                    List<ChatMessage> chatMessages = entry.getValue();
                    chatMessages.sort(Comparator.comparing(chatMessage -> new Date(chatMessage.getDate())));
                    ChatMessage lastChatMessage = chatMessages.get(chatMessages.size() - 1);
                    return new Pair<>(entry.getKey(), lastChatMessage);
                })
                .filter(pair -> pair.getSecond().getChatMessageType() != ChatMessageType.LEAVE)
                .map(Pair::getFirst)
                .collect(Collectors.toSet());
    }

}
