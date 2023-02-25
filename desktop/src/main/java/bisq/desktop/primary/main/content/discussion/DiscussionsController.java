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

package bisq.desktop.primary.main.content.discussion;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.*;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicDiscussionChannelSelection;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public class DiscussionsController extends ChatController<DiscussionsView, DiscussionsModel> implements Controller {
    private final ChannelSelectionService discussionChannelSelectionService;
    private final PublicChannelService publicDiscussionChannelService;
    private PublicDiscussionChannelSelection publicDiscussionChannelSelection;

    public DiscussionsController(DefaultApplicationService applicationService) {
        super(applicationService, ChannelDomain.DISCUSSION, NavigationTarget.NONE);

        publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
        discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        channelSidebar.setSelectedNotificationType(discussionChannelSelectionService.getSelectedChannel().get().getChannelNotificationType().get());
        notificationSettingSubscription = EasyBind.subscribe(channelSidebar.getSelectedNotificationType(),
                value -> {
                    Channel<? extends ChatMessage> channel = discussionChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicDiscussionChannelService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    public void createComponents() {
        publicDiscussionChannelSelection = new PublicDiscussionChannelSelection(applicationService);
    }

    @Override
    public DiscussionsModel getChatModel(ChannelDomain channelDomain) {
        return new DiscussionsModel(channelDomain);
    }

    @Override
    public DiscussionsView getChatView() {
        return new DiscussionsView(model,
                this,
                publicDiscussionChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateChannel) {
            applyPeersIcon((BasePrivateChannel<?>) channel);
            publicDiscussionChannelSelection.deSelectChannel();
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
