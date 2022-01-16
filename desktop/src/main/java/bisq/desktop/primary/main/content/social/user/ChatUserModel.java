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
import bisq.desktop.common.view.Model;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.social.chat.ChatService;
import bisq.social.chat.ChatPeer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
@Getter
public class ChatUserModel implements Model {
    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ObservableList<ChatPeer> chatPeers = FXCollections.observableArrayList();
    private final ObjectProperty<ChatPeer> selectedChatUser = new SimpleObjectProperty<>(null);
    private final ChatService chatService;

    public ChatUserModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        chatService = serviceProvider.getChatService();
    }

    void setAllChatUsers(Collection<ChatPeer> chatPeer) {
        chatPeers.setAll(chatPeer);
    }

    void addChatUser(ChatPeer chatPeer) {
        if (!chatPeers.contains(chatPeer)) {
            chatPeers.add(chatPeer);
        }
    }

    public void selectChatUser(ChatPeer chatPeer) {
        selectedChatUser.set(chatPeer);
    }
}
