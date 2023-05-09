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

package bisq.desktop.notifications.chat;

import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelNotificationType;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.message.*;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Handles chat notifications
 */
@Slf4j
public class ChatNotifications {
    private final ChatService chatService;
    private final NotificationsService notificationsService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final Map<String, Pin> pinByChannelId = new HashMap<>();
    private final ObservableList<ChatNotification<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
    private final FilteredList<ChatNotification<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
    private final SortedList<ChatNotification<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
    @Setter
    private Predicate<? super ChatNotification<? extends ChatMessage>> predicate = e -> true;

    public ChatNotifications(ChatService chatService,
                             UserService userService,
                             SettingsService settingsService,
                             NotificationsService notificationsService) {
        this.chatService = chatService;
        this.notificationsService = notificationsService;
        this.settingsService = settingsService;

        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();

        chatMessages.addListener((ListChangeListener<ChatNotification<? extends ChatMessage>>) c -> {
            if (userIdentityService.hasUserIdentities()) {
                c.next();
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(this::onChatNotificationAdded);
                }
            }
        });

        sortedChatMessages.setComparator(ChatNotification::compareTo);

        BisqEasyPrivateTradeChatChannelService bisqEasyPrivateTradeChatChannelService = chatService.getBisqEasyPrivateTradeChatChannelService();
        bisqEasyPrivateTradeChatChannelService.getChannels().addListener(() ->
                onBisqEasyPrivateTradeChatChannelsChanged(bisqEasyPrivateTradeChatChannelService.getChannels()));
        BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyPublicChatChannelService.getChannels().addListener(() ->
                onBisqEasyPublicChatChannelsChanged(bisqEasyPublicChatChannelService.getChannels()));

        chatService.getCommonPublicChatChannelServices().values()
                .forEach(commonPublicChatChannelService -> {
                    commonPublicChatChannelService.getChannels().addListener(() ->
                            onCommonPublicChatChannelsChanged(commonPublicChatChannelService.getChannels()));
                });

        chatService.getTwoPartyPrivateChatChannelServices().values()
                .forEach(twoPartyPrivateChatChannelService -> {
                    twoPartyPrivateChatChannelService.getChannels().addListener(() ->
                            onTwoPartyPrivateChatChannelsChanged(twoPartyPrivateChatChannelService.getChannels()));
                });
    }

    private void onChatNotificationAdded(ChatNotification<? extends ChatMessage> chatNotification) {
        ChatMessage chatMessage = chatNotification.getChatMessage();
        boolean isMyMessage = userIdentityService.isUserIdentityPresent(chatMessage.getAuthorUserProfileId());
        if (isMyMessage) {
            return;
        }

        String id = chatMessage.getId();
        if (notificationsService.contains(id)) {
            return;
        }

        notificationsService.add(id);

        // If user is ignored we do not notify, but we still keep the messageIds to not trigger 
        // notifications after un-ignore.
        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorUserProfileId())) {
            return;
        }

        ChatChannel<? extends ChatMessage> chatChannel = chatNotification.getChatChannel();
        if (chatChannel == null) {
            return;
        }

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
                notificationsService.notify(title, msg);
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
        notificationsService.notify(title, chatNotification.getMessage());
    }

    // Failed to use generics for Channel and ChatMessage with FxBindings, 
    // thus we have boilerplate methods here...

    private void onBisqEasyPrivateTradeChatChannelsChanged(ObservableArray<BisqEasyPrivateTradeChatChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<BisqEasyPrivateTradeChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onBisqEasyPublicChatChannelsChanged(ObservableArray<BisqEasyPublicChatChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<BisqEasyPublicChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onTwoPartyPrivateChatChannelsChanged(ObservableArray<TwoPartyPrivateChatChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<TwoPartyPrivateChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onCommonPublicChatChannelsChanged(ObservableArray<CommonPublicChatChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<CommonPublicChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }
}