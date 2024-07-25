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

package bisq.chat.priv;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class LeavePrivateChatManager {
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final Map<ChatChannelDomain, TwoPartyPrivateChatChannelService> twoPartyPrivateChatChannelServices;
    private final Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices;
    private final ChatNotificationService chatNotificationService;

    public LeavePrivateChatManager(BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService,
                                   Map<ChatChannelDomain, TwoPartyPrivateChatChannelService> twoPartyPrivateChatChannelServices,
                                   Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices,
                                   ChatNotificationService chatNotificationService) {
        this.bisqEasyOpenTradeChannelService = bisqEasyOpenTradeChannelService;
        this.twoPartyPrivateChatChannelServices = twoPartyPrivateChatChannelServices;
        this.chatChannelSelectionServices = chatChannelSelectionServices;
        this.chatNotificationService = chatNotificationService;
    }

    public void leaveChatChannel(PrivateChatChannel<? extends ChatMessage> chatChannel) {
        ChatChannelDomain chatChannelDomain = chatChannel.getChatChannelDomain();
        PrivateChatChannelService<?, ? extends PrivateChatMessage<?>, ? extends PrivateChatChannel<?>, ?> channelService = findChannelService(chatChannelDomain);
        Optional<? extends PrivateChatChannel<?>> optionalChannel = channelService.findChannel(chatChannel.getId());
        if (optionalChannel.isEmpty()) {
            log.warn("channel not found in channelService. chatChannel={}, channelService={}", chatChannel, channelService);
            return;
        }
        channelService.leaveChannel(chatChannel.getId());
        ChatChannelSelectionService selectionService = chatChannelSelectionServices.get(chatChannelDomain);
        if (selectionService.getSelectedChannel().get() == null) {
            log.warn("selectionService.selectedChannel is null at leaveChatChannel. chatChannel={}", chatChannel);
        }
        selectionService.selectChannel(channelService.getChannels().stream().findFirst().orElse(null));

        chatNotificationService.consume(chatChannel.getId());
    }

    private PrivateChatChannelService<?, ? extends PrivateChatMessage<?>, ? extends PrivateChatChannel<?>, ?> findChannelService(ChatChannelDomain chatChannelDomain) {
        switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK:
                throw new IllegalArgumentException("BISQ_EASY_OFFERBOOK is not supported at LeavePrivateChatChannelService");
            case BISQ_EASY_OPEN_TRADES:
                return bisqEasyOpenTradeChannelService;
            case BISQ_EASY_PRIVATE_CHAT:
            case DISCUSSION:
            case EVENTS:
            case SUPPORT:
                return twoPartyPrivateChatChannelServices.get(chatChannelDomain);
            default:
                throw new IllegalArgumentException("Not supported chatChannelDomain " + chatChannelDomain);
        }
    }
}
