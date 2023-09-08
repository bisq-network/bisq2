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

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.private_chats.BisqEasyPrivateChatChannelSelectionService;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatController;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyPrivateChatsController extends ChatController<BisqEasyPrivateChatsView, BisqEasyPrivateChatsModel> {
    private final BisqEasyPrivateChatsModel bisqEasyPrivateChatsModel;
    private final TwoPartyPrivateChatChannelService channelService;
    private final BisqEasyPrivateChatChannelSelectionService selectionService;

    private Pin channelItemPin, selectedChannelPin;

    public BisqEasyPrivateChatsController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT, NavigationTarget.BISQ_EASY_PRIVATE_CHAT);

        channelService = chatService.getTwoPartyPrivateChatChannelServices().get(ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT);
        selectionService = chatService.getBisqEasyPrivateChatChannelSelectionService();

        bisqEasyPrivateChatsModel = getModel();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public BisqEasyPrivateChatsModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyPrivateChatsModel(chatChannelDomain);
    }

    @Override
    public BisqEasyPrivateChatsView createAndGetView() {
        return new BisqEasyPrivateChatsView(model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        channelItemPin = FxBindings.<TwoPartyPrivateChatChannel, BisqEasyPrivateChatsView.ListItem>bind(model.getListItems())
                .map(BisqEasyPrivateChatsView.ListItem::new)
                .to(channelService.getChannels());

        //todo handle case when no channels are available
        if (selectionService.getSelectedChannel().get() == null && !model.getListItems().isEmpty()) {
            selectionService.getSelectedChannel().set(model.getListItems().get(0).getChannel());
        }

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
    }

    @Override
    public void onDeactivate() {
        channelItemPin.unbind();
        selectedChannelPin.unbind();
        resetSelectedChildTarget();
    }


    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            super.chatChannelChanged(chatChannel);

            UIThread.run(() -> {
                TwoPartyPrivateChatChannel privateChannel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(privateChannel);
            });
        }
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }
}
