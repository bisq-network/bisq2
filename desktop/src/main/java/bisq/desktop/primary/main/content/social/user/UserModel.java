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
import bisq.desktop.primary.main.content.social.hangout.ChatUser;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NodeIdAndPubKey;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class UserModel implements Model {

    private final NetworkService networkService;
    private final IdentityService identityService;
    private final ObservableList<ChatUser> chatUsers = FXCollections.observableArrayList();
    private final ObjectProperty<ChatUser> selectedChatUser = new SimpleObjectProperty<>(null);

    public UserModel(DefaultServiceProvider serviceProvider) {
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();

        NodeIdAndPubKey defaultIdentity = identityService.getNodeIdAndPubKey(IdentityService.DEFAULT);
        networkService.getInitializedNetworkIdAsync(defaultIdentity)
                .whenComplete((networkId, throwable) -> {
                    if (throwable != null) {
                        log.error(throwable.toString());
                        return;
                    }
                    if (chatUsers.isEmpty()) {
                        chatUsers.add(new ChatUser("default", "Default user", networkId));
                    }
                });
    }

    public void addChatUser(ChatUser chatUser) {
        if (!chatUsers.contains(chatUser)) {
            chatUsers.add(chatUser);
        }
    }

    public void selectChatUser(ChatUser chatUser) {
        selectedChatUser.set(chatUser);
    }
}
