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

package bisq.desktop.primary.main.content.events;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatDomain;
import bisq.chat.channel.Channel;
import bisq.chat.channel.PrivateChannel;
import bisq.chat.channel.PublicChannel;
import bisq.chat.channel.private_two_party.PrivateTwoPartyChannel;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.events.pub.PublicEventsChannelService;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicEventsChannelSelection;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public class EventsController extends ChatController<EventsView, EventsModel> implements Controller {
    private final PublicEventsChannelService publicEventsChannelService;
    private final EventsChannelSelectionService eventsChannelSelectionService;
    private PublicEventsChannelSelection publicEventsChannelSelection;

    public EventsController(DefaultApplicationService applicationService) {
        super(applicationService, ChatDomain.EVENTS, NavigationTarget.NONE);

        publicEventsChannelService = chatService.getPublicEventsChannelService();
        eventsChannelSelectionService = chatService.getEventsChannelSelectionService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        notificationSettingSubscription = EasyBind.subscribe(channelSidebar.getSelectedNotificationType(),
                value -> {
                    Channel<? extends ChatMessage> channel = eventsChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicEventsChannelService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = eventsChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    public void createComponents() {
        publicEventsChannelSelection = new PublicEventsChannelSelection(applicationService);
    }

    @Override
    public EventsModel getChatModel(ChatDomain chatDomain) {
        return new EventsModel(chatDomain);
    }

    @Override
    public EventsView getChatView() {
        return new EventsView(model,
                this,
                publicEventsChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateTwoPartyChannel) {
            applyPeersIcon((PrivateChannel<?>) channel);
            publicEventsChannelSelection.deSelectChannel();
        } else {
            applyDefaultPublicChannelIcon((PublicChannel<?>) channel);
            privateChannelSelection.deSelectChannel();
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }
}
