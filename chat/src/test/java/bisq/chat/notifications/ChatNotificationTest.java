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

package bisq.chat.notifications;

import bisq.chat.ChatChannelDomain;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what a {@link ChatNotification} reports to the mobile relay. The wire side of this is covered by
 * {@code MobileNotificationPayloadTest}, but that builds its notifications from an anonymous
 * {@link bisq.notifications.Notification}, so it fixes the interface contract and not the one
 * implementation that actually produces push payloads in production.
 */
class ChatNotificationTest {
    private static final String CHANNEL_ID = "discussion.alice-bob";
    private static final String MESSAGE_ID = "message-1";

    /**
     * Both halves of the pair, because the channel only survives to the wire while the trade id is absent:
     * {@code MobileNotificationPayload#from} drops the channel in favour of a trade id whenever there is
     * one. A DM that started reporting a trade id would silently route the push to the open-trade list
     * instead of the conversation, and nothing downstream would fail.
     */
    @Test
    void theChannelIsReportedSoADmPushCanDeepLinkToTheConversation() {
        ChatNotification notification = notificationFrom(dmChannel(), Optional.of(userProfile("Alice")));

        assertThat(notification.getChannelId()).contains(CHANNEL_ID);
        assertThat(notification.getTradeId()).isEmpty();
    }

    @Test
    void theSenderIsReportedAsThePeerUserName() {
        ChatNotification notification = notificationFrom(dmChannel(), Optional.of(userProfile("Alice")));

        assertThat(notification.getPeerUserName()).contains("Alice");
    }

    /**
     * The same condition under which {@code ChatNotificationService.createNotification} falls back to
     * {@code Res.get("data.na")} for the title. The name is left absent rather than carrying that
     * placeholder to the wire, because the client cannot localise a string this side already resolved.
     */
    @Test
    void anUnresolvedSenderReportsNoPeerUserNameRatherThanAPlaceholder() {
        ChatNotification notification = notificationFrom(dmChannel(), Optional.empty());

        assertThat(notification.getPeerUserName()).isEmpty();
    }

    /**
     * A trade chat reports both ids. Which of them reaches the wire is decided at the transport
     * boundary by {@code MobileNotificationPayload#from}, which drops the channel — this pins the
     * producer half of that rule, so a change here cannot silently make the drop a no-op.
     */
    @Test
    void aTradeChatReportsBothTheTradeIdAndTheChannelId() {
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getId()).thenReturn("bisq-easy-open-trades.trade-abcd");
        when(channel.getChatChannelDomain()).thenReturn(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
        when(channel.getTradeId()).thenReturn("trade-abcd");
        when(channel.getMediator()).thenReturn(Optional.empty());

        BisqEasyOpenTradeMessage message = mock(BisqEasyOpenTradeMessage.class);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getDate()).thenReturn(1L);

        ChatNotification notification = new ChatNotification("id", "title", "message",
                channel, message, Optional.of(userProfile("Bob")));

        assertThat(notification.getTradeId()).contains("trade-abcd");
        assertThat(notification.getChannelId()).contains("bisq-easy-open-trades.trade-abcd");
    }

    private static ChatNotification notificationFrom(TwoPartyPrivateChatChannel channel,
                                                     Optional<UserProfile> senderUserProfile) {
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getDate()).thenReturn(1L);
        return new ChatNotification("id", "title", "message", channel, message, senderUserProfile);
    }

    private static TwoPartyPrivateChatChannel dmChannel() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channel.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        return channel;
    }

    /**
     * {@code getUserName()} is stubbed rather than left to the real implementation, which resolves the
     * nym through the {@code UserProfileService} singleton — absent in a unit test.
     */
    private static UserProfile userProfile(String userName) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getUserName()).thenReturn(userName);
        return userProfile;
    }
}
