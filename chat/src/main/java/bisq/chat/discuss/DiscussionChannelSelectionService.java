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

package bisq.chat.discuss;

import bisq.chat.channel.Channel;
import bisq.chat.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Getter
public class DiscussionChannelSelectionService implements PersistenceClient<DiscussionChannelSelectionStore> {
    private final DiscussionChannelSelectionStore persistableStore = new DiscussionChannelSelectionStore();
    private final Persistence<DiscussionChannelSelectionStore> persistence;
    private final PrivateDiscussionChannelService privateChannelService;
    private final PublicDiscussionChannelService publicChannelService;

    public DiscussionChannelSelectionService(PersistenceService persistenceService,
                                             PrivateDiscussionChannelService privateChannelService,
                                             PublicDiscussionChannelService publicChannelService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
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

    public void selectChannel(Channel<? extends ChatMessage> channel) {
        if (channel instanceof PrivateDiscussionChannel) {
            privateChannelService.removeExpiredMessages((PrivateDiscussionChannel) channel);
        }

        getSelectedChannel().set(channel);
        persist();
    }

    public Observable<Channel<? extends ChatMessage>> getSelectedChannel() {
        return persistableStore.getSelectedChannel();
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