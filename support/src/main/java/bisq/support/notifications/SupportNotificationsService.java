package bisq.support.notifications;

import bisq.chat.ChatChannel;
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

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class SupportNotificationsService extends RateLimitedPersistenceClient<SupportNotificationsStore> implements Service {
    private final Persistence<SupportNotificationsStore> persistence;
    private final ChatService chatService;
    private final SupportService supportService;
    final SupportNotificationsStore persistableStore = new SupportNotificationsStore();
    Pin moderatorObserver, bisqEasyChannelSelectionObserver, bisqEasyChannelsObserver, musigChannelSelectionObserver, musigChannelsObserver;
    HashSet<Pin> bisqEasyMediatorObservers = new HashSet<>();
    HashSet<Pin> muSigMediatorObservers = new HashSet<>();
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
                    removeNotification(notification);
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
                    bisqEasyMediatorObservers.add(pin);
                }
            }

            @Override
            public void onRemoved(Object element) {
            }

            @Override
            public void onCleared() {
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
                    muSigMediatorObservers.add(pin);
                }
            }

            @Override
            public void onRemoved(Object element) {
            }

            @Override
            public void onCleared() {
            }
        });

        bisqEasyChannelSelectionObserver = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .getSelectedChannel().addObserver(this::selectedChannelChanged);
        musigChannelSelectionObserver = chatService.getChatChannelSelectionService(ChatChannelDomain.MU_SIG_OPEN_TRADES)
                .getSelectedChannel().addObserver(this::selectedChannelChanged);

        return CompletableFuture.completedFuture(true);
    }

    public void selectedChannelChanged(ChatChannel chatChannel) {
        if (chatChannel == null) {
            return;
        }

        if (chatChannel instanceof MuSigOpenTradeChannel) {
            boolean isMyMediationChannel = supportService.getMuSigMediatorService()
                    .findMyMediatorUserIdentity(((MuSigOpenTradeChannel) chatChannel).getMediator()).isPresent();
            if (isMyMediationChannel) {
                getNotConsumedMusigMediatorNotifications().forEach(notification-> {
                    if(notification.getChannelId().isPresent()
                            && notification.getChannelId().get().equals(chatChannel.getId())) {
                        consumeNotification(notification);
                    }
                });
            }
        }
        else if (chatChannel instanceof BisqEasyOpenTradeChannel) {
            boolean isMyMediationChannel = supportService.getBisqEasyMediatorService()
                    .findMyMediatorUserIdentity(((BisqEasyOpenTradeChannel) chatChannel).getMediator()).isPresent();
            if(isMyMediationChannel) {
                getNotConsumedBisqEasyMediatorNotifications().forEach(notification-> {
                    if(notification.getChannelId().isPresent()
                            && notification.getChannelId().get().equals(chatChannel.getId())) {
                        consumeNotification(notification);
                    }
                });
            }
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

    public void removeNotification(SupportNotification notification) {
        boolean wasRemoved = false;
        synchronized (persistableStore) {
            if (persistableStore.getNotifications().contains(notification)) {
                persistableStore.getNotifications().remove(notification);
                wasRemoved = true;
            }
        }
        if (wasRemoved) {
            persist();
            changedNotification.set(null);
            changedNotification.set(notification);
        }
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        moderatorObserver.unbind();
        bisqEasyChannelSelectionObserver.unbind();
        bisqEasyChannelsObserver.unbind();
        musigChannelSelectionObserver.unbind();
        musigChannelsObserver.unbind();
        bisqEasyMediatorObservers.forEach(Pin::unbind);
        bisqEasyMediatorObservers.clear();
        muSigMediatorObservers.forEach(Pin::unbind);
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
