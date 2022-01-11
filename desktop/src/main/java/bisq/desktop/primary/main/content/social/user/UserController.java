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
import bisq.desktop.primary.main.content.social.hangout.ChatUser;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.NodeIdAndPubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class UserController implements Controller {
    public interface Listener {
        void onSelectChatUser(ChatUser chatUser);
    }

    private final NetworkService networkService;
    private final IdentityService identityService;
    @Getter
    private final UserModel model;
    @Getter
    private final UserView view;
    private final Set<Listener> listeners = new HashSet<>();
    private final DefaultServiceProvider serviceProvider;

    public UserController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        networkService = serviceProvider.getNetworkService();
        identityService = serviceProvider.getIdentityService();
        model = new UserModel(serviceProvider);
        view = new UserView(model, this);

        NodeIdAndPubKey defaultIdentity = identityService.getNodeIdAndPubKey(IdentityService.DEFAULT);
        networkService.getInitializedNetworkIdAsync(defaultIdentity)
                .whenComplete((networkId, throwable) -> {
                    if (throwable != null) {
                        log.error(throwable.toString());
                        return;
                    }
                    ChatUser chatUser = new ChatUser("default", "Default user", networkId);
                    model.addChatUser(chatUser);
                    model.selectChatUser(chatUser);
                    listeners.forEach(l -> l.onSelectChatUser(chatUser));
                });
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    public void onViewDetached() {
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void onSelectChatUser(ChatUser chatUser) {
        model.selectChatUser(chatUser);
        listeners.forEach(l -> l.onSelectChatUser(chatUser));
    }
}
