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

package bisq.chat.common;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * It manages the selection of the public channels and the private channels associated with the domain
 */
@Slf4j
@Getter
public class CommonChannelSelectionService extends ChatChannelSelectionService {
    private final Optional<TwoPartyPrivateChatChannelService> privateChatChannelService;
    private final CommonPublicChatChannelService publicChatChannelService;
    private final UserIdentityService userIdentityService;
    private Optional<TwoPartyPrivateChatChannel> lastSelectedPrivateChannel = Optional.empty();

    public CommonChannelSelectionService(PersistenceService persistenceService,
                                         Optional<TwoPartyPrivateChatChannelService> privateChatChannelService,
                                         CommonPublicChatChannelService publicChatChannelService,
                                         ChatChannelDomain chatChannelDomain,
                                         UserIdentityService userIdentityService) {
        super(persistenceService, chatChannelDomain);

        this.userIdentityService = userIdentityService;
        this.privateChatChannelService = privateChatChannelService;
        this.publicChatChannelService = publicChatChannelService;
    }

    public CompletableFuture<Boolean> initialize() {
        if (selectedChannel.get() == null) {
            publicChatChannelService.getDefaultChannel().ifPresent(this::selectChannel);
        }
        privateChatChannelService.ifPresent(service -> lastSelectedPrivateChannel = service.getChannels().stream().findFirst());

        return super.initialize();
    }

    @Override
    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        // Assume only private channels can be set to null
        if (chatChannel == null) {
            lastSelectedPrivateChannel = Optional.empty();
        } else if (chatChannel instanceof PublicChatChannel) {
            publicChatChannelService.removeExpiredMessages(chatChannel);
        } else if (chatChannel instanceof TwoPartyPrivateChatChannel privateChatChannel) {
            lastSelectedPrivateChannel = Optional.of(privateChatChannel);
            userIdentityService.selectChatUserIdentity(privateChatChannel.getMyUserIdentity());
        }
        super.selectChannel(chatChannel);
    }

    @Override
    protected Stream<ChatChannel<?>> getAllChatChannels() {
        return Stream.concat(publicChatChannelService.getChannels().stream(),
                privateChatChannelService.stream().flatMap(service -> service.getChannels().stream()));
    }
}