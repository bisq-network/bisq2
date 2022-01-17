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

package bisq.desktop.primary.main.content.social.user;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.ChatPeer;
import bisq.social.chat.ChatService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatUserController implements Controller, ChatService.Listener {

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ChatService chatService;

    @Getter
    private final ChatUserModel model;
    @Getter
    private final ChatUserView view;
    private final DefaultServiceProvider serviceProvider;

    public ChatUserController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        chatService = serviceProvider.getChatService();
        model = new ChatUserModel(serviceProvider);
        view = new ChatUserView(model, this);
    }

    @Override
    public void onViewAttached() {
        chatService.addListener(this);
    }

    @Override
    public void onViewDetached() {
        chatService.removeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ChatService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onChannelAdded(Channel channel) {
    }

    @Override
    public void onChannelSelected(Channel channel) {
    }

    @Override
    public void onChatMessageAdded(Channel channel, ChatMessage newChatMessage) {
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onSelectChatUser(ChatPeer chatPeer) {
        //chatService.selectChatPeer(chatPeer);
    }

    void onCreateNewChatUser(String userName) {
        //todo
       // chatService.createNewChatUser(userName);
    }
}
