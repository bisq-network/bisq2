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

package bisq.desktop.main.content.chat.tab;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.chat.chats.CommonPublicChatController;
import bisq.desktop.main.content.chat.chats.PrivateChatsController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public final class CommonChatTabController extends ContentTabController<CommonChatTabModel> {
    @Getter
    private final CommonChatTabView view;
    private final ChatNotificationService chatNotificationService;
    private final ChatChannelDomain channelDomain;
    private final CommonPublicChatChannelService commonPublicChatChannelService;
    private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
    private final ChatChannelSelectionService chatChannelSelectionService;
    private final ChatToolbox chatToolbox;
    private Pin selectedChannelPin;
    private Pin changedChatNotificationPin;

    public CommonChatTabController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(new CommonChatTabModel(chatChannelDomain), navigationTarget, serviceProvider);

        ChatService chatService = serviceProvider.getChatService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        channelDomain = chatChannelDomain;
        commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        twoPartyPrivateChatChannelService = chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);

        createChannels();

        chatToolbox = new ChatToolbox();
        view = new CommonChatTabView(model, this, chatToolbox.getRoot());

        model.getSelectedTabButton().addListener(observable -> {
            TabButton tabButton = model.getSelectedTabButton().get();
            boolean noSelectedChannel = tabButton.getNavigationTarget() == model.getPrivateChatsNavigationTarget()
                    && twoPartyPrivateChatChannelService.getChannels().isEmpty();
            model.getHasSelectedChannel().set(!noSelectedChannel);
        });
    }

    private void createChannels() {
        commonPublicChatChannelService.getChannels().forEach(commonPublicChatChannel -> {
            Channel channel = findOrCreateChannelItem(commonPublicChatChannel);
            if (channel != null) {
                model.channels.put(channel.getChannelId(), channel);
            }
        });
    }

    private Channel findOrCreateChannelItem(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof CommonPublicChatChannel) {
            CommonPublicChatChannel commonChannel = (CommonPublicChatChannel) chatChannel;
            if (model.channels.containsKey(chatChannel.getId())) {
                return model.channels.get(chatChannel.getId());
            } else {
                String targetName = channelDomain.toString() + "_" + commonChannel.getChannelTitle().toUpperCase();
                try {
                    return new Channel(commonChannel, commonPublicChatChannelService, NavigationTarget.valueOf(targetName));
                } catch (IllegalArgumentException e) {
                    log.info("Couldn't find navigation target " + targetName + " in channel domain " + channelDomain);
                }
            }
        }
        return null;
    }

    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = FxBindings.subscribe(chatChannelSelectionService.getSelectedChannel(),
                channel -> UIThread.run(() -> handleSelectedChannelChanged(channel)));
        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        selectedChannelPin.unbind();
        changedChatNotificationPin.unbind();
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || notification.getChatChannelDomain() != channelDomain) {
            return;
        }

        String channelId = notification.getChatChannelId();
        if (isPrivateChannel(channelId)) {
            handlePrivateNotification();
        }

        if (model.channels.containsKey(channelId)) {
            updateTabButtonNotifications(channelId, chatNotificationService.getNumNotifications(channelId));
        }
    }

    private void updateTabButtonNotifications(String channelId, long newCount) {
        UIThread.run(() -> {
            Channel channel = model.channels.get(channelId);
            model.getTabButtons().stream()
                    .filter(tabButton -> channel.getNavigationTarget() == tabButton.getNavigationTarget())
                    .findAny()
                    .ifPresent(tabButton -> tabButton.setNumNotifications(newCount));
        });
    }

    private boolean isPrivateChannel(String channelId) {
        return twoPartyPrivateChatChannelService.findChannel(channelId).isPresent();
    }

    private void handlePrivateNotification() {
        AtomicLong count = new AtomicLong();
        twoPartyPrivateChatChannelService.getChannels().forEach(channel ->
                count.addAndGet(chatNotificationService.getNotConsumedNotifications(channel.getId()).count()));
        UIThread.run(() ->
                model.getTabButtons().stream()
                        .filter(tabButton -> model.getPrivateChatsNavigationTarget() == tabButton.getNavigationTarget())
                        .findAny()
                        .ifPresent(tabButton -> tabButton.setNumNotifications(count.get()))
        );
    }

    protected void handleSelectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        chatToolbox.resetSearchText();

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
        model.channels.values().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getNavigationTarget().equals(navigationTarget))
                .findFirst()
                .<ChatChannel<? extends ChatMessage>>map(Channel::getChatChannel)
                .ifPresent(chatChannelSelectionService::selectChannel);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case DISCUSSION_BISQ:
            case DISCUSSION_BITCOIN:
            case DISCUSSION_MARKETS:
            case DISCUSSION_OFFTOPIC:
            case EVENTS_CONFERENCES:
            case EVENTS_MEETUPS:
            case EVENTS_PODCASTS:
            case EVENTS_TRADEEVENTS:
            case SUPPORT_SUPPORT:
            case SUPPORT_QUESTIONS:
            case SUPPORT_REPORTS: {
                return Optional.of(new CommonPublicChatController(serviceProvider, channelDomain, navigationTarget, Optional.of(chatToolbox)));
            }
            case DISCUSSION_PRIVATECHATS:
            case EVENTS_PRIVATECHATS:
            case SUPPORT_PRIVATECHATS: {
                return Optional.of(new PrivateChatsController(serviceProvider, channelDomain, navigationTarget, Optional.of(chatToolbox)));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
