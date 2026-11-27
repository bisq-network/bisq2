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

package bisq.support.notifications;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.persistence.PersistableStore;
import bisq.persistence.backup.MaxBackupSize;
import bisq.support.SupportService;
import bisq.support.mediation.bisq_easy.BisqEasyMediatorService;
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.support.moderator.ModeratorService;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportNotificationsServiceTest {
    private Persistence<SupportNotificationsStore> persistence;
    private SupportNotificationsService service;
    private ObservableSet<ReportToModeratorMessage> reportToModeratorMessages;
    private ObservableSet<BisqEasyOpenTradeChannel> bisqEasyChannels;
    private ObservableSet<MuSigOpenTradeChannel> muSigChannels;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(PersistenceClient.class),
                any(DbSubDirectory.class),
                any(PersistableStore.class),
                any(MaxBackupSize.class))).thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        reportToModeratorMessages = new ObservableSet<>();
        ModeratorService moderatorService = mock(ModeratorService.class);
        when(moderatorService.getReportToModeratorMessages()).thenReturn(reportToModeratorMessages);

        bisqEasyChannels = new ObservableSet<>();
        BisqEasyOpenTradeChannelService bisqEasyChannelService = mock(BisqEasyOpenTradeChannelService.class);
        when(bisqEasyChannelService.getChannels()).thenReturn(bisqEasyChannels);

        muSigChannels = new ObservableSet<>();
        MuSigOpenTradeChannelService muSigChannelService = mock(MuSigOpenTradeChannelService.class);
        when(muSigChannelService.getChannels()).thenReturn(muSigChannels);

        ChatChannelSelectionService bisqEasySelectionService = mock(ChatChannelSelectionService.class);
        when(bisqEasySelectionService.getSelectedChannel()).thenReturn(new Observable<>());
        ChatChannelSelectionService muSigSelectionService = mock(ChatChannelSelectionService.class);
        when(muSigSelectionService.getSelectedChannel()).thenReturn(new Observable<>());

        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(bisqEasyChannelService);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(muSigChannelService);
        when(chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OPEN_TRADES))
                .thenReturn(bisqEasySelectionService);
        when(chatService.getChatChannelSelectionService(ChatChannelDomain.MU_SIG_OPEN_TRADES))
                .thenReturn(muSigSelectionService);

        BisqEasyMediatorService bisqEasyMediatorService = mock(BisqEasyMediatorService.class);
        when(bisqEasyMediatorService.findMyMediatorUserIdentity(any()))
                .thenReturn(Optional.of(mock(UserIdentity.class)));
        MuSigMediatorService muSigMediatorService = mock(MuSigMediatorService.class);
        when(muSigMediatorService.findMyMediatorUserIdentity(any()))
                .thenReturn(Optional.of(mock(UserIdentity.class)));

        SupportService supportService = mock(SupportService.class);
        when(supportService.getModeratorService()).thenReturn(moderatorService);
        when(supportService.getBisqEasyMediatorService()).thenReturn(bisqEasyMediatorService);
        when(supportService.getMuSigMediatorService()).thenReturn(muSigMediatorService);

        service = new SupportNotificationsService(
                persistenceService,
                chatService,
                supportService);
        service.initialize().join();
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void consumedReplayUpdatesEqualPersistedNotification() {
        SupportNotification persistedNotification = createNotification(false);
        service.getNotifications().add(persistedNotification);

        service.consumeModeratorNotification(createNotification(true));

        assertThat(persistedNotification.isNotConsumed()).isFalse();
        assertThat(service.getNotifications()).containsExactly(persistedNotification);
        verify(persistence).persistAsync(any());
    }

    @Test
    void moderatorNotificationsFollowStoreLifecycle() {
        ReportToModeratorMessage report = moderatorReportMessage();

        reportToModeratorMessages.add(report);

        assertThat(service.getNotifications()).hasSize(1);
        SupportNotification storedNotification = service.getNotifications().iterator().next();
        assertThat(storedNotification.isModeratorNotification()).isTrue();
        assertThat(storedNotification.isNotConsumed()).isTrue();

        service.consumeModeratorNotifications();

        assertThat(storedNotification.isNotConsumed()).isFalse();

        reportToModeratorMessages.remove(report);

        assertThat(service.getNotifications()).isEmpty();
    }

    @Test
    void removesNotificationsWhenBisqEasyMediatorChannelIsRemoved() {
        String channelId = "bisq-easy-channel";
        BisqEasyOpenTradeChannel channel = bisqEasyMediatorChannel(channelId);
        bisqEasyChannels.add(channel);

        SupportNotification relatedNotification = createNotification(
                "related-notification",
                SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION,
                channelId,
                false);
        SupportNotification unrelatedNotification = createNotification(
                "unrelated-notification",
                SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION,
                "other-channel",
                false);
        service.getNotifications().addAll(List.of(relatedNotification, unrelatedNotification));

        bisqEasyChannels.remove(channel);

        assertThat(service.getNotifications()).containsExactly(unrelatedNotification);
    }

    @Test
    void removesNotificationsWhenMuSigMediatorChannelIsRemoved() {
        String channelId = "mu-sig-channel";
        MuSigOpenTradeChannel channel = muSigMediatorChannel(channelId);
        muSigChannels.add(channel);

        SupportNotification relatedNotification = createNotification(
                "related-notification",
                SupportNotificationType.MU_SIG_MEDIATOR_NOTIFICATION,
                channelId,
                false);
        SupportNotification unrelatedNotification = createNotification(
                "unrelated-notification",
                SupportNotificationType.MU_SIG_MEDIATOR_NOTIFICATION,
                "other-channel",
                false);
        service.getNotifications().addAll(List.of(relatedNotification, unrelatedNotification));

        muSigChannels.remove(channel);

        assertThat(service.getNotifications()).containsExactly(unrelatedNotification);
    }

    private ReportToModeratorMessage moderatorReportMessage() {
        ReportToModeratorMessage report = mock(ReportToModeratorMessage.class);
        when(report.getDate()).thenReturn(1L);
        when(report.getReporterUserProfileId()).thenReturn("reporter-id");
        when(report.getMessage()).thenReturn("reported message");
        return report;
    }

    private BisqEasyOpenTradeChannel bisqEasyMediatorChannel(String channelId) {
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getId()).thenReturn(channelId);
        when(channel.isInMediation()).thenReturn(true);
        when(channel.getMediator()).thenReturn(Optional.of(mock(UserProfile.class)));
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>());
        return channel;
    }

    private MuSigOpenTradeChannel muSigMediatorChannel(String channelId) {
        MuSigOpenTradeChannel channel = mock(MuSigOpenTradeChannel.class);
        when(channel.getId()).thenReturn(channelId);
        when(channel.getMediator()).thenReturn(Optional.of(mock(UserProfile.class)));
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>());
        return channel;
    }

    private SupportNotification createNotification(boolean consumed) {
        return createNotification(
                "notification-id",
                SupportNotificationType.BISQ_EASY_MEDIATOR_NOTIFICATION,
                "channel-id",
                consumed);
    }

    private SupportNotification createNotification(String id,
                                                   SupportNotificationType type,
                                                   String channelId,
                                                   boolean consumed) {
        return new SupportNotification(
                id,
                type.toProtoEnum(),
                Optional.of(channelId),
                Optional.of("sender-id"),
                "title",
                "message",
                consumed);
    }
}
