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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.bisqeasy.channel.open_trades.BisqEasyOpenTradeChatChannel;
import bisq.chat.bisqeasy.channel.open_trades.BisqEasyOpenTradeChatChannelService;
import bisq.chat.bisqeasy.channel.open_trades.BisqEasyOpenTradesSelectionService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.TradeStateController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyOpenTradesController extends ChatController<BisqEasyOpenTradesView, BisqEasyOpenTradesModel> {
    private final BisqEasyOpenTradesModel bisqEasyOpenTradesModel;
    private final BisqEasyOpenTradeChatChannelService channelService;
    private final BisqEasyOpenTradesSelectionService selectionService;

    private TradeStateController tradeStateController;
    private Pin channelItemPin, selectedChannelPin;

    public BisqEasyOpenTradesController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OPEN_TRADES, NavigationTarget.BISQ_EASY_OPEN_TRADES);

        channelService = chatService.getBisqEasyOpenTradeChatChannelService();
        selectionService = chatService.getBisqEasyOpenTradesChannelSelectionService();
        bisqEasyOpenTradesModel = getModel();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        tradeStateController = new TradeStateController(serviceProvider, this::openUserProfileSidebar);
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
    public BisqEasyOpenTradesModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyOpenTradesModel(chatChannelDomain);
    }

    @Override
    public BisqEasyOpenTradesView createAndGetView() {
        return new BisqEasyOpenTradesView(model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot(),
                tradeStateController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        channelItemPin = FxBindings.<BisqEasyOpenTradeChatChannel, BisqEasyOpenTradesView.ChannelItem>bind(model.getChannelItems())
                .map(BisqEasyOpenTradesView.ChannelItem::new)
                .to(channelService.getChannels());

        //todo handle case when no channels are available
        if (selectionService.getSelectedChannel().get() == null && !model.getChannelItems().isEmpty()) {
            selectionService.getSelectedChannel().set(model.getChannelItems().get(0).getChannel());
        }

        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
    }


    @Override
    public void onDeactivate() {
        channelItemPin.unbind();
        selectedChannelPin.unbind();
        resetSelectedChildTarget();
    }

    private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
        if (channel instanceof BisqEasyOpenTradeChatChannel) {
            chatChannelChanged(channel);
        }
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatChannel instanceof BisqEasyOpenTradeChatChannel) {
            UIThread.run(() -> {
                BisqEasyOpenTradeChatChannel channel = (BisqEasyOpenTradeChatChannel) chatChannel;
                applyPeersIcon(channel);

                BisqEasyOffer offer = channel.getBisqEasyOffer();
                boolean isMaker = isMaker(offer);
                String peer = ((BisqEasyOpenTradeChatChannel) chatChannel).getPeer().getUserName();
                String title = isMaker ?
                        Res.get("bisqEasy.topPane.privateTradeChannel.maker.title", peer, offer.getShortId()) :
                        Res.get("bisqEasy.topPane.privateTradeChannel.taker.title", peer, offer.getShortId());
                model.getChannelTitle().set(title);

                tradeStateController.setSelectedChannel(channel);
            });
        }
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }
}
