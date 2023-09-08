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
import bisq.chat.pub.PublicChatMessage;
import bisq.common.application.Service;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Handles chat notifications
 */
@Slf4j
public class ChatNotificationService implements Service {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Static
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // title can have "-" as separator
    // channelId is "[ChatChannelDomain].[title]"
    // notificationId is "[ChatChannelDomain].[title].[messageId]"
    public static String createNotificationId(String channelId, String messageId) {
        return channelId + "." + messageId;
    }

    public static String getChatChannelId(String notificationId) {
        String[] tokens = notificationId.split("\\.");
        checkArgument(tokens.length == 3, "unexpected tokens size. notificationId=" + notificationId);
        return tokens[0] + "." + tokens[1];
    }

    public static String getChatMessageId(String notificationId) {
        String[] tokens = notificationId.split("\\.");
        checkArgument(tokens.length == 3, "unexpected tokens size. notificationId=" + notificationId);
        return tokens[2];
    }

    public static ChatChannelDomain getChatChannelDomain(String notificationId) {
        String channelId = getChatChannelId(notificationId);
        String[] tokens = channelId.split("\\.");
        checkArgument(tokens.length == 2, "unexpected tokens size. notificationId=" + notificationId);
        return ChatChannelDomain.valueOf(tokens[0].toUpperCase());
    }

    public static String getChatChannelTitle(String notificationId) {
        String channelId = getChatChannelId(notificationId);
        String[] tokens = channelId.split("\\.");
        checkArgument(tokens.length == 2, "unexpected tokens size. notificationId=" + notificationId);
        return tokens[1];
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private final ChatService chatService;
    private final NotificationsService notificationsService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final Map<String, Pin> chatMessagesByChannelIdPins = new ConcurrentHashMap<>();

    public ChatNotificationService(ChatService chatService,
                                   NotificationsService notificationsService,
                                   SettingsService settingsService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService) {
        this.chatService = chatService;
        this.notificationsService = notificationsService;
        this.settingsService = settingsService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        bisqEasyOpenTradeChannelService.getChannels().addListener(() ->
                onChatChannelsChanged(bisqEasyOpenTradeChannelService.getChannels()));

        BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookChannelService.getChannels().addListener(() ->
                onChatChannelsChanged(bisqEasyOfferbookChannelService.getChannels()));

        chatService.getCommonPublicChatChannelServices().values()
                .forEach(commonPublicChatChannelService -> {
                    commonPublicChatChannelService.getChannels().addListener(() ->
                            onChatChannelsChanged(commonPublicChatChannelService.getChannels()));
                });

        chatService.getTwoPartyPrivateChatChannelServices().values()
                .forEach(twoPartyPrivateChatChannelService -> {
                    twoPartyPrivateChatChannelService.getChannels().addListener(() ->
                            onChatChannelsChanged(twoPartyPrivateChatChannelService.getChannels()));
                });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void consumeNotificationId(ChatChannel<? extends ChatMessage> chatChannel) {
        chatChannel.getChatMessages()
                .stream()
                .map(chatMessage -> createNotificationId(chatChannel.getId(),
                        chatMessage.getId()))
                .forEach(notificationsService::consumeNotificationId);
    }

    public int getNumNotificationsByDomain(ChatChannelDomain chatChannelDomain) {
        return (int) notificationsService.getNotConsumedNotificationIds().stream()
                .filter(notificationId -> chatChannelDomain == getChatChannelDomain(notificationId))
                .count();
    }

    public <C extends ChatChannel<?>> Integer getNumNotificationsByChannel(C chatChannel) {
        return (int) notificationsService.getNotConsumedNotificationIds().stream()
                .filter(notificationId -> chatChannel.getId().equals(getChatChannelId(notificationId)))
                .count();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private <M extends ChatMessage> void onChatChannelsChanged(ObservableArray<? extends ChatChannel<M>> channels) {
        channels.forEach(chatChannel -> {
            String channelId = chatChannel.getId();
            if (chatMessagesByChannelIdPins.containsKey(channelId)) {
                chatMessagesByChannelIdPins.get(channelId).unbind();
            }
            Pin pin = chatChannel.getChatMessages().addListener(new CollectionObserver<>() {
                @Override
                public void add(M message) {
                    chatNotificationAdded(chatChannel, message);
                }

                @Override
                public void remove(Object message) {
                    if (message instanceof ChatMessage) {
                        ChatMessage chatMessage = (ChatMessage) message;
                        String notificationId = createNotificationId(chatChannel.getId(), chatMessage.getId());
                        notificationsService.removeNotificationId(notificationId);
                    }
                }

                @Override
                public void clear() {
                    String channelId = chatChannel.getId();
                    Set<String> toRemove = notificationsService.getAllNotificationIds().stream()
                            .filter(notificationId -> channelId.equals(getChatChannelId(notificationId)))
                            .collect(Collectors.toSet());
                    toRemove.forEach(notificationsService::removeNotificationId);
                }
            });
            chatMessagesByChannelIdPins.put(channelId, pin);
        });
    }

    private <M extends ChatMessage> void chatNotificationAdded(ChatChannel<M> chatChannel, M chatMessage) {
        String notificationId = createNotificationId(chatChannel.getId(), chatMessage.getId());
        if (notificationsService.containsNotificationId(notificationId)) {
            return;
        }

        if (chatMessage.isMyMessage(userIdentityService)) {
            return;
        }

        // If user is ignored we do not notify, but we still keep the messageIds to not trigger 
        // notifications after un-ignore.
        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId())) {
            return;
        }

        notificationsService.addNotificationId(notificationId);

        if (userIdentityService.hasUserIdentities()) {
            ChatNotification<M> chatNotification = new ChatNotification<>(userProfileService,
                    notificationId,
                    chatChannel,
                    chatMessage);
            maybeSendNotification(chatChannel, chatMessage, notificationId, chatNotification);
        } else {
            notificationsService.consumeNotificationId(notificationId);
        }
    }

    private <M extends ChatMessage> void maybeSendNotification(ChatChannel<M> chatChannel,
                                                               M chatMessage,
                                                               String notificationId,
                                                               ChatNotification<M> chatNotification) {
        ChatChannelNotificationType chatChannelNotificationType = chatChannel.getChatChannelNotificationType().get();
        if (chatChannelNotificationType == ChatChannelNotificationType.GLOBAL_DEFAULT) {
            // Map from global settings enums
            switch (settingsService.getChatNotificationType().get()) {
                case ALL:
                    chatChannelNotificationType = ChatChannelNotificationType.ALL;
                    break;
                case MENTION:
                    chatChannelNotificationType = ChatChannelNotificationType.MENTION;
                    break;
                case OFF:
                    chatChannelNotificationType = ChatChannelNotificationType.OFF;
                    break;
            }
        }
        switch (chatChannelNotificationType) {
            case GLOBAL_DEFAULT:
                throw new RuntimeException("GLOBAL_DEFAULT not possible here");
            case ALL:
                break;
            case MENTION:
                if (userIdentityService.getUserIdentities().stream()
                        .noneMatch(chatMessage::wasMentioned)) {
                    return;
                }
                break;
            case OFF:
            default:
                return;
        }
        String channelInfo;
        String title;
        if (chatMessage instanceof BisqEasyOpenTradeMessage) {
            BisqEasyOpenTradeMessage bisqEasyOpenTradeMessage = (BisqEasyOpenTradeMessage) chatMessage;
            if (bisqEasyOpenTradeMessage.getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                BisqEasyOpenTradeChannel privateTradeChannel = (BisqEasyOpenTradeChannel) chatChannel;
                title = Res.get("chat.notifications.offerTaken", privateTradeChannel.getPeer().getUserName());
                notificationsService.sendNotification(notificationId, title, "");
                return;
            }
        }

        if (chatMessage instanceof PublicChatMessage) {
            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                if (settingsService.getOffersOnly().get() && !bisqEasyOfferbookMessage.hasBisqEasyOffer()) {
                    return;
                }
                BisqEasyOfferbookChannel bisqEasyOfferbookChannel = (BisqEasyOfferbookChannel) chatChannel;
                if (!chatService.getBisqEasyOfferbookChannelService().isVisible(bisqEasyOfferbookChannel)) {
                    return;
                }
            }
            channelInfo = chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse(Res.get("data.na"));
        } else {
            channelInfo = chatChannel.getChatChannelDomain().getDisplayString() + " - " + Res.get("chat.notifications.privateMessage.headline");
        }
        title = StringUtils.truncate(chatNotification.getUserName(), 15) + " (" + channelInfo + ")";
        notificationsService.sendNotification(notificationId, title, chatNotification.getMessage());
    }
}