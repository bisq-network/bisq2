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

package bisq.desktop.primary.main.content.common_chat;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.PublicChatController;
import bisq.desktop.primary.main.content.chat.channels.CommonPublicChannelSelectionMenu;
import bisq.desktop.primary.main.content.chat.channels.PublicChannelSelectionMenu;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonChatController extends PublicChatController<CommonChatView, CommonChatModel> implements Controller {
    public CommonChatController(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        super(applicationService, chatChannelDomain, NavigationTarget.NONE);
    }

    @Override
    public ChatChannelSelectionService getChannelSelectionService(ChatChannelDomain chatChannelDomain) {
        return chatService.getChatChannelSelectionServices().get(chatChannelDomain);
    }

    @Override
    public CommonPublicChatChannelService getPublicChannelService(ChatChannelDomain chatChannelDomain) {
        return chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
    }

    @Override
    public PublicChannelSelectionMenu<?, ?, ?> getPublicChannelSelection(ChatChannelDomain chatChannelDomain) {
        return new CommonPublicChannelSelectionMenu(applicationService, chatChannelDomain);
    }

    @Override
    public CommonChatModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new CommonChatModel(chatChannelDomain);
    }

    @Override
    public CommonChatView createAndGetView() {
        return new CommonChatView(model,
                this,
                publicChatChannelSelection.getRoot(),
                twoPartyPrivateChannelSelectionMenu.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }
}
