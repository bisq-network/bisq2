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

package bisq.desktop.main.content.bisq_easy.chat;

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.bisq_easy.chat.trade_state.TradeStateController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.chat.channels.BisqEasyPrivateChannelSelectionMenu;
import bisq.desktop.main.content.chat.channels.BisqEasyPublicChannelSelectionMenu;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.overlay.bisq_easy.create_offer.CreateOfferController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final SettingsService settingsService;
    private BisqEasyPublicChannelSelectionMenu bisqEasyPublicChannelSelectionMenu;
    private BisqEasyPrivateChannelSelectionMenu bisqEasyPrivateChannelSelectionMenu;

    private Pin offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin;
    private TradeStateController tradeStateController;

    public BisqEasyChatController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_OFFERBOOK);

        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        settingsService = serviceProvider.getSettingsService();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        tradeStateController = new TradeStateController(serviceProvider, this::openUserProfileSidebar);
        bisqEasyPublicChannelSelectionMenu = new BisqEasyPublicChannelSelectionMenu(serviceProvider);
        bisqEasyPrivateChannelSelectionMenu = new BisqEasyPrivateChannelSelectionMenu(serviceProvider);
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
    public BisqEasyChatModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyChatModel(chatChannelDomain);
    }

    @Override
    public BisqEasyChatView createAndGetView() {
        return new BisqEasyChatView(model,
                this,
                bisqEasyPublicChannelSelectionMenu.getRoot(),
                bisqEasyPrivateChannelSelectionMenu.getRoot(),
                twoPartyPrivateChannelSelectionMenu.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot(),
                tradeStateController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = bisqEasyChatChannelSelectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyPrivateTradeChatChannel> bisqEasyPrivateTradeChatChannels = chatService.getBisqEasyPrivateTradeChatChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyPrivateTradeChatChannels.addListener(() ->
                model.getIsTradeChannelVisible().set(!bisqEasyPrivateTradeChatChannels.isEmpty()));
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();

        resetSelectedChildTarget();
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        boolean isBisqEasyPublicChatChannel = chatChannel instanceof BisqEasyPublicChatChannel;
        boolean isBisqEasyPrivateTradeChatChannel = chatChannel instanceof BisqEasyPrivateTradeChatChannel;
        boolean isTwoPartyPrivateChatChannel = chatChannel instanceof TwoPartyPrivateChatChannel;

        model.getIsBisqEasyPrivateTradeChatChannel().set(isBisqEasyPrivateTradeChatChannel);
        model.getCreateOfferButtonVisible().set(isBisqEasyPublicChatChannel);
        model.getTopSeparatorVisible().set(!isBisqEasyPrivateTradeChatChannel);
        model.getOfferOnlyVisible().set(isBisqEasyPublicChatChannel);

        //todo check if delay needed
        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getChannelIconNode().set(null);
                return;
            }

            if (isBisqEasyPublicChatChannel) {
                twoPartyPrivateChannelSelectionMenu.deSelectChannel();
                bisqEasyPrivateChannelSelectionMenu.deSelectChannel();

                resetSelectedChildTarget();

                Market market = ((BisqEasyPublicChatChannel) chatChannel).getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarket(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase()).getFirst();

                //todo get larger icons and dont use scaling
                marketsImage.setScaleX(1.25);
                marketsImage.setScaleY(1.25);
                model.getChannelIconNode().set(marketsImage);
            } else if (isBisqEasyPrivateTradeChatChannel) {
                bisqEasyPublicChannelSelectionMenu.deSelectChannel();
                twoPartyPrivateChannelSelectionMenu.deSelectChannel();

                BisqEasyPrivateTradeChatChannel channel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                applyPeersIcon(channel);

                BisqEasyOffer offer = channel.getBisqEasyOffer();
                boolean isMaker = isMaker(offer);
                String peer = ((BisqEasyPrivateTradeChatChannel) chatChannel).getPeer().getUserName();
                String title = isMaker ?
                        Res.get("bisqEasy.topPane.privateTradeChannel.maker.title", peer, offer.getShortId()) :
                        Res.get("bisqEasy.topPane.privateTradeChannel.taker.title", peer, offer.getShortId());
                model.getChannelTitle().set(title);

                tradeStateController.setSelectedChannel(channel);
            } else if (isTwoPartyPrivateChatChannel) {
                bisqEasyPublicChannelSelectionMenu.deSelectChannel();
                bisqEasyPrivateChannelSelectionMenu.deSelectChannel();

                TwoPartyPrivateChatChannel privateChannel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(privateChannel);
            }
        });
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    void onCreateOffer() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPublicChatChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
    }
}
