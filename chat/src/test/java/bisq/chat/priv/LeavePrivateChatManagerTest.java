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

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The selection is shared with the desktop's Discussion page. The desktop only ever leaves the channel
 * it is showing, so it never saw the difference; the API leaves any channel by id, and moving the
 * selection for a background one would switch or clear what the desktop displays.
 */
class LeavePrivateChatManagerTest {
    private TwoPartyPrivateChatChannelService channelService;
    private ChatChannelSelectionService selectionService;
    private ChatNotificationService chatNotificationService;
    private final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();
    private TwoPartyPrivateChatChannel leaving;
    private TwoPartyPrivateChatChannel remaining;
    private LeavePrivateChatManager manager;

    @BeforeEach
    void setUp() {
        leaving = channel("discussion.a-b");
        remaining = channel("discussion.a-c");

        channelService = mock(TwoPartyPrivateChatChannelService.class);
        when(channelService.findChannel("discussion.a-b")).thenReturn(Optional.of(leaving));
        // What is left after the leave, which is what a successor is picked from.
        when(channelService.getChannels()).thenReturn(new ObservableSet<>(Set.of(remaining)));

        selectionService = mock(ChatChannelSelectionService.class);
        when(selectionService.getSelectedChannel()).thenReturn(selectedChannel);
        chatNotificationService = mock(ChatNotificationService.class);

        manager = new LeavePrivateChatManager(mock(BisqEasyOpenTradeChannelService.class),
                mock(MuSigOpenTradeChannelService.class),
                Map.of(ChatChannelDomain.DISCUSSION, channelService),
                Map.of(ChatChannelDomain.DISCUSSION, selectionService),
                chatNotificationService);
    }

    @Test
    void leavingTheSelectedChannelMovesTheSelectionToTheNextOne() {
        // The persisted selection is a different instance under the same id, so the comparison is by id.
        selectedChannel.set(channel("discussion.a-b"));

        manager.leaveChannel(leaving);

        verify(selectionService).selectChannel(remaining);
    }

    @Test
    void leavingABackgroundChannelLeavesTheSelectionAlone() {
        selectedChannel.set(remaining);

        manager.leaveChannel(leaving);

        verify(selectionService, never()).selectChannel(any());
        verify(channelService).leaveChannel("discussion.a-b");
        verify(chatNotificationService).consume(leaving);
    }

    @Test
    void leavingWithNothingSelectedSelectsNothing() {
        manager.leaveChannel(leaving);

        verify(selectionService, never()).selectChannel(any());
        verify(channelService).leaveChannel("discussion.a-b");
    }

    private static TwoPartyPrivateChatChannel channel(String id) {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(id);
        when(channel.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        return channel;
    }
}
