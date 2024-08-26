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

package bisq.chat;

import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.persistence.PersistableStore;
import bisq.persistence.PersistenceClient;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class ChatChannelService<M extends ChatMessage, C extends ChatChannel<M>, S extends PersistableStore<S>>
        implements Service, PersistenceClient<S> {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final ChatChannelDomain chatChannelDomain;
    protected final BannedUserService bannedUserService;

    public ChatChannelService(NetworkService networkService,
                              UserService userService,
                              ChatChannelDomain chatChannelDomain) {
        this.networkService = networkService;
        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();
        bannedUserService = userService.getBannedUserService();
        this.chatChannelDomain = chatChannelDomain;
    }

    public void setChatChannelNotificationType(ChatChannel<? extends ChatMessage> chatChannel,
                                               ChatChannelNotificationType chatChannelNotificationType) {
        synchronized (this) {
            chatChannel.getChatChannelNotificationType().set(chatChannelNotificationType);
        }
        persist();
    }

    public void addMessage(M message, C channel) {
        if (bannedUserService.isUserProfileBanned(message.getAuthorUserProfileId())) {
            log.warn("Message ignored as sender is banned");
            return;
        }
        synchronized (getPersistableStore()) {
            channel.addChatMessage(message);
        }
        persist();
    }

    protected boolean isValid(M message) {
        if (bannedUserService.isUserProfileBanned(message.getAuthorUserProfileId())) {
            log.warn("Message invalid as sender is banned");
            return false;
        }
        return true;
    }

    protected boolean canHandleChannelDomain(M message) {
        return message.getChatChannelDomain() == chatChannelDomain;
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

    public abstract ObservableSet<C> getChannels();

    protected void doRemoveExpiredMessages(C channel) {
        Set<M> toRemove = channel.getChatMessages().stream()
                .filter(ChatMessage::isExpired)
                .collect(Collectors.toSet());
        if (!toRemove.isEmpty()) {
            synchronized (getPersistableStore()) {
                channel.removeChatMessages(toRemove);
            }
            persist();
        }
    }

    public Optional<C> findChannel(String channelId) {
        return getChannels().stream()
                .filter(channel -> channel.getId().equals(channelId))
                .findAny();
    }

    public Optional<C> getDefaultChannel() {
        return getChannels().stream().findFirst();
    }

    protected abstract String getChannelTitlePostFix(ChatChannel<? extends ChatMessage> chatChannel);

    protected void addMessageReaction(ChatMessageReaction chatMessageReaction, M message) {
        if (bannedUserService.isUserProfileBanned(chatMessageReaction.getUserProfileId())) {
            log.warn("Reaction ignored as sender is banned.");
            return;
        }
        synchronized (getPersistableStore()) {
            message.addChatMessageReaction(chatMessageReaction);
        }
        persist();
    }

    protected void removeMessageReaction(ChatMessageReaction chatMessageReaction, M message) {
        synchronized (getPersistableStore()) {
            message.getChatMessageReactions().remove(chatMessageReaction);
        }
        persist();
    }
}
