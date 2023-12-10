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
import bisq.chat.ChatChannelDomain;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.chat.channels.TwoPartyPrivateChannelSelectionMenu;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChatController extends CommonChatController<PrivateChatView, PrivateChatModel> implements Controller {
    private TwoPartyPrivateChannelSelectionMenu twoPartyPrivateChannelSelectionMenu;
    private Pin selectedChannelPin;

    public PrivateChatController(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain, NavigationTarget navigationTarget) {
        super(serviceProvider, chatChannelDomain, navigationTarget);
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        super.createDependencies(chatChannelDomain);

        twoPartyPrivateChannelSelectionMenu = new TwoPartyPrivateChannelSelectionMenu(serviceProvider, chatChannelDomain);
    }

    @Override
    public PrivateChatModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new PrivateChatModel(chatChannelDomain);
    }

    @Override
    public PrivateChatView createAndGetView() {
        return new PrivateChatView(model,
                this,
                twoPartyPrivateChannelSelectionMenu.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        ObservableArray<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels =
                chatService.getTwoPartyPrivateChatChannelServices().get(model.getChatChannelDomain()).getChannels();
        chatChannelSelectionService.selectChannel(twoPartyPrivateChatChannels.stream().findFirst().orElse(null));
        selectedChannelPin = chatChannelSelectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        selectedChannelChanged(null);
        selectedChannelPin.unbind();
    }
}
