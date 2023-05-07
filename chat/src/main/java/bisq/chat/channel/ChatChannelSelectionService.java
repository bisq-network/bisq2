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

import bisq.chat.channel.priv.PrivateChatChannelService;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.channel.pub.PublicChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.util.StringUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Getter
public class ChatChannelSelectionService implements PersistenceClient<ChatChannelSelectionStore> {
    protected final ChatChannelSelectionStore persistableStore = new ChatChannelSelectionStore();
    protected final Persistence<ChatChannelSelectionStore> persistence;
    protected final PrivateChatChannelService<?, ?, ?> privateChatChannelService;
    protected final PublicChatChannelService<?, ?, ?> publicChatChannelService;
    protected final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public ChatChannelSelectionService(PersistenceService persistenceService,
                                       PrivateChatChannelService<?, ?, ?> privateChatChannelService,
                                       PublicChatChannelService<?, ?, ?> publicChatChannelService,
                                       ChatChannelDomain chatChannelDomain) {
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                StringUtils.capitalize(chatChannelDomain.name()) + "ChannelSelectionStore",
                persistableStore);
        this.privateChatChannelService = privateChatChannelService;
        this.publicChatChannelService = publicChatChannelService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        maybeSelectChannels();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onPersistedApplied(ChatChannelSelectionStore persisted) {
        applyPersistedSelectedChannel();
    }

    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof PublicChatChannel) {
            publicChatChannelService.removeExpiredMessages((PublicChatChannel<?>) chatChannel);
        }

        persistableStore.setSelectedChannelId(chatChannel != null ? chatChannel.getId() : null);
        persist();

        applyPersistedSelectedChannel();
    }

    protected void applyPersistedSelectedChannel() {
        Stream<ChatChannel<?>> stream = Stream.concat(publicChatChannelService.getChannels().stream(),
                privateChatChannelService.getChannels().stream());
        selectedChannel.set(stream
                .filter(channel -> channel.getId().equals(persistableStore.getSelectedChannelId()))
                .findAny()
                .orElse(null));
    }

    protected void maybeSelectChannels() {
        if (selectedChannel.get() == null) {
            publicChatChannelService.getDefaultChannel().ifPresent(this::selectChannel);
        }
        persist();
    }
}