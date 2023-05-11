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

import bisq.chat.message.ChatMessage;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableArray;
import bisq.network.NetworkService;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class ChatChannelService<M extends ChatMessage, C extends ChatChannel<M>, S extends PersistableStore<S>> implements Service, PersistenceClient<S> {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChatChannelDomain chatChannelDomain;

    public ChatChannelService(NetworkService networkService,
                              UserIdentityService userIdentityService,
                              UserProfileService userProfileService,
                              ChatChannelDomain chatChannelDomain) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
        this.chatChannelDomain = chatChannelDomain;
    }

    public void setChatChannelNotificationType(ChatChannel<? extends ChatMessage> chatChannel,
                                               ChatChannelNotificationType chatChannelNotificationType) {
        chatChannel.getChatChannelNotificationType().set(chatChannelNotificationType);
        persist();
    }

    public void addMessage(M message, C channel) {
        synchronized (getPersistableStore()) {
            channel.addChatMessage(message);
        }
        persist();
    }

    public void updateSeenChatMessageIds(ChatChannel<? extends ChatMessage> chatChannel) {
        synchronized (getPersistableStore()) {
            chatChannel.setAllMessagesSeen();
        }
        persist();
    }

    public Optional<C> findChannel(ChatMessage chatMessage) {
        return findChannel(chatMessage.getChannelId());
    }

    public String getChannelTitle(ChatChannel<? extends ChatMessage> chatChannel) {
        return chatChannel.getDisplayString() + getChannelTitlePostFix(chatChannel);
    }

    public void removeExpiredMessages(ChatChannel<? extends ChatMessage> chatChannel) {
        findChannel(chatChannel.getId()).ifPresent(this::doRemoveExpiredMessages);
    }

    public abstract void leaveChannel(C channel);

    public abstract ObservableArray<C> getChannels();

    protected void doRemoveExpiredMessages(C channel) {
        Set<M> toRemove = channel.getChatMessages().stream()
                .filter(ChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (getPersistableStore()) {
                channel.removeChatMessages(toRemove);
                channel.setAllMessagesSeen();
            }
            persist();
        }
    }

    protected Optional<C> findChannel(String id) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(id))
                .findAny();
    }

    public Optional<C> getDefaultChannel() {
        return getChannels().stream().findFirst();
    }

    protected abstract String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel);
}