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
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
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

    public void leaveChannel(PrivateChatChannel<? extends ChatMessage> chatChannel) {
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

        // We do not select first channel if it is a BisqEasyOpenTradeChannel as it might be that there is no matching
        // trade for that. We leave selection to higher level domains.
        selectionService.selectChannel(channelService.getChannels().stream()
                .filter(channel -> !(channel instanceof BisqEasyOpenTradeChannel))
                .findFirst()
                .orElse(null));

        chatNotificationService.consume(chatChannel);
    }

    private PrivateChatChannelService<?, ? extends PrivateChatMessage<?>, ? extends PrivateChatChannel<?>, ?> findChannelService(
            ChatChannelDomain chatChannelDomain) {
        return switch (chatChannelDomain) {
            case BISQ_EASY_OFFERBOOK ->
                    throw new IllegalArgumentException("BISQ_EASY_OFFERBOOK is not supported at LeavePrivateChatChannelService");
            case BISQ_EASY_OPEN_TRADES -> bisqEasyOpenTradeChannelService;
            case BISQ_EASY_PRIVATE_CHAT, DISCUSSION, EVENTS, SUPPORT ->
                    twoPartyPrivateChatChannelServices.get(chatChannelDomain);
        };
    }
}
