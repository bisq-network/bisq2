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

import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatController;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class BisqEasyPrivateChatsController extends ChatController<BisqEasyPrivateChatsView, BisqEasyPrivateChatsModel> {
    private final BisqEasyPrivateChatsModel bisqEasyPrivateChatsModel;
    private final TwoPartyPrivateChatChannelService twoPartyPrivateChatChannelService;

    private Pin channelItemPin;
    private Subscription selectedChannelItemPin;

    public BisqEasyPrivateChatsController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_OFFERBOOK);

        twoPartyPrivateChatChannelService = chatService.getTwoPartyPrivateChatChannelServices().get(ChatChannelDomain.BISQ_EASY);
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
        channelItemPin = FxBindings.<TwoPartyPrivateChatChannel, BisqEasyPrivateChatsView.ChannelItem>bind(model.getChannelItems())
                .map(BisqEasyPrivateChatsView.ChannelItem::new)
                .to(twoPartyPrivateChatChannelService.getChannels());

        //todo handle case when no channels are available
        if (model.getSelectedChannelItem().get() == null && !model.getChannelItems().isEmpty()) {
            model.getSelectedChannelItem().set(model.getChannelItems().get(0));
        }

        selectedChannelItemPin = EasyBind.subscribe(model.getSelectedChannelItem(), this::selectedChannelItemChanged);
    }

    @Override
    public void onDeactivate() {
        channelItemPin.unbind();
        selectedChannelItemPin.unsubscribe();
        resetSelectedChildTarget();
    }

    private void selectedChannelItemChanged(BisqEasyPrivateChatsView.ChannelItem channelItem) {
        if (channelItem != null) {
            chatChannelChanged(channelItem.getChannel());
        }
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatChannel instanceof TwoPartyPrivateChatChannel) {
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
