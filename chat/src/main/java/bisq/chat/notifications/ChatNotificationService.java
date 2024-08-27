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

import bisq.chat.*;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeMessage;
import bisq.chat.priv.PrivateChatMessage;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SystemNotificationService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StoreType.AUTHENTICATED_DATA_STORE;

/**
 * Handles chat notifications
 */
@Slf4j
public class ChatNotificationService implements PersistenceClient<ChatNotificationsStore>, Service {
    // BisqEasyOfferbookMessage use TTL_10_DAYS, BisqEasyOpenTradeMessage and TwoPartyPrivateChatMessage
    // use TTL_30_DAYS
    private static final long MAX_AGE = MetaData.TTL_30_DAYS;

    @Getter
    private final ChatNotificationsStore persistableStore = new ChatNotificationsStore();
    @Getter
    private final Persistence<ChatNotificationsStore> persistence;
    private final ChatService chatService;
    private final SystemNotificationService systemNotificationService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    // changedNotification contains the ChatNotification which was added, removed or got the consumed flag changed
    @Getter
    private final Observable<ChatNotification> changedNotification = new Observable<>();
    private final Map<ChatChannelDomain, Predicate<ChatNotification>> predicateByChatChannelDomain = new HashMap<>();
    private final Map<String, Pin> chatMessagesByChannelIdPins = new ConcurrentHashMap<>();
    private final long startUpDateTime = System.currentTimeMillis();
    @Setter
    private boolean isApplicationFocussed;
    private final Set<String> prunedAndExpiredChatMessageIds = new HashSet<>();

    public ChatNotificationService(PersistenceService persistenceService,
                                   NetworkService networkService,
                                   ChatService chatService,
                                   SystemNotificationService systemNotificationService,
                                   SettingsService settingsService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
        this.chatService = chatService;
        this.systemNotificationService = systemNotificationService;
        this.settingsService = settingsService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;

        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().getStoresByStoreType(AUTHENTICATED_DATA_STORE)
                        .map(DataStorageService::getPrunedAndExpiredDataRequests)
                        .forEach(prunedAndExpiredDataRequests -> prunedAndExpiredDataRequests.addObserver(new CollectionObserver<>() {
                            @Override
                            public void add(DataRequest element) {
                                if (element instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
                                    if (addAuthenticatedDataRequest.getDistributedData() instanceof ChatMessage chatMessage) {
                                        String id = ChatNotification.createId(chatMessage.getChannelId(), chatMessage.getId());
                                        // As we get called at pruning persistence which happens before initializing the services,
                                        // We store the ids to apply the remove at out initialize method.
                                        // For the cases when we get expired data during runtime we call removeNotification.
                                        // For the pre-initialize state that would fail as our persisted data might be filled after
                                        // the network data store.
                                        prunedAndExpiredChatMessageIds.add(id);
                                        removeNotification(id);
                                    }
                                }
                            }

                            @Override
                            public void remove(Object element) {
                            }

                            @Override
                            public void clear() {
                            }
                        })));
    }

    @Override
    public ChatNotificationsStore prunePersisted(ChatNotificationsStore persisted) {
        long pruneDate = System.currentTimeMillis() - MAX_AGE;
        return new ChatNotificationsStore(persisted.getNotifications().stream()
                .filter(e -> e.getDate() > pruneDate)
                .collect(Collectors.toSet()));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        prunedAndExpiredChatMessageIds.forEach(this::removeNotification);
        prunedAndExpiredChatMessageIds.clear();

        BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        bisqEasyOpenTradeChannelService.getChannels().addObserver(() ->
                onChannelsChanged(bisqEasyOpenTradeChannelService.getChannels()));

        BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookChannelService.getChannels().addObserver(() ->
                onChannelsChanged(bisqEasyOfferbookChannelService.getChannels()));

        chatService.getCommonPublicChatChannelServices().values()
                .forEach(commonPublicChatChannelService -> commonPublicChatChannelService.getChannels().addObserver(() ->
                        onChannelsChanged(commonPublicChatChannelService.getChannels())));

        chatService.getTwoPartyPrivateChatChannelServices()
                .forEach(twoPartyPrivateChatChannelService -> twoPartyPrivateChatChannelService.getChannels().addObserver(() ->
                        onChannelsChanged(twoPartyPrivateChatChannelService.getChannels())));

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Consume notifications
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void consume(ChatChannel<?> channel) {
        consume(channel.getChatChannelDomain(), channel.getId());
    }

    public void consume(ChatChannelDomain chatChannelDomain) {
        getNotConsumedNotifications(chatChannelDomain).forEach(this::consumeNotification);
    }

    public void consume(ChatChannelDomain chatChannelDomain, String chatChannelId) {
        getNotConsumedNotifications(chatChannelDomain, chatChannelId).forEach(this::consumeNotification);
    }

    public void consumeAllNotifications() {
        getNotConsumedNotifications().forEach(this::consumeNotification);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Not consumed notifications
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<ChatNotification> getNotConsumedNotifications() {
        synchronized (persistableStore) {
            return persistableStore.getNotConsumedNotifications();
        }
    }

    public Stream<ChatNotification> getNotConsumedNotifications(ChatChannel<?> channel) {
        return getNotConsumedNotifications(channel.getChatChannelDomain(), channel.getId());
    }

    public Stream<ChatNotification> getNotConsumedNotifications(ChatChannelDomain chatChannelDomain) {
        return getNotConsumedNotifications()
                .filter(chatNotification -> chatNotification.getChatChannelDomain() == chatChannelDomain)
                .filter(chatNotification -> findPredicate(chatChannelDomain)
                        .map(predicate -> predicate.test(chatNotification))
                        .orElse(true));
    }

    public Stream<ChatNotification> getNotConsumedNotifications(ChatChannelDomain chatChannelDomain,
                                                                String chatChannelId) {
        // We filter early for the channelId to avoid unnecessary calls on the predicates
        return getNotConsumedNotifications()
                .filter(chatNotification -> chatNotification.getChatChannelId().equals(chatChannelId))
                .filter(chatNotification -> chatNotification.getChatChannelDomain() == chatChannelDomain)
                .filter(this::testChatChannelDomainPredicate);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Number of not consumed notifications
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public long getNumNotifications(ChatChannel<?> channel) {
        return getNumNotifications(channel.getChatChannelDomain(), channel.getId());
    }

    public long getNumNotifications(ChatChannelDomain chatChannelDomain) {
        return getNotConsumedNotifications(chatChannelDomain).count();
    }

    public long getNumNotifications(ChatChannelDomain chatChannelDomain, String chatChannelId) {
        return getNotConsumedNotifications(chatChannelDomain, chatChannelId).count();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatChannelDomain based Predicate
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void putPredicate(ChatChannelDomain chatChannelDomain, Predicate<ChatNotification> predicate) {
        predicateByChatChannelDomain.put(chatChannelDomain, predicate);
        // We use the changedNotification observable for triggering updates. We could make predicateByChatChannelDomain
        // an ObservableHashMap but then all clients need to handle both observables.
        // Seems better to use the below hack to force an update on changedNotification.
        ChatNotification temp = changedNotification.get();
        changedNotification.set(null);
        changedNotification.set(temp);
    }

    public Optional<Predicate<ChatNotification>> findPredicate(ChatChannelDomain chatChannelDomain) {
        return Optional.ofNullable(predicateByChatChannelDomain.get(chatChannelDomain));
    }

    public Boolean testChatChannelDomainPredicate(ChatNotification chatNotification) {
        return findPredicate(chatNotification.getChatChannelDomain())
                .map(predicate -> predicate.test(chatNotification))
                .orElse(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void addNotification(ChatNotification notification) {
        boolean wasAdded = false;
        synchronized (persistableStore) {
            if (!persistableStore.getNotifications().contains(notification)) {
                persistableStore.getNotifications().add(notification);
                wasAdded = true;
            }
            // We always set it as otherwise at restart with no new notifications we would not trigger the observers
            changedNotification.set(notification);
        }
        if (wasAdded) {
            persist();
        }
    }

    private void removeNotification(String id) {
        boolean wasRemoved;
        synchronized (persistableStore) {
            Optional<ChatNotification> candidate = persistableStore.findNotification(id);
            wasRemoved = candidate.map(notification -> {
                        boolean result = persistableStore.getNotifications().remove(notification);
                        if (result) {
                            changedNotification.set(null);
                            changedNotification.set(notification);
                        }
                        return result;
                    })
                    .orElse(false);
        }
        if (wasRemoved) {
            persist();
        }
    }

    private void consumeNotification(ChatNotification notification) {
        if (notification.getIsConsumed().get()) {
            return;
        }
        boolean hadChange;
        synchronized (persistableStore) {
            if (!persistableStore.getNotifications().contains(notification)) {
                notification.setConsumed(true);
                persistableStore.getNotifications().add(notification);
                hadChange = true;
                changedNotification.set(notification);
            } else {
                hadChange = persistableStore.getNotConsumedNotifications()
                        .filter(e -> e.equals(notification))
                        .map(e -> {
                            e.setConsumed(true);
                            return true;
                        })
                        .findAny()
                        .orElse(false);
            }
            if (hadChange) {
                // If we changed the consumed state we need to trigger an update of the observable by setting it to null
                // first as the isConsumed field is excluded from EqualsAndHashCode and thus would not trigger
                // notifications of observers.
                changedNotification.set(null);
                changedNotification.set(notification);
            }
        }
        if (hadChange) {
            persist();
        }
    }

    private boolean isConsumed(ChatNotification notification) {
        synchronized (persistableStore) {
            return persistableStore.findNotification(notification).map(e -> e.getIsConsumed().get()).orElse(false);
        }
    }

    private <M extends ChatMessage> void onChannelsChanged(ObservableSet<? extends ChatChannel<M>> channels) {
        channels.forEach(chatChannel -> {
            String channelId = chatChannel.getId();
            if (chatMessagesByChannelIdPins.containsKey(channelId)) {
                chatMessagesByChannelIdPins.get(channelId).unbind();
            }
            Pin pin = chatChannel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M message) {
                    onMessageAdded(chatChannel, message);
                }

                @Override
                public void remove(Object message) {
                    if (message instanceof ChatMessage chatMessage) {
                        String id = ChatNotification.createId(chatChannel.getId(), chatMessage.getId());
                        removeNotification(id);
                    }
                }

                @Override
                public void clear() {
                    chatChannel.getChatMessages().stream()
                            .map(chatMessage -> ChatNotification.createId(chatChannel.getId(), chatMessage.getId()))
                            .forEach(id -> removeNotification(id));
                }
            });
            chatMessagesByChannelIdPins.put(channelId, pin);
        });
    }

    private <M extends ChatMessage> void onMessageAdded(ChatChannel<M> chatChannel, M chatMessage) {
        if (chatMessage.isMyMessage(userIdentityService)) {
            return;
        }

        if (chatMessage.getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
            // TAKE_BISQ_EASY_OFFER does not result in any text message but is a signal message only, thus we don't
            // use it for notifications
            return;
        }

        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId())) {
            // If we un-ignore later we will get the notifications of the previously banned messages.
            // We might consider to consume the notification to avoid that.
            return;
        }

        String id = ChatNotification.createId(chatChannel.getId(), chatMessage.getId());
        ChatNotification chatNotification = persistableStore.findNotification(id)
                .orElseGet(() -> createNotification(id, chatChannel, chatMessage));

        if (isConsumed(chatNotification)) {
            return;
        }

        // At first start-up when user has not setup their profile yet, we set all notifications as consumed
        if (!userIdentityService.hasUserIdentities()) {
            consumeNotification(chatNotification);
            return;
        }

        // For BisqEasyOfferbookChannels we add it to consumed to not get them shown when switching to a new channel
        /*if (chatChannel instanceof BisqEasyOfferbookChannel &&
                !chatService.getChatChannelSelectionService(chatChannel.getChatChannelDomain()).getSelectedChannel().get().equals(chatChannel)) {
            consumeNotification(chatNotification);
            return;
        }*/

        // If user has set "Show offers only" in settings we mark messages as consumed
        if (chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
            if (settingsService.getOffersOnly().get() && !bisqEasyOfferbookMessage.hasBisqEasyOffer()) {
                consumeNotification(chatNotification);
                return;
            }
        }

        ChatChannelNotificationType notificationType = chatChannel.getChatChannelNotificationType().get();
        if (notificationType == ChatChannelNotificationType.GLOBAL_DEFAULT) {
            notificationType = ChatChannelNotificationType.fromChatNotificationType(settingsService.getChatNotificationType().get());
        }
        boolean shouldSendNotification = switch (notificationType) {
            case GLOBAL_DEFAULT -> throw new RuntimeException("GLOBAL_DEFAULT not possible here");
            case ALL -> true;
            case MENTION ->
                // We treat citations also like mentions
                    userIdentityService.getUserIdentities().stream()
                            .anyMatch(userIdentity -> chatMessage.wasMentioned(userIdentity) ||
                                    chatMessage.wasCited(userIdentity));
            default -> false;
        };

        if (shouldSendNotification) {
            addNotification(chatNotification);
            maybeShowSystemNotification(chatNotification);
        } else {
            consumeNotification(chatNotification);
        }
    }

    private <M extends ChatMessage> ChatNotification createNotification(String id,
                                                                        ChatChannel<M> chatChannel,
                                                                        M chatMessage) {
        Optional<UserProfile> senderUserProfile = chatMessage instanceof PrivateChatMessage
                ? Optional.of(((PrivateChatMessage<?>) chatMessage).getSenderUserProfile())
                : userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
        String title, message;
        if (chatMessage instanceof BisqEasyOpenTradeMessage &&
                chatMessage.getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
            // If TAKE_BISQ_EASY_OFFER message we show title: `Your offer was taken by {peers username}`
            // and message: `Trade ID {tradeId}`
            BisqEasyOpenTradeChannel privateTradeChannel = (BisqEasyOpenTradeChannel) chatChannel;
            title = Res.get("chat.notifications.offerTaken.title", privateTradeChannel.getPeer().getUserName());
            message = Res.get("chat.notifications.offerTaken.message", privateTradeChannel.getTradeId().substring(0, 8));
        } else {
            // For Public messages we show title: `{username} ({channel domain} - {channel title})`
            // For Private messages we show title: `{username} ({channel domain} - Private message)`
            String userName = senderUserProfile.map(UserProfile::getUserName).orElse(Res.get("data.na"));
            String channelInfo = ChatUtil.getChannelNavigationPath(chatChannel);
            title = StringUtils.truncate(userName, 20) + " (" + channelInfo + ")";
            String text = chatMessage.getText();
            if (chatMessage instanceof BisqEasyOpenTradeMessage &&
                    chatMessage.getChatMessageType() == ChatMessageType.PROTOCOL_LOG_MESSAGE) {
                // We have encoded the i18n key so that we can show log messages in the users language.
                text = Res.decode(text);
            }
            message = StringUtils.truncate(text, 210);
        }
        return new ChatNotification(id,
                title,
                message,
                chatChannel,
                chatMessage,
                senderUserProfile);
    }

    private void maybeShowSystemNotification(ChatNotification chatNotification) {
        if (!isApplicationFocussed &&
                isReceivedAfterStartUp(chatNotification) &&
                testChatChannelDomainPredicate(chatNotification)) {
            systemNotificationService.show(chatNotification);
        }
        getNotConsumedNotifications(chatNotification.getChatChannelDomain(), chatNotification.getChatChannelId());
    }

    private boolean isReceivedAfterStartUp(ChatNotification chatNotification) {
        return chatNotification.getDate() > startUpDateTime;
    }
}
