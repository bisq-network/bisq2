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
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
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
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.SendNotificationService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final SendNotificationService sendNotificationService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    // changedNotification contains the ChatNotification which was added, removed or got the consumed flag changed
    @Getter
    private final Observable<ChatNotification> changedNotification = new Observable<>();
    private final Map<String, Pin> chatMessagesByChannelIdPins = new ConcurrentHashMap<>();

    public ChatNotificationService(PersistenceService persistenceService,
                                   ChatService chatService,
                                   SendNotificationService sendNotificationService,
                                   SettingsService settingsService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.chatService = chatService;
        this.sendNotificationService = sendNotificationService;
        this.settingsService = settingsService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
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
        BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        bisqEasyOpenTradeChannelService.getChannels().addObserver(() ->
                onChannelsChanged(bisqEasyOpenTradeChannelService.getChannels()));

        BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookChannelService.getChannels().addObserver(() ->
                onChannelsChanged(bisqEasyOfferbookChannelService.getChannels()));

        chatService.getCommonPublicChatChannelServices().values()
                .forEach(commonPublicChatChannelService -> commonPublicChatChannelService.getChannels().addObserver(() ->
                        onChannelsChanged(commonPublicChatChannelService.getChannels())));

        chatService.getTwoPartyPrivateChatChannelServices().values()
                .forEach(twoPartyPrivateChatChannelService -> twoPartyPrivateChatChannelService.getChannels().addObserver(() ->
                        onChannelsChanged(twoPartyPrivateChatChannelService.getChannels())));

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void consume(String channelId) {
        getNotConsumedNotifications(channelId)
                .forEach(this::consumeNotification);
    }

    public void consumeAllNotifications() {
        getNotConsumedNotifications()
                .forEach(this::consumeNotification);
    }

    public Stream<ChatNotification> getNotConsumedNotifications() {
        synchronized (persistableStore) {
            return persistableStore.getNotConsumedNotifications();
        }
    }

    public Stream<ChatNotification> getNotConsumedNotifications(ChatChannelDomain chatChannelDomain) {
        return getNotConsumedNotifications()
                .filter(chatNotification -> chatNotification.getChatChannelDomain() == chatChannelDomain);
    }

    public Stream<ChatNotification> getNotConsumedNotifications(String channelId) {
        return getNotConsumedNotifications()
                .filter(chatNotification -> chatNotification.getChatChannelId().equals(channelId));
    }

    public Set<String> getTradeIdsOfNotConsumedNotifications() {
        return getNotConsumedNotifications(ChatChannelDomain.BISQ_EASY_OPEN_TRADES)
                .flatMap(chatNotification -> chatNotification.getTradeId().stream())
                .collect(Collectors.toSet());
    }

    public long getNumNotifications(ChatChannelDomain chatChannelDomain) {
        return getNotConsumedNotifications(chatChannelDomain).count();
    }

    public long getNumNotifications(String channelId) {
        return getNotConsumedNotifications(channelId).count();
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
        if (notification.isConsumed()) {
            return;
        }
        boolean hadChange;
        synchronized (persistableStore) {
            if (!persistableStore.getNotifications().contains(notification)) {
                notification.setConsumed(true);
                persistableStore.getNotifications().add(notification);
                hadChange = true;
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
                changedNotification.set(notification);
            }
        }
        if (hadChange) {
            persist();
        }
    }

    private boolean isConsumed(ChatNotification notification) {
        synchronized (persistableStore) {
            return persistableStore.findNotification(notification).map(ChatNotification::isConsumed).orElse(false);
        }
    }

    private <M extends ChatMessage> void onChannelsChanged(ObservableArray<? extends ChatChannel<M>> channels) {
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
                    if (message instanceof ChatMessage) {
                        ChatMessage chatMessage = (ChatMessage) message;
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
        String id = ChatNotification.createId(chatChannel.getId(), chatMessage.getId());
        ChatNotification chatNotification = createNotification(id, chatChannel, chatMessage);

        // At first start-up when user has not setup their profile yet, we set all notifications as consumed
        if (!userIdentityService.hasUserIdentities()) {
            consumeNotification(chatNotification);
            return;
        }

        if (isConsumed(chatNotification)) {
            return;
        }

        if (chatMessage.isMyMessage(userIdentityService)) {
            return;
        }

        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId())) {
            return;
        }

        // For BisqEasyOfferbookChannels we add it to consumed to not get them shown when switching to a new channel
        if (chatChannel instanceof BisqEasyOfferbookChannel &&
                !chatService.getChatChannelSelectionService(chatChannel.getChatChannelDomain()).getSelectedChannel().get().equals(chatChannel)) {
            consumeNotification(chatNotification);
            return;
        }

        // If user has set "Show offers only" in settings we mark messages as consumed
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
            if (settingsService.getOffersOnly().get() && !bisqEasyOfferbookMessage.hasBisqEasyOffer()) {
                consumeNotification(chatNotification);
                return;
            }
        }

        ChatChannelNotificationType notificationType = chatChannel.getChatChannelNotificationType().get();
        if (notificationType == ChatChannelNotificationType.GLOBAL_DEFAULT) {
            notificationType = ChatChannelNotificationType.fromChatNotificationType(settingsService.getChatNotificationType().get());
        }
        boolean shouldSendNotification;
        switch (notificationType) {
            case GLOBAL_DEFAULT:
                throw new RuntimeException("GLOBAL_DEFAULT not possible here");
            case ALL:
                shouldSendNotification = true;
                break;
            case MENTION:
                // We treat citations also like mentions
                shouldSendNotification = userIdentityService.getUserIdentities().stream()
                        .anyMatch(userIdentity -> chatMessage.wasMentioned(userIdentity) ||
                                chatMessage.wasCited(userIdentity));
                break;
            case OFF:
            default:
                shouldSendNotification = false;
                break;
        }

        if (shouldSendNotification) {
            addNotification(chatNotification);
            sendNotificationService.send(chatNotification);
        } else {
            consumeNotification(chatNotification);
        }
    }

    private <M extends ChatMessage> ChatNotification createNotification(String id, ChatChannel<M> chatChannel, M chatMessage) {
        Optional<UserProfile> senderUserProfile = chatMessage instanceof PrivateChatMessage ?
                Optional.of(((PrivateChatMessage) chatMessage).getSenderUserProfile()) :
                userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
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
            title = StringUtils.truncate(userName, 15) + " (" + channelInfo + ")";
            message = StringUtils.truncate(chatMessage.getText(), 210);
        }
        return new ChatNotification(id,
                title,
                message,
                chatChannel,
                chatMessage,
                senderUserProfile);
    }
}
