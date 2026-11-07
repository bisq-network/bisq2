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

package bisq.mu_sig;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MusigNotificationServiceTest {
    /**
     * A notification can arrive before its trade and private chat channel exist.
     * It is kept as an orphan and retried when either one changes. This test verifies
     * that consuming the notification during that gap prevents the later retry from
     * showing it in the MuSig notification banner.
     */
    @Test
    void consumedOrphanIsNotRepublishedWhenTradeChannelResolves() {
        ChatNotificationService chatNotificationService = mock(ChatNotificationService.class);
        when(chatNotificationService.getNotConsumedNotifications()).thenReturn(Stream.empty());

        // This set models the notification store's live unconsumed query. The same
        // notification instances are also retained by MusigNotificationService as
        // orphans, so changing this set models consumeNotification updating them.
        Set<ChatNotification> unconsumedNotifications = new HashSet<>();
        ChatNotification orphanedNotification = mock(ChatNotification.class);
        when(orphanedNotification.getTradeId()).thenReturn(Optional.of("orphan-trade-id"));
        when(orphanedNotification.isNotConsumed()).thenAnswer(invocation ->
                unconsumedNotifications.contains(orphanedNotification));

        ChatNotification resolvedNotification = mock(ChatNotification.class);
        when(resolvedNotification.getTradeId()).thenReturn(Optional.of("resolved-trade-id"));
        when(resolvedNotification.isNotConsumed()).thenAnswer(invocation ->
                unconsumedNotifications.contains(resolvedNotification));

        unconsumedNotifications.add(orphanedNotification);
        unconsumedNotifications.add(resolvedNotification);
        when(chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.MU_SIG_OPEN_TRADES))
                .thenAnswer(invocation -> unconsumedNotifications.stream());

        Observable<ChatNotification> changedNotification = new Observable<>();
        when(chatNotificationService.getChangedNotification()).thenReturn(changedNotification);

        ChatService chatService = mock(ChatService.class);
        when(chatService.getChatNotificationService()).thenReturn(chatNotificationService);

        MuSigTrade orphanedTrade = mock(MuSigTrade.class);
        when(orphanedTrade.getId()).thenReturn("orphan-trade-id");
        MuSigTrade resolvedTrade = mock(MuSigTrade.class);
        when(resolvedTrade.getId()).thenReturn("resolved-trade-id");
        ObservableHashMap<String, MuSigTrade> trades = new ObservableHashMap<>();
        MuSigTradeService muSigTradeService = mock(MuSigTradeService.class);
        when(muSigTradeService.getTradeById()).thenReturn(trades);
        when(muSigTradeService.findOpenTrade("orphan-trade-id")).thenReturn(Optional.empty());
        when(muSigTradeService.findOpenTrade("resolved-trade-id")).thenReturn(Optional.of(resolvedTrade));

        ObservableSet<MuSigOpenTradeChannel> channels = new ObservableSet<>();
        MuSigOpenTradeChannelService muSigOpenTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        when(muSigOpenTradeChannelService.getChannels()).thenReturn(channels);
        when(muSigOpenTradeChannelService.findChannelByTradeId("orphan-trade-id")).thenReturn(Optional.empty());
        when(muSigOpenTradeChannelService.findChannelByTradeId("resolved-trade-id"))
                .thenReturn(Optional.of(mock(MuSigOpenTradeChannel.class)));

        MusigNotificationService service = new MusigNotificationService(muSigOpenTradeChannelService,
                chatService,
                muSigTradeService,
                mock(MuSigMediatorService.class));
        service.initialize();

        // The first notification is orphaned, while the second one resolves and makes
        // the panel visible. This gives the test a real visible-to-hidden transition.
        changedNotification.set(resolvedNotification);
        assertTrue(service.getIsNotificationPanelVisible().get());
        assertEquals(1, service.getTradeNotifications().size());
        assertTrue(service.getTradeNotifications().contains(resolvedNotification));

        // Keep a different notification as the last change event so the next event
        // can consume the resolved notification.
        changedNotification.set(orphanedNotification);

        // NotificationsSettingsController can consume all notifications through
        // ChatNotificationService before the orphan's trade and channel arrive.
        unconsumedNotifications.remove(resolvedNotification);
        changedNotification.set(resolvedNotification);
        assertFalse(service.getIsNotificationPanelVisible().get());
        assertEquals(0, service.getTradeNotifications().size());

        // Consume the unresolved notification while it is still an orphan.
        unconsumedNotifications.remove(orphanedNotification);
        changedNotification.set(orphanedNotification);

        // The trade and channel now resolve the orphan and trigger the replay path.
        when(muSigTradeService.findOpenTrade("orphan-trade-id")).thenReturn(Optional.of(orphanedTrade));
        when(muSigOpenTradeChannelService.findChannelByTradeId("orphan-trade-id"))
                .thenReturn(Optional.of(mock(MuSigOpenTradeChannel.class)));
        trades.put("orphan-trade-id", orphanedTrade);
        channels.add(mock(MuSigOpenTradeChannel.class));

        // The consumed orphan must not be republished into the banner.
        assertFalse(service.getIsNotificationPanelVisible().get());
        assertEquals(0, service.getTradeNotifications().size());
        assertFalse(service.getTradeNotifications().contains(orphanedNotification));
    }

    @Test
    void unconsumedOrphanIsRepublishedWhenTradeChannelResolves() {
        ChatNotificationService chatNotificationService = mock(ChatNotificationService.class);
        when(chatNotificationService.getNotConsumedNotifications()).thenReturn(Stream.empty());

        Set<ChatNotification> unconsumedNotifications = new HashSet<>();
        ChatNotification notification = mock(ChatNotification.class);
        when(notification.getTradeId()).thenReturn(Optional.of("trade-id"));
        when(notification.isNotConsumed()).thenReturn(true);
        unconsumedNotifications.add(notification);
        when(chatNotificationService.getNotConsumedNotifications(ChatChannelDomain.MU_SIG_OPEN_TRADES))
                .thenAnswer(invocation -> unconsumedNotifications.stream());

        Observable<ChatNotification> changedNotification = new Observable<>();
        when(chatNotificationService.getChangedNotification()).thenReturn(changedNotification);

        ChatService chatService = mock(ChatService.class);
        when(chatService.getChatNotificationService()).thenReturn(chatNotificationService);

        MuSigTrade trade = mock(MuSigTrade.class);
        when(trade.getId()).thenReturn("trade-id");
        ObservableHashMap<String, MuSigTrade> trades = new ObservableHashMap<>();
        MuSigTradeService muSigTradeService = mock(MuSigTradeService.class);
        when(muSigTradeService.getTradeById()).thenReturn(trades);
        when(muSigTradeService.findOpenTrade("trade-id")).thenReturn(Optional.empty());

        ObservableSet<MuSigOpenTradeChannel> channels = new ObservableSet<>();
        MuSigOpenTradeChannelService muSigOpenTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        when(muSigOpenTradeChannelService.getChannels()).thenReturn(channels);
        when(muSigOpenTradeChannelService.findChannelByTradeId("trade-id")).thenReturn(Optional.empty());

        MusigNotificationService service = new MusigNotificationService(muSigOpenTradeChannelService,
                chatService,
                muSigTradeService,
                mock(MuSigMediatorService.class));
        service.initialize();

        // The notification is unconsumed, but it cannot be shown until its trade
        // and channel have both been received.
        changedNotification.set(notification);
        assertFalse(service.getIsNotificationPanelVisible().get());
        assertEquals(0, service.getTradeNotifications().size());

        // Resolving the trade alone is not enough because the channel is still missing.
        when(muSigTradeService.findOpenTrade("trade-id")).thenReturn(Optional.of(trade));
        trades.put("trade-id", trade);
        assertFalse(service.getIsNotificationPanelVisible().get());
        assertEquals(0, service.getTradeNotifications().size());

        // Once the channel resolves, the unconsumed orphan is republished into the banner
        when(muSigOpenTradeChannelService.findChannelByTradeId("trade-id"))
                .thenReturn(Optional.of(mock(MuSigOpenTradeChannel.class)));
        channels.add(mock(MuSigOpenTradeChannel.class));

        assertTrue(service.getIsNotificationPanelVisible().get());
        assertEquals(1, service.getTradeNotifications().size());
        assertTrue(service.getTradeNotifications().contains(notification));
    }
}
