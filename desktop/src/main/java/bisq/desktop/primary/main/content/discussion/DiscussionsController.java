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
import bisq.chat.channel.ChannelSelectionService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.CommonPublicChatChannelService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicChannelSelection;
import bisq.desktop.primary.main.content.chat.channels.PublicDiscussionChannelSelection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscussionsController extends ChatController<DiscussionsView, DiscussionsModel> implements Controller {
    public DiscussionsController(DefaultApplicationService applicationService) {
        super(applicationService, ChatChannelDomain.DISCUSSION, NavigationTarget.NONE);
    }

    @Override
    public ChannelSelectionService getChannelSelectionService() {
        return chatService.getDiscussionChannelSelectionService();
    }

    @Override
    public CommonPublicChatChannelService getPublicChannelService() {
        return chatService.getPublicDiscussionChannelService();
    }

    @Override
    public PublicChannelSelection getPublicChannelSelection() {
        return new PublicDiscussionChannelSelection(applicationService);
    }

    @Override
    public DiscussionsModel getChatModel(ChatChannelDomain chatChannelDomain) {
        return new DiscussionsModel(chatChannelDomain);
    }

    @Override
    public DiscussionsView getChatView() {
        return new DiscussionsView(model,
                this,
                publicChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }
}
