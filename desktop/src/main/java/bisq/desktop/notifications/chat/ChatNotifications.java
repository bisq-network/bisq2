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
import bisq.chat.channel.PrivateChannel;
import bisq.chat.channel.PrivateChannelService;
import bisq.chat.channel.PublicChannel;
import bisq.chat.channel.PublicChannelService;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.PrivateChatMessage;
import bisq.chat.message.PublicChatMessage;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.chat.trade.priv.PrivateTradeChatMessage;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.common.observable.ObservableArray;
import bisq.common.observable.Pin;
import bisq.desktop.common.notifications.Notifications;
import bisq.desktop.common.observable.FxBindings;
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

@Slf4j
public class ChatNotifications {
    private final NotificationsService notificationsService;
    private final SettingsService settingsService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;

    private final PrivateTradeChannelService privateTradeChannelService;
    private final PublicTradeChannelService publicTradeChannelService;
    private final PrivateChannelService privateDiscussionChannelService;
    private final PublicChannelService publicDiscussionChannelService;
    private final PrivateChannelService privateEventsChannelService;
    private final PublicChannelService publicEventsChannelService;
    private final PrivateChannelService privateSupportChannelService;
    private final PublicChannelService publicSupportChannelService;

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

        privateTradeChannelService.getChannels().addChangedListener(() ->
                onPrivateTradeChannelsChanged(privateTradeChannelService.getChannels()));
        publicTradeChannelService.getChannels().addChangedListener(() ->
                onPublicTradeChannelsChanged(publicTradeChannelService.getChannels()));

        privateDiscussionChannelService.getChannels().addChangedListener(() ->
                onPrivateChannelsChanged(privateDiscussionChannelService.getChannels()));
        publicDiscussionChannelService.getChannels().addChangedListener(() ->
                onPublicChannelsChanged(publicDiscussionChannelService.getChannels()));

        privateEventsChannelService.getChannels().addChangedListener(() ->
                onPrivateChannelsChanged(privateEventsChannelService.getChannels()));
        publicEventsChannelService.getChannels().addChangedListener(() ->
                onPublicChannelsChanged(publicEventsChannelService.getChannels()));

        privateSupportChannelService.getChannels().addChangedListener(() ->
                onPrivateChannelsChanged(privateSupportChannelService.getChannels()));
        publicSupportChannelService.getChannels().addChangedListener(() ->
                onPublicChannelsChanged(publicSupportChannelService.getChannels()));
    }


    private void onChatNotificationAdded(ChatNotification<? extends ChatMessage> chatNotification) {
        boolean isMyMessage = userIdentityService.isUserIdentityPresent(chatNotification.getChatMessage().getAuthorId());
        if (isMyMessage) {
            return;
        }

        boolean doNotify;
        switch (chatNotification.getChannel().getChannelNotificationType().get()) {
            case ALL:
                doNotify = true;
                break;
            case MENTION:
                doNotify = userIdentityService.getUserIdentities().stream()
                        .anyMatch(userIdentity -> chatNotification.getChatMessage().wasMentioned(userIdentity));
                break;
            case NEVER:
            default:
                doNotify = false;
                break;
        }
        if (doNotify) {
            String id = chatNotification.getChatMessage().getMessageId();
            if (!notificationsService.wasDisplayed(id)) {
                notificationsService.add(id);
                Notifications.notify(chatNotification.getUserName(), chatNotification.getMessage());
            }
        }
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

    private void onPrivateChannelsChanged(ObservableArray<PrivateChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<PrivateChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }

    private void onPublicChannelsChanged(ObservableArray<PublicChannel> channels) {
        channels.forEach(channel -> {
            String channelId = channel.getId();
            if (pinByChannelId.containsKey(channelId)) {
                pinByChannelId.get(channelId).unbind();
            }
            Pin pin = FxBindings.<PublicChatMessage,
                            ChatNotification<? extends ChatMessage>>bind(chatMessages)
                    .map(chatMessage -> new ChatNotification<>(channel, chatMessage, userProfileService))
                    .to(channel.getChatMessages());
            pinByChannelId.put(channelId, pin);
        });
    }
}