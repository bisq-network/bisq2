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
import bisq.desktop.main.content.chat.ChatController;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyPrivateChatsController extends ChatController<BisqEasyPrivateChatsView, BisqEasyPrivateChatsModel> {
    private final TwoPartyPrivateChatChannelService channelService;
    private final BisqEasyPrivateChatChannelSelectionService selectionService;
    private final ReputationService reputationService;

    private Pin channelItemPin, selectedChannelPin, channelsPin;

    public BisqEasyPrivateChatsController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT, NavigationTarget.BISQ_EASY_PRIVATE_CHAT);

        channelService = chatService.getTwoPartyPrivateChatChannelServices().get(ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT);
        selectionService = chatService.getBisqEasyPrivateChatChannelSelectionService();
        reputationService = serviceProvider.getUserService().getReputationService();
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
                .map(channel -> {
                    // We call maybeSelectFirst one render frame after we applied the item to the model list.
                    UIThread.runOnNextRenderFrame(this::maybeSelectFirst);
                    return new BisqEasyPrivateChatsView.ListItem(channel, reputationService);
                })
                .to(channelService.getChannels());

        channelsPin = channelService.getChannels().addObserver(this::channelsChanged);

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);

        maybeSelectFirst();
    }

    @Override
    public void onDeactivate() {
        channelItemPin.unbind();
        channelsPin.unbind();
        selectedChannelPin.unbind();
        model.getListItems().clear();
        resetSelectedChildTarget();
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedItem().set(null);
                maybeSelectFirst();
            }

            if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                TwoPartyPrivateChatChannel channel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(channel);
                UserProfile peer = channel.getPeer();
                model.setPeersReputationScore(reputationService.getReputationScore(peer));
                model.getPeersUserProfile().set(peer);
                model.getListItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedItem().set(item));
            }
        });
    }

    void onSelectItem(BisqEasyPrivateChatsView.ListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
        }
    }

    void onLeaveChat() {
        //todo add popup
        if (model.getSelectedChannel() != null) {
            channelService.leaveChannel(model.getSelectedChannel().getId());
            selectionService.getSelectedChannel().set(null);
        }
    }

    private void channelsChanged() {
        UIThread.run(() -> {
            model.getNoOpenChats().set(model.getFilteredList().isEmpty());
            maybeSelectFirst();
        });
    }

    private void maybeSelectFirst() {
        if (selectionService.getSelectedChannel().get() == null &&
                !channelService.getChannels().isEmpty() &&
                !model.getSortedList().isEmpty()) {
            selectionService.getSelectedChannel().set(model.getSortedList().get(0).getChannel());
        }
    }
}
