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

package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.chat.ChatController;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class CommonChatController extends ChatController<CommonChatView, CommonChatModel> implements Controller {
    private final ChatSearchService chatSearchService;
    private ChatChannelSelectionService chatChannelSelectionService;
    private Subscription searchTextPin;

    public CommonChatController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(serviceProvider, chatChannelDomain, navigationTarget);

        chatSearchService = serviceProvider.getChatSearchService();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);
    }

    @Override
    public CommonChatModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new CommonChatModel(chatChannelDomain);
    }

    @Override
    public CommonChatView createAndGetView() {
        return new CommonChatView(model, this, chatMessagesComponent.getRoot(), channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        selectedChannelChanged(chatChannelSelectionService.getSelectedChannel().get());
        searchTextPin = EasyBind.subscribe(chatSearchService.getSearchText(), searchText ->
                chatMessagesComponent.setSearchPredicate(item ->
                    searchText == null || searchText.isEmpty() || item.match(searchText)));
        chatSearchService.setOnHelpRequested(this::onOpenHelp);
        chatSearchService.setOnInfoRequested(this::onToggleChannelInfo);
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }
}
