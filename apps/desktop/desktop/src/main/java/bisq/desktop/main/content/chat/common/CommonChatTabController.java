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

package bisq.desktop.main.content.chat.common;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.common.CommonChannelSelectionService;
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
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.chat.common.priv.CommonPrivateChatsController;
import bisq.desktop.main.content.chat.common.pub.CommonPublicChatController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public final class CommonChatTabController extends ContentTabController<CommonChatTabModel> {
    @Getter
    private final CommonChatTabView view;
    private final ChatNotificationService chatNotificationService;
    private final ChatChannelDomain channelDomain;
    private final CommonPublicChatChannelService commonPublicChatChannelService;
    private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;
    private final CommonChannelSelectionService chatChannelSelectionService;
    private Pin selectedChannelPin, changedChatNotificationPin;

    public CommonChatTabController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(new CommonChatTabModel(chatChannelDomain), navigationTarget, serviceProvider);

        ChatService chatService = serviceProvider.getChatService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        channelDomain = chatChannelDomain;
        commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        twoPartyPrivateChatChannelService = chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        chatChannelSelectionService = (CommonChannelSelectionService) chatService.getChatChannelSelectionServices().get(chatChannelDomain);

        createChannels();
        view = new CommonChatTabView(model, this);
    }

    private void createChannels() {
        commonPublicChatChannelService.getChannels().forEach(commonPublicChatChannel -> {
            ChannelTabButtonModel channelTabButtonModel = findOrCreateChannelItem(commonPublicChatChannel);
            if (channelTabButtonModel != null) {
                model.channelTabButtonModelByChannelId.put(channelTabButtonModel.getChannelId(), channelTabButtonModel);
            }
        });
    }

    private ChannelTabButtonModel findOrCreateChannelItem(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof CommonPublicChatChannel commonChannel) {
            if (model.channelTabButtonModelByChannelId.containsKey(chatChannel.getId())) {
                return model.channelTabButtonModelByChannelId.get(chatChannel.getId());
            } else {
                String targetName = channelDomain.toString() + "_" + commonChannel.getChannelTitle().toUpperCase();
                try {
                    return new ChannelTabButtonModel(commonChannel, NavigationTarget.valueOf(targetName), commonPublicChatChannelService);
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
        ChatChannelDomain chatChannelDomain = notification.getChatChannelDomain();
        if (isPrivateChannelPresent(channelId)) {
            handlePrivateNotification();
        }

        if (model.channelTabButtonModelByChannelId.containsKey(channelId)) {
            updateTabButtonNotifications(chatChannelDomain, channelId);
        }
    }

    private void updateTabButtonNotifications(ChatChannelDomain chatChannelDomain, String channelId) {
        UIThread.run(() -> {
            ChannelTabButtonModel channelTabButtonModel = model.channelTabButtonModelByChannelId.get(channelId);
            model.getTabButtons().stream()
                    .filter(tabButton -> channelTabButtonModel.getNavigationTarget() == tabButton.getNavigationTarget())
                    .findAny()
                    .ifPresent(tabButton -> tabButton.setNumNotifications(chatNotificationService.getNumNotifications(chatChannelDomain, channelId)));
        });
    }

    private boolean isPrivateChannelPresent(String channelId) {
        return twoPartyPrivateChatChannelService.findChannel(channelId).isPresent();
    }

    private void handlePrivateNotification() {
        UIThread.run(() -> {
            long numNotifications = twoPartyPrivateChatChannelService.getChannels().stream()
                    .flatMap(chatNotificationService::getNotConsumedNotifications)
                    .count();
                    model.getTabButtons().stream()
                            .filter(tabButton -> model.getPrivateChatsNavigationTarget() == tabButton.getNavigationTarget())
                            .findAny()
                            .ifPresent(tabButton -> tabButton.setNumNotifications(numNotifications));
                }
        );
    }

    void handleSelectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        ChannelTabButtonModel channelTabButtonModel = findOrCreateChannelItem(chatChannel);
        if (channelTabButtonModel != null) {
            model.selectedChannelTabButtonModel.set(channelTabButtonModel);

            if (model.previousSelectedChannelTabButtonModel != null) {
                model.previousSelectedChannelTabButtonModel.setSelected(false);
            }
            model.previousSelectedChannelTabButtonModel = channelTabButtonModel;

            channelTabButtonModel.setSelected(true);
        }
    }

    void onSelected(NavigationTarget navigationTarget) {
        if (navigationTarget == model.getPrivateChatsNavigationTarget()) {
            chatChannelSelectionService.selectChannel(chatChannelSelectionService.getLastSelectedPrivateChannel().orElse(null));
        } else {
            model.channelTabButtonModelByChannelId.values().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getNavigationTarget().equals(navigationTarget))
                    .findFirst()
                    .<ChatChannel<? extends ChatMessage>>map(ChannelTabButtonModel::getChatChannel)
                    .ifPresent(chatChannelSelectionService::selectChannel);
        }
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
                return Optional.of(new CommonPublicChatController(serviceProvider, channelDomain, navigationTarget));
            }
            case DISCUSSION_PRIVATECHATS:
            case EVENTS_PRIVATECHATS:
            case SUPPORT_PRIVATECHATS: {
                return Optional.of(new CommonPrivateChatsController(serviceProvider, channelDomain, navigationTarget));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
