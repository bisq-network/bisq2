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

package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ChatContainerController extends ContentTabController<ChatContainerModel> {
    protected final ChatService chatService;
    @Getter
    private final ChatContainerView view;
    private final ChatNotificationService chatNotificationService;
    protected final ChatChannelDomain channelDomain;
    private final CommonPublicChatChannelService chatChannelService;
    private final ChatChannelSelectionService chatChannelSelectionService;
    private Pin selectedChannelPin;
    private final Map<String, Pin> pinByChannelId = new HashMap<>();
    private final Map<Channel, Pin> channelNumNotificationsPin = new HashMap<>();

    public ChatContainerController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(new ChatContainerModel(chatChannelDomain), navigationTarget, serviceProvider);

        chatService = serviceProvider.getChatService();
        //notificationsService = serviceProvider.
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        channelDomain = chatChannelDomain;
        // chatChannel
        chatChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
                //chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);

        createChannels();
        view = new ChatContainerView(model, this);
    }

    private void createChannels() {
        chatChannelService.getChannels().forEach(commonPublicChatChannel -> {
            Channel channel = findOrCreateChannelItem(commonPublicChatChannel);
            if (channel != null) {
                model.channels.add(channel);
            }
        });
    }

    private Channel findOrCreateChannelItem(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof CommonPublicChatChannel) {
            CommonPublicChatChannel commonChannel = (CommonPublicChatChannel) chatChannel;
            String targetName = channelDomain.toString() + "_" + commonChannel.getChannelTitle().toUpperCase();
            try {
                NavigationTarget navigationTarget = NavigationTarget.valueOf(targetName);
                return model.channels.stream()
                        .filter(Objects::nonNull)
                        .filter(item -> item.getChatChannel().getId().equals(commonChannel.getId()))
                        .findAny()
                        .orElseGet(() -> new Channel(commonChannel, chatChannelService, navigationTarget, chatNotificationService));
            } catch (IllegalArgumentException e) {
                // Log or handle the navigation target not found case
            }
        }
        return null;
    }


    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = FxBindings.subscribe(chatChannelSelectionService.getSelectedChannel(),
                channel -> UIThread.run(() -> handleSelectedChannelChange(channel)));

        model.getChannels().forEach(channel -> {
            //channel.updateNumNotifications(chatNotificationService.getChangedNotification().get());
            Pin pin = channel.getNumNotifications().addObserver(newCount -> {
                if (newCount != null) {
                    updateTabButtonNotifications(channel, newCount);
                }
            });
            channelNumNotificationsPin.put(channel, pin);
        });
    }

    private void updateTabButtonNotifications(Channel channel, long newCount) {
        model.getTabButtons().stream()
                .filter(tabButton -> channel.getNavigationTarget() == tabButton.getNavigationTarget())
                .findAny()
                .ifPresent(tabButton -> tabButton.setNumNotifications(newCount));
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        selectedChannelPin.unbind();
        channelNumNotificationsPin.values().forEach(Pin::unbind);
        channelNumNotificationsPin.clear();
        model.channels.forEach(Channel::dispose);
    }

    protected void handleSelectedChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
        Channel channel = findOrCreateChannelItem(chatChannel);
        if (channel != null) {
            model.selectedChannel.set(channel);

            if (model.previousSelectedChannel != null) {
                model.previousSelectedChannel.setSelected(false);
            }
            model.previousSelectedChannel = channel;

            channel.setSelected(true);
        }
    }

    protected void onSelected(NavigationTarget navigationTarget) {
        Optional<Channel> channel = model.channels.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getNavigationTarget().equals(navigationTarget))
                .findFirst();
        chatChannelSelectionService.selectChannel(
                channel.<ChatChannel<? extends ChatMessage>>map(Channel::getChatChannel).orElse(null));
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case DISCUSSION_BISQ:
            case DISCUSSION_BITCOIN:
            case DISCUSSION_MARKETS:
            case DISCUSSION_ECONOMY:
            case DISCUSSION_OFFTOPIC: {
                return Optional.of(new CommonChatController(serviceProvider, channelDomain, navigationTarget));
            }
//            case DISCUSSION_PRIVATECHATS: {
//                onSelected(navigationTarget);
//                // TODO: Make new class for private chats
//                return Optional.of(new CommonChatController(serviceProvider, channelDomain, navigationTarget));
//            }
            default: {
                return Optional.empty();
            }
        }
    }
}
