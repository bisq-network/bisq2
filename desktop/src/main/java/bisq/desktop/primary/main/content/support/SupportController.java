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

package bisq.desktop.primary.main.content.support;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.*;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicSupportChannelSelection;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public class SupportController extends ChatController<SupportView, SupportModel> implements Controller {
    private final PublicChannelService publicSupportChannelService;
    private final ChannelSelectionService supportChannelSelectionService;
    private PublicSupportChannelSelection publicSupportChannelSelection;

    public SupportController(DefaultApplicationService applicationService) {
        super(applicationService, ChannelDomain.SUPPORT, NavigationTarget.NONE);

        publicSupportChannelService = chatService.getPublicSupportChannelService();
        supportChannelSelectionService = chatService.getSupportChannelSelectionService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        notificationSettingSubscription = EasyBind.subscribe(channelSidebar.getSelectedNotificationType(),
                value -> {
                    Channel<? extends ChatMessage> channel = supportChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicSupportChannelService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = supportChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    public void createComponents() {
        publicSupportChannelSelection = new PublicSupportChannelSelection(applicationService);
    }

    @Override
    public SupportModel getChatModel(ChannelDomain channelDomain) {
        return new SupportModel(channelDomain);
    }

    @Override
    public SupportView getChatView() {
        return new SupportView(model,
                this,
                publicSupportChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateTwoPartyChannel) {
            applyPeersIcon((PrivateChannel<?>) channel);
            publicSupportChannelSelection.deSelectChannel();
        } else {
            applyDefaultPublicChannelIcon((BasePublicChannel<?>) channel);
            privateChannelSelection.deSelectChannel();
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }
}
