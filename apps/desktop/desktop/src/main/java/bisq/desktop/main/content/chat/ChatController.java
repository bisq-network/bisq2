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

package bisq.desktop.main.content.chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public abstract class ChatController<V extends ChatView<V, M>, M extends ChatModel>
        extends BaseChatController<V, M> implements Controller {
    protected ChatChannelSelectionService selectionService;
    protected Pin selectedChannelPin;

    public ChatController(ServiceProvider serviceProvider,
                          ChatChannelDomain chatChannelDomain,
                          NavigationTarget navigationTarget) {
        super(serviceProvider, chatChannelDomain, navigationTarget);
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        selectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);
    }

    @Override
    public void onActivate() {
        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
        model.getSearchText().set("");
        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                chatMessageContainerController.setSearchPredicate(item -> true);
            } else {
                chatMessageContainerController.setSearchPredicate(item -> item.match(searchText));
            }
        });
    }

    @Override
    public void onDeactivate() {
        selectedChannelPin.unbind();
        searchTextPin.unsubscribe();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }
}
