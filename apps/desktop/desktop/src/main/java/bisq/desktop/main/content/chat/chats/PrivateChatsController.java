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

package bisq.desktop.main.content.chat.chats;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.main.content.chat.navigation.ChatToolbox;
import bisq.desktop.main.content.common_chat.CommonChatController;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class PrivateChatsController extends CommonChatController<PrivateChatsView, PrivateChatsModel> {
    private final TwoPartyPrivateChatChannelService channelService;
    private final ReputationService reputationService;
    private Pin channelItemPin, channelsPin;
    private Subscription openPrivateChatsPin;

    public PrivateChatsController(ServiceProvider serviceProvider,
                                  ChatChannelDomain chatChannelDomain,
                                  NavigationTarget navigationTarget,
                                  Optional<ChatToolbox> toolbox) {
        super(serviceProvider, chatChannelDomain, navigationTarget, toolbox);

        channelService = chatService.getTwoPartyPrivateChatChannelServices().get(chatChannelDomain);
        reputationService = serviceProvider.getUserService().getReputationService();
    }

    @Override
    public PrivateChatsModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new PrivateChatsModel(chatChannelDomain);
    }

    @Override
    public PrivateChatsView createAndGetView() {
        return new PrivateChatsView(model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        channelItemPin = FxBindings.<TwoPartyPrivateChatChannel, PrivateChatsView.ListItem>bind(model.getListItems())
                .map(channel -> {
                    // We call maybeSelectFirst one render frame after we applied the item to the model list.
                    UIThread.runOnNextRenderFrame(this::maybeSelectFirst);
                    return new PrivateChatsView.ListItem(channel, reputationService);
                })
                .to(channelService.getChannels());

        channelsPin = channelService.getChannels().addObserver(this::channelsChanged);

        openPrivateChatsPin = EasyBind.subscribe(model.getNoOpenChats(),
                noOpenChats -> chatMessagesComponent.enableChatDialog(!noOpenChats));

        maybeSelectFirst();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        channelItemPin.unbind();
        channelsPin.unbind();
        model.getListItems().clear();
        resetSelectedChildTarget();
        openPrivateChatsPin.unsubscribe();
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
                UserProfile myProfile = channel.getMyUserIdentity().getUserProfile();
                model.getMyUserProfile().set(myProfile);
                model.getListItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedItem().set(item));
            }
        });
    }

    void onSelectItem(PrivateChatsView.ListItem item) {
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
