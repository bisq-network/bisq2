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

package bisq.chat.bisq_easy.private_chats;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Getter
public class BisqEasyPrivateChatChannelSelectionService extends ChatChannelSelectionService {
    private final TwoPartyPrivateChatChannelService channelService;
    private final UserIdentityService userIdentityService;

    public BisqEasyPrivateChatChannelSelectionService(PersistenceService persistenceService,
                                                      TwoPartyPrivateChatChannelService channelService,
                                                      UserIdentityService userIdentityService) {
        super(persistenceService, ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT);

        this.userIdentityService = userIdentityService;
        this.channelService = channelService;
    }

    public CompletableFuture<Boolean> initialize() {
        if (selectedChannel.get() == null) {
            channelService.getDefaultChannel().ifPresent(this::selectChannel);
        }
        return super.initialize();
    }

    @Override
    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel != null) {
            PrivateChatChannel<?> privateChatChannel = (PrivateChatChannel<?>) chatChannel;
            userIdentityService.selectChatUserIdentity(privateChatChannel.getMyUserIdentity());
        }
        super.selectChannel(chatChannel);
    }

    @Override
    protected Stream<ChatChannel<?>> getAllChatChannels() {
        // fixme (low prio): cannot return publicChatChannelService.getChannels().stream() due type issues
        return Stream.concat(channelService.getChannels().stream(), Stream.empty());
    }
}