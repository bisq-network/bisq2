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
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Getter
public class CommonChannelSelectionService extends ChatChannelSelectionService {
    private final TwoPartyPrivateChatChannelService privateChatChannelService;
    private final PublicChatChannelService<?, ?, ?> publicChatChannelService;
    private final UserIdentityService userIdentityService;

    public CommonChannelSelectionService(PersistenceService persistenceService,
                                         TwoPartyPrivateChatChannelService privateChatChannelService,
                                         PublicChatChannelService<?, ?, ?> publicChatChannelService,
                                         ChatChannelDomain chatChannelDomain,
                                         UserIdentityService userIdentityService) {
        super(persistenceService, chatChannelDomain);

        this.userIdentityService = userIdentityService;
        this.privateChatChannelService = privateChatChannelService;
        this.publicChatChannelService = publicChatChannelService;
    }

    public CompletableFuture<Boolean> initialize() {
        publicChatChannelService.getDefaultChannel().ifPresent(this::selectChannel);
        return super.initialize();
    }

    @Override
    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof PublicChatChannel) {
            publicChatChannelService.removeExpiredMessages(chatChannel);
        } else if (chatChannel instanceof PrivateChatChannel) {
            PrivateChatChannel<?> privateChatChannel = (PrivateChatChannel<?>) chatChannel;
            userIdentityService.selectChatUserIdentity(privateChatChannel.getMyUserIdentity());
        }
        super.selectChannel(chatChannel);
    }

    @Override
    protected Stream<ChatChannel<?>> getAllChatChannels() {
        return Stream.concat(publicChatChannelService.getChannels().stream(),
                privateChatChannelService.getChannels().stream());
    }

    @Override
    public void maybeSelectFirstChannel() {
        if (!publicChatChannelService.getChannels().isEmpty()) {
            selectChannel(publicChatChannelService.getChannels().stream().findFirst().orElse(null));
        } else if (!privateChatChannelService.getChannels().isEmpty()) {
            selectChannel(privateChatChannelService.getChannels().stream().findFirst().orElse(null));
        } else {
            selectChannel(null);
        }
    }
}