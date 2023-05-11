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

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Stream;

@Slf4j
@Getter
public class BisqEasyChatChannelSelectionService extends ChatChannelSelectionService {
    private final TwoPartyPrivateChatChannelService privateBisqEasyTwoPartyChannelService;

    public BisqEasyChatChannelSelectionService(PersistenceService persistenceService,
                                               BisqEasyPrivateTradeChatChannelService privateChatChannelService,
                                               BisqEasyPublicChatChannelService publicChatChannelService,
                                               TwoPartyPrivateChatChannelService privateBisqEasyTwoPartyChannelService,
                                               UserIdentityService userIdentityService) {
        super(persistenceService,
                privateChatChannelService,
                publicChatChannelService,
                ChatChannelDomain.BISQ_EASY,
                userIdentityService);
        this.privateBisqEasyTwoPartyChannelService = privateBisqEasyTwoPartyChannelService;
    }

    @Override
    public void selectChannel(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof BisqEasyPublicChatChannel) {
            BisqEasyPublicChatChannel bisqEasyPublicChatChannel = (BisqEasyPublicChatChannel) chatChannel;
            ((BisqEasyPublicChatChannelService) publicChatChannelService).joinChannel(bisqEasyPublicChatChannel);
        }

        super.selectChannel(chatChannel);
    }

    @Override
    protected Stream<ChatChannel<?>> getAllChatChannels() {
        return Stream.concat(publicChatChannelService.getChannels().stream(),
                Stream.concat(privateBisqEasyTwoPartyChannelService.getChannels().stream(),
                        privateChatChannelService.getChannels().stream()));
    }
}