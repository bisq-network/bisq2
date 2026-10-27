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
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.backup.MaxBackupSize;
import bisq.support.SupportService;
import bisq.support.moderator.ReportToModeratorMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class SupportNotificationsService extends RateLimitedPersistenceClient<SupportNotificationsStore> implements Service {
    private final Persistence<SupportNotificationsStore> persistence;
    private final ChatService chatService;
    private final SupportService supportService;
    final SupportNotificationsStore persistableStore = new SupportNotificationsStore();
    Pin moderatorObserver, bisqEasyChannelSelectionObserver, bisqEasyChannelsObserver, musigChannelSelectionObserver, musigChannelsObserver;
    HashMap<String, Pin> bisqEasyMediatorObservers = new HashMap<>();
    HashMap<String, Pin> muSigMediatorObservers = new HashMap<>();
    @Getter
    Observable<SupportNotification> changedNotification = new Observable<>();

    public SupportNotificationsService(PersistenceService persistenceService,
                                ChatService chatService,
                                SupportService supportService) {
        this.chatService = chatService;
        this.supportService = supportService;
        this.persistence = persistenceService.getOrCreatePersistence(this,
                DbSubDirectory.SETTINGS,
                persistableStore,
                MaxBackupSize.from(DbSubDirectory.SETTINGS));
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        moderatorObserver = supportService.getModeratorService().getReportToModeratorMessages()
                .addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(ReportToModeratorMessage message) {
                SupportNotification notification = new SupportNotification(message);
                addNotification(notification);
            }

            @Override
            public void onRemoved(Object element) {
                if(element instanceof ReportToModeratorMessage message) {
                    SupportNotification notification = new SupportNotification(message);
                    removeModeratorNotification(notification);
                }
            }

            @Override
            public void onCleared() {
            }
        });

        bisqEasyChannelsObserver = chatService.getBisqEasyOpenTradeChannelService().getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(BisqEasyOpenTradeChannel channel) {
                if (isMyMediatorChannel(channel)) {
                    Pin pin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void onAdded(BisqEasyOpenTradeMessage element) {
                            SupportNotification notification = new SupportNotification(element);
                            if(notification.getChannelId().isEmpty()) {
                                return;
                            }
                            BisqEasyOpenTradeChannel selectedChannel = (BisqEasyOpenTradeChannel) chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OPEN_TRADES).getSelectedChannel().get();
                            if (selectedChannel != null && notification.getChannelId().get().equals(selectedChannel.getId())) {
                                notification.setConsumed(true);
                            }
                            addNotification(notification);
                        }

                        @Override
                        public void onRemoved(Object element) {
                        }

                        @Override
                        public void onCleared() {
                        }
                    });
                    bisqEasyMediatorObservers.put(channel.getId(), pin);
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel channel) {
                    Pin pin = bisqEasyMediatorObservers.get(channel.getId());
                    if (pin != null) {
                        pin.unbind();
                        bisqEasyMediatorObservers.remove(channel.getId());
                    }
                    removeNotificationsByChannelId(channel.getId());
                }
            }

            @Override
            public void onCleared() {
                bisqEasyMediatorObservers.values().forEach(Pin::unbind);
                bisqEasyMediatorObservers.clear();
                getNotifications().removeIf(SupportNotification::isBisqEasyMediatorNotification);
                persist();
            }
        });

        musigChannelsObserver = chatService.getMuSigOpenTradeChannelService().getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(MuSigOpenTradeChannel channel) {
                if (isMyMediatorChannel(channel)) {
                    Pin pin = channel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void onAdded(MuSigOpenTradeMessage element) {
                            SupportNotification notification = new SupportNotification(element);
                            if (notification.getChannelId().isEmpty()) {
                                return;
                            }
                            MuSigOpenTradeChannel selectedChannel = (MuSigOpenTradeChannel) chatService.getChatChannelSelectionService(ChatChannelDomain.MU_SIG_OPEN_TRADES).getSelectedChannel().get();
                            if (selectedChannel != null && notification.getChannelId().get().equals(selectedChannel.getId())) {
                                notification.setConsumed(true);
                            }
                            addNotification(notification);
                        }

                        @Override
                        public void onRemoved(Object element) {
                        }

                        @Override
                        public void onCleared() {
                        }
                    });
                    muSigMediatorObservers.put(channel.getId(), pin);
                }
            }

            @Override
            public void onRemoved(Object element) {
                if (element instanceof MuSigOpenTradeChannel channel) {
                    Pin pin = muSigMediatorObservers.remove(channel.getId());
                    if (pin != null) {
                        pin.unbind();
                    }
                    removeNotificationsByChannelId(channel.getId());
                }
            }

            @Override
            public void onCleared() {
                muSigMediatorObservers.values().forEach(Pin::unbind);
                muSigMediatorObservers.clear();
                getNotifications().removeIf(SupportNotification::isMuSigMediatorNotification);
                persist();
            }
        });

        bisqEasyChannelSelectionObserver = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .getSelectedChannel().addObserver(chatChannel -> selectedChannelChanged((BisqEasyOpenTradeChannel) chatChannel));
        musigChannelSelectionObserver = chatService.getChatChannelSelectionService(ChatChannelDomain.MU_SIG_OPEN_TRADES)
                .getSelectedChannel().addObserver(chatChannel -> selectedChannelChanged((MuSigOpenTradeChannel) chatChannel));

        return CompletableFuture.completedFuture(true);
    }

    public void selectedChannelChanged(BisqEasyOpenTradeChannel chatChannel) {
        if (chatChannel == null) {
            return;
        }

        if(isMyMediatorChannel(chatChannel)) {
            getNotConsumedBisqEasyMediatorNotifications().forEach(notification-> {
                if(notification.getChannelId().isPresent()
                        && notification.getChannelId().get().equals(chatChannel.getId())) {
                    consumeNotification(notification);
                }
            });
        }
    }

    public void selectedChannelChanged(MuSigOpenTradeChannel chatChannel) {
        if (chatChannel == null) {
            return;
        }

        if (isMyMediatorChannel(chatChannel)) {
            getNotConsumedMusigMediatorNotifications().forEach(notification-> {
                if(notification.getChannelId().isPresent()
                        && notification.getChannelId().get().equals(chatChannel.getId())) {
                    consumeNotification(notification);
                }
            });
        }
    }

    public void consumeModeratorNotifications() {
         getNotConsumedNotifications().filter(SupportNotification::isModeratorNotification)
                .forEach(this::consumeNotification);
    }

    public ObservableSet<SupportNotification> getNotifications() {
        return persistableStore.getNotifications();
    }

    public Stream<SupportNotification> getNotConsumedNotifications() {
        return persistableStore.getNotifications().stream().filter(SupportNotification::isNotConsumed);
    }

    public Stream<SupportNotification> getNotConsumedMediatorNotifications() {
        return getNotConsumedNotifications().filter(SupportNotification::isMediatorNotification);
    }

    public Stream<SupportNotification> getNotConsumedBisqEasyMediatorNotifications() {
        return getNotConsumedNotifications().filter(SupportNotification::isBisqEasyMediatorNotification);
    }

    public Stream<SupportNotification> getNotConsumedMusigMediatorNotifications() {
        return getNotConsumedNotifications().filter(SupportNotification::isMuSigMediatorNotification);
    }

    public Stream<SupportNotification> getNotConsumedModeratorNotifications() {
        return getNotConsumedNotifications().filter(SupportNotification::isModeratorNotification);
    }

    private void addNotification(SupportNotification notification) {
        boolean wasAdded = false;
        synchronized (persistableStore) {
            if (!persistableStore.getNotifications().contains(notification)) {
                persistableStore.getNotifications().add(notification);
                wasAdded = true;
            }
        }
        if (wasAdded) {
            persist();
            changedNotification.set(notification);
        }
    }

    public void consumeNotification(SupportNotification notification) {
        boolean hadChange;
        synchronized (persistableStore) {
            if (!persistableStore.getNotifications().contains(notification)) {
                notification.setConsumed(true);
                persistableStore.getNotifications().add(notification);
                hadChange = true;
            } else {
                hadChange = getNotConsumedNotifications()
                        .filter(e -> e.equals(notification))
                        .map(e -> {
                            e.setConsumed(true);
                            return true;
                        })
                        .findAny()
                        .orElse(false);
            }
        }
        if (hadChange) {
            persist();
            changedNotification.set(null);
            changedNotification.set(notification);
        }
    }

    private void removeModeratorNotification(SupportNotification notification) {
        boolean wasRemoved = false;
        synchronized (persistableStore) {
            if (persistableStore.getNotifications().contains(notification)) {
                wasRemoved = persistableStore.getNotifications().remove(notification);
            }
        }
        if (wasRemoved) {
            // notifications are already consumed on moderator page, no need to trigger change
            persist();
        }
    }

    private void removeNotificationsByChannelId(String channelId) {
        boolean wasRemoved;
        synchronized (persistableStore) {
            wasRemoved = persistableStore.getNotifications().removeIf(notification ->
                    notification.getChannelId().isPresent()
                            && notification.getChannelId().get().equals(channelId));
        }
        if (wasRemoved) {
            persist();
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        moderatorObserver.unbind();
        bisqEasyChannelSelectionObserver.unbind();
        bisqEasyChannelsObserver.unbind();
        musigChannelSelectionObserver.unbind();
        musigChannelsObserver.unbind();
        bisqEasyMediatorObservers.values().forEach(Pin::unbind);
        bisqEasyMediatorObservers.clear();
        muSigMediatorObservers.values().forEach(Pin::unbind);
        muSigMediatorObservers.clear();
        
        changedNotification.set(null);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Persistence<SupportNotificationsStore> getPersistence() {
        return persistence;
    }

    @Override
    public PersistableStore<SupportNotificationsStore> getPersistableStore() {
        return persistableStore;
    }

    public boolean isMyMediatorChannel(BisqEasyOpenTradeChannel channel) {
        if (channel.isInMediation() && channel.getMediator().isPresent()) {
            return supportService.getBisqEasyMediatorService().findMyMediatorUserIdentity(channel.getMediator()).isPresent();
        }
        return false;
    }

    public boolean isMyMediatorChannel(MuSigOpenTradeChannel channel) {
        if (channel.getMediator().isPresent()) {
            return supportService.getMuSigMediatorService().findMyMediatorUserIdentity(channel.getMediator()).isPresent();
        }
        return false;
    }
}
