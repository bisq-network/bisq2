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
import bisq.common.observable.Observable;
import bisq.common.util.StringUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Getter
public class ChannelSelectionService implements PersistenceClient<ChannelSelectionStore> {
    private final ChannelSelectionStore persistableStore = new ChannelSelectionStore();
    private final Persistence<ChannelSelectionStore> persistence;
    private final PrivateChannelService privateChannelService;
    private final PublicChannelService publicChannelService;
    private final Observable<Channel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public ChannelSelectionService(PersistenceService persistenceService,
                                   PrivateChannelService privateChannelService,
                                   PublicChannelService publicChannelService,
                                   ChannelDomain channelDomain) {
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                StringUtils.capitalize(channelDomain.name()) + "ChannelSelectionStore",
                persistableStore);
        this.privateChannelService = privateChannelService;
        this.publicChannelService = publicChannelService;
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
    public void onPersistedApplied(ChannelSelectionStore persisted) {
        applySelectedChannel();
    }

    public void selectChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateChannel) {
            privateChannelService.removeExpiredMessages((PrivateChannel) channel);
        }

        persistableStore.setSelectedChannelId(channel != null ? channel.getId() : null);
        persist();

        applySelectedChannel();
    }

    private void applySelectedChannel() {
        Stream<Channel<?>> stream = Stream.concat(publicChannelService.getChannels().stream(),
                privateChannelService.getChannels().stream());
        selectedChannel.set(stream
                .filter(channel -> channel.getId().equals(persistableStore.getSelectedChannelId()))
                .findAny()
                .orElse(null));
    }

    public void reportUserProfile(UserProfile userProfile, String reason) {
        //todo report user to admin and moderators, add reason
        log.info("called reportChatUser {} {}", userProfile, reason);
    }

    private void maybeSelectChannels() {
        if (getSelectedChannel().get() == null) {
            publicChannelService.getChannels().stream().findAny().ifPresent(this::selectChannel);
        }
        persist();
    }
}