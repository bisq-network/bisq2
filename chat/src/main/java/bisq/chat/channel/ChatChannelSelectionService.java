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

import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
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
public class ChatChannelSelectionService implements PersistenceClient<ChatChannelSelectionStore> {
    private final ChatChannelSelectionStore persistableStore = new ChatChannelSelectionStore();
    private final Persistence<ChatChannelSelectionStore> persistence;
    private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
    private final CommonPublicChatChannelService commonPublicChatChannelService;
    private final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public ChatChannelSelectionService(PersistenceService persistenceService,
                                       TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService,
                                       CommonPublicChatChannelService commonPublicChatChannelService,
                                       ChatChannelDomain chatChannelDomain) {
        persistence = persistenceService.getOrCreatePersistence(this,
                "db",
                StringUtils.capitalize(chatChannelDomain.name()) + "ChannelSelectionStore",
                persistableStore);
        this.twoPartyPrivateChatChannelService = twoPartyPrivateChatChannelService;
        this.commonPublicChatChannelService = commonPublicChatChannelService;
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
        applySelectedChannel();
    }

    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            twoPartyPrivateChatChannelService.removeExpiredMessages((TwoPartyPrivateChatChannel) chatChannel);
        }

        persistableStore.setSelectedChannelId(chatChannel != null ? chatChannel.getId() : null);
        persist();

        applySelectedChannel();
    }

    private void applySelectedChannel() {
        Stream<ChatChannel<?>> stream = Stream.concat(commonPublicChatChannelService.getChannels().stream(),
                twoPartyPrivateChatChannelService.getChannels().stream());
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
            commonPublicChatChannelService.getChannels().stream().findAny().ifPresent(this::selectChannel);
        }
        persist();
    }
}