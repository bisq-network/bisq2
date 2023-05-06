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
import bisq.chat.channel.*;
import bisq.chat.message.*;
import bisq.chat.trade.channel.PrivateTradeChannel;
import bisq.chat.trade.channel.PrivateTradeChannelService;
import bisq.chat.trade.channel.PublicTradeChannel;
import bisq.chat.trade.channel.PublicTradeChannelService;
import bisq.chat.trade.message.PrivateTradeChatMessage;
import bisq.chat.trade.message.PublicTradeChatMessage;
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
    private final NotificationsService notificationsService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;

    private final PrivateTradeChannelService privateTradeChannelService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final PrivateTwoPartyChannelService privateDiscussionChannelService;
    private final PublicChatChannelService publicDiscussionChannelService;
    private final PrivateTwoPartyChannelService privateEventsChannelService;
    private final PublicChatChannelService publicEventsChannelService;
    private final PrivateTwoPartyChannelService privateSupportChannelService;
    private final PublicChatChannelService publicSupportChannelService;

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
        this.notificationsService = notificationsService;
        this.settingsService = settingsService;

        privateTradeChannelService = chatService.getPrivateTradeChannelService();
        publicTradeChannelService = chatService.getPublicTradeChannelService();

        privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
        publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();

        privateEventsChannelService = chatService.getPrivateEventsChannelService();
        publicEventsChannelService = chatService.getPublicEventsChannelService();

        privateSupportChannelService = chatService.getPrivateSupportChannelService();
        publicSupportChannelService = chatService.getPublicSupportChannelService();

        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();

        chatMessages.addListener((ListChangeListener<ChatNotification<? extends ChatMessage>>) c -> {
            c.next();
            if (c.wasAdded()) {
                c.getAddedSubList().forEach(this::onChatNotificationAdded);
            }
        });

        sortedChatMessages.setComparator(ChatNotification::compareTo);

        privateTradeChannelService.getChannels().addListener(() ->
                onPrivateTradeChannelsChanged(privateTradeChannelService.getChannels()));
        publicTradeChannelService.getChannels().addListener(() ->
                onPublicTradeChannelsChanged(publicTradeChannelService.getChannels()));

        privateDiscussionChannelService.getChannels().addListener(() ->
                onPrivateChannelsChanged(privateDiscussionChannelService.getChannels()));
        publicDiscussionChannelService.getChannels().addListener(() ->
                onPublicChannelsChanged(publicDiscussionChannelService.getChannels()));

        privateEventsChannelService.getChannels().addListener(() ->
                onPrivateChannelsChanged(privateEventsChannelService.getChannels()));
        publicEventsChannelService.getChannels().addListener(() ->
                onPublicChannelsChanged(publicEventsChannelService.getChannels()));

        privateSupportChannelService.getChannels().addListener(() ->
                onPrivateChannelsChanged(privateSupportChannelService.getChannels()));
        publicSupportChannelService.getChannels().addListener(() ->
                onPublicChannelsChanged(publicSupportChannelService.getChannels()));
    }


    private void onChatNotificationAdded(ChatNotification<? extends ChatMessage> chatNotification) {
        ChatMessage chatMessage = chatNotification.getChatMessage();
        boolean isMyMessage = userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        if (isMyMessage) {
            return;
        }

        String id = chatMessage.getMessageId();
        if (notificationsService.contains(id)) {
            return;
        }

        notificationsService.add(id);

        // If user is ignored we do not notify, but we still keep the messageIds to not trigger 
        // notifications after un-ignore.
        if (userProfileService.isChatUserIgnored(chatMessage.getAuthorId())) {
            return;
        }

        ChatChannel<? extends ChatMessage> chatChannel = chatNotification.getChatChannel();
        ChannelNotificationType channelNotificationType = chatChannel.getChannelNotificationType().get();
        if (channelNotificationType == ChannelNotificationType.GLOBAL_DEFAULT) {
            // Map from global settings enums
            switch (settingsService.getChatNotificationType().get()) {
                case ALL:
                    channelNotificationType = ChannelNotificationType.ALL;
                    break;
                case MENTION:
                    channelNotificationType = ChannelNotificationType.MENTION;
                    break;
                case OFF:
                    channelNotificationType = ChannelNotificationType.OFF;
                    break;
            }
        }
        switch (channelNotificationType) {
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
        if (chatMessage instanceof PrivateTradeChatMessage) {
            PrivateTradeChatMessage privateTradeChatMessage = (PrivateTradeChatMessage) chatMessage;
            if (privateTradeChatMessage.getMessageType() == MessageType.TAKE_OFFER) {
                PrivateTradeChannel privateTradeChannel = (PrivateTradeChannel) chatChannel;
                String msg = privateTradeChannel.getPeer().getUserName() + ":\n" + chatNotification.getMessage();
                title = Res.get("takeOfferMessage");
                notificationsService.notify(title, msg);
                return;
            }
        }

        if (chatMessage instanceof PublicChatMessage) {
            if (chatMessage instanceof PublicTradeChatMessage) {
                PublicTradeChatMessage publicTradeChatMessage = (PublicTradeChatMessage) chatMessage;
                if (settingsService.getOffersOnly().get() && !publicTradeChatMessage.hasTradeChatOffer()) {
                    return;
                }
                PublicTradeChannel publicTradeChannel = (PublicTradeChannel) chatChannel;
                if (!publicTradeChannelService.isVisible(publicTradeChannel)) {
                    return;
                }
            }
            channelInfo = chatChannel.getDisplayString();
        } else {
            // All PrivateChatMessages excluding PrivateTradeChatMessage
            channelInfo = Res.get(chatChannel.getChannelDomain().name().toLowerCase()) + " - " + Res.get("privateMessage");
        }
        title = StringUtils.abbreviate(chatNotification.getUserName(), 15) + " (" + channelInfo + ")";
        notificationsService.notify(title, chatNotification.getMessage());
    }

    // Failed to use generics for Channel and ChatMessage with FxBindings, 
    // thus we have boilerplate methods here...

    private void onPrivateTradeChannelsChanged(ObservableArray<PrivateTradeChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<PrivateTradeChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onPublicTradeChannelsChanged(ObservableArray<PublicTradeChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<PublicTradeChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onPrivateChannelsChanged(ObservableArray<PrivateTwoPartyChannel> channels) {
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

    private void onPublicChannelsChanged(ObservableArray<PublicChatChannel> channels) {
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