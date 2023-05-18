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

import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.PublicChatMessage;
import bisq.common.application.Service;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Handles chat notifications
 */
@Slf4j
public class ChatNotificationService implements Service {
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

    private final ChatService chatService;
    private final NotificationsService notificationsService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    @Getter
    private final ObservableSet<ChatNotification<? extends ChatMessage>> chatNotifications = new ObservableSet<>();

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

    @Override
    public CompletableFuture<Boolean> initialize() {
        BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        bisqEasyPrivateTradeChatChannelService.getChannels().addListener(() ->
                onChatChannelsChanged(bisqEasyPrivateTradeChatChannelService.getChannels()));
        BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyPublicChatChannelService.getChannels().addListener(() ->
                onChatChannelsChanged(bisqEasyPublicChatChannelService.getChannels()));

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

    private void onChatNotificationAdded(ChatNotification<? extends ChatMessage> chatNotification) {
        ChatMessage chatMessage = chatNotification.getChatMessage();
        if (chatMessage.isMyMessage(userIdentityService)) {
            return;
        }

        // If user is ignored we do not notify, but we still keep the messageIds to not trigger 
        // notifications after un-ignore.
        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId())) {
            return;
        }

        ChatChannel<? extends ChatMessage> chatChannel = chatNotification.getChatChannel();
        if (chatChannel == null) {
            return;
        }

        String messageId = chatMessage.getId();
        String notificationId = createNotificationId(chatChannel.getId(), messageId);
        if (notificationsService.contains(notificationId)) {
            return;
        }
        notificationsService.add(notificationId);

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
        if (chatMessage instanceof BisqEasyPrivateTradeChatMessage) {
            BisqEasyPrivateTradeChatMessage bisqEasyPrivateTradeChatMessage = (BisqEasyPrivateTradeChatMessage) chatMessage;
            if (bisqEasyPrivateTradeChatMessage.getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                BisqEasyPrivateTradeChatChannel privateTradeChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                String msg = privateTradeChannel.getPeer().getUserName() + ":\n" + chatNotification.getMessage();
                title = Res.get("takeOfferMessage");
                notificationsService.notify(notificationId, title, msg);
                return;
            }
        }

        if (chatMessage instanceof PublicChatMessage) {
            if (chatMessage instanceof BisqEasyPublicChatMessage) {
                BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) chatMessage;
                if (settingsService.getOffersOnly().get() && !bisqEasyPublicChatMessage.hasTradeChatOffer()) {
                    return;
                }
                BisqEasyPublicChatChannel bisqEasyPublicChatChannel = (BisqEasyPublicChatChannel) chatChannel;
                if (!chatService.getBisqEasyPublicChatChannelService().isVisible(bisqEasyPublicChatChannel)) {
                    return;
                }
            }
            channelInfo = chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElseThrow();
        } else {
            // All PrivateChatMessages excluding PrivateTradeChatMessage
            channelInfo = chatChannel.getChatChannelDomain().getDisplayString() + " - " + Res.get("privateMessage");
        }
        title = StringUtils.truncate(chatNotification.getUserName(), 15) + " (" + channelInfo + ")";
        notificationsService.notify(notificationId, title, chatNotification.getMessage());
    }

    private <M extends ChatMessage> void onChatChannelsChanged(ObservableArray<? extends ChatChannel<M>> channels) {
        channels.forEach(channel -> {
            channel.getChatMessages().addListener(new CollectionObserver<>() {
                @Override
                public void add(M message) {
                    ChatNotification<M> notification = new ChatNotification<>(channel, message, userProfileService);
                    chatNotifications.add(notification);
                    onChatNotificationAdded(notification);
                }

                @Override
                public void remove(Object message) {
                    if (message instanceof BisqEasyPrivateTradeChatMessage) {
                        ChatNotification<BisqEasyPrivateTradeChatMessage> notification = new ChatNotification<>(channel, (BisqEasyPrivateTradeChatMessage) message, userProfileService);
                        chatNotifications.remove(notification);
                        onChatNotificationAdded(notification);
                    }
                }

                @Override
                public void clear() {
                    chatNotifications.clear();
                }
            });
        });
    }

    public int getNumNotifications(ChatChannelDomain chatChannelDomain) {
        String domain = chatChannelDomain.name().toLowerCase();
        return (int) notificationsService.getNotificationIds().stream()
                .filter(notificationId -> notificationId.split("\\.")[0].equals(domain))
                .count();
    }
}