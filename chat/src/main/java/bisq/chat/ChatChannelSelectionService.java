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

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.StringUtils;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@Getter
public abstract class ChatChannelSelectionService extends RateLimitedPersistenceClient<ChatChannelSelectionStore> implements Service {
    protected final ChatChannelSelectionStore persistableStore = new ChatChannelSelectionStore();
    protected final Persistence<ChatChannelSelectionStore> persistence;
    protected final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public ChatChannelSelectionService(PersistenceService persistenceService,
                                       ChatChannelDomain chatChannelDomain) {
        String prefix = StringUtils.capitalize(
                StringUtils.snakeCaseToCamelCase(chatChannelDomain.name().toLowerCase(Locale.ROOT), Locale.ROOT),
                Locale.ROOT);
        persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.SETTINGS,
                prefix + "ChannelSelectionStore",
                persistableStore);
    }

    @Override
    public void onPersistedApplied(ChatChannelSelectionStore persisted) {
        selectedChannel.set(getAllChatChannels()
                .filter(channel -> channel.getId().equals(persistableStore.getSelectedChannelId()))
                .findAny()
                .orElse(null));
    }

    public void selectChannel(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
        persistableStore.setSelectedChannelId(chatChannel != null ? chatChannel.getId() : null);
        persist();

        selectedChannel.set(chatChannel);
    }

    protected abstract Stream<ChatChannel<?>> getAllChatChannels();
}