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

package bisq.chat.bisqeasy.channel;

import bisq.chat.bisqeasy.channel.offerbook.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.offerbook.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.channel.open_trades.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Getter
public class BisqEasyChatChannelSelectionService extends ChatChannelSelectionService {
    private final BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService;

    public BisqEasyChatChannelSelectionService(PersistenceService persistenceService,
                                               TwoPartyPrivateChatChannelService privateBisqEasyTwoPartyChannelService,
                                               BisqEasyPublicChatChannelService publicChatChannelService,
                                               BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService,
                                               UserIdentityService userIdentityService) {
        super(persistenceService,
                privateBisqEasyTwoPartyChannelService,
                publicChatChannelService,
                ChatChannelDomain.BISQ_EASY,
                userIdentityService);
        this.bisqEasyPrivateTradeChatChannelService = bisqEasyPrivateTradeChatChannelService;
    }

    @Override
    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectChannel(chatChannel);
    }

    @Override
    public void maybeSelectFirstChannel() {
        Set<BisqEasyPublicChatChannel> visibleBisqEasyPublicChatChannels = getVisibleChannels();
        if (!visibleBisqEasyPublicChatChannels.isEmpty()) {
            selectChannel(getVisibleChannels().stream().findFirst().orElse(null));
        } else if (!bisqEasyPrivateTradeChatChannelService.getChannels().isEmpty()) {
            selectChannel(bisqEasyPrivateTradeChatChannelService.getChannels().stream().findFirst().orElse(null));
        } else if (!privateChatChannelService.getChannels().isEmpty()) {
            selectChannel(privateChatChannelService.getChannels().stream().findFirst().orElse(null));
        } else {
            selectChannel(null);
        }
    }

    private Set<BisqEasyPublicChatChannel> getVisibleChannels() {
        return ((BisqEasyPublicChatChannelService) publicChatChannelService).getVisibleChannels();
    }

    @Override
    protected Stream<ChatChannel<?>> getAllChatChannels() {
        return Stream.concat(publicChatChannelService.getChannels().stream(),
                Stream.concat(privateChatChannelService.getChannels().stream(),
                        bisqEasyPrivateTradeChatChannelService.getChannels().stream()));
    }
}