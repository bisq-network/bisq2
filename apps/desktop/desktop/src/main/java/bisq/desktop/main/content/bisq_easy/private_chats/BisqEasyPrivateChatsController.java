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

package bisq.desktop.main.content.bisq_easy.private_chats;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.chat.priv.PrivateChatsController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BisqEasyPrivateChatsController extends PrivateChatsController {

    public BisqEasyPrivateChatsController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT, NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        selectionService = chatService.getBisqEasyPrivateChatChannelSelectionService();
    }

    @Override
    public BisqEasyPrivateChatsModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyPrivateChatsModel(chatChannelDomain);
    }

    @Override
    public BisqEasyPrivateChatsView createAndGetView() {
        return new BisqEasyPrivateChatsView((BisqEasyPrivateChatsModel) model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }
}
