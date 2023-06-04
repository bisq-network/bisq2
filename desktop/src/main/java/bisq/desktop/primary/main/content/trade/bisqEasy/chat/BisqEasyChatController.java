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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat;

import bisq.application.DefaultApplicationService;
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
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPrivateChannelSelectionMenu;
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPublicChannelSelectionMenu;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.TradeAssistantController;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final SettingsService settingsService;
    private TradeAssistantController tradeAssistantController;
    private BisqEasyPublicChannelSelectionMenu bisqEasyPublicChannelSelectionMenu;
    private BisqEasyPrivateChannelSelectionMenu bisqEasyPrivateChannelSelectionMenu;

    private Pin offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_CHAT);

        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        settingsService = applicationService.getSettingsService();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        tradeAssistantController = new TradeAssistantController(applicationService, this::openUserProfileSidebar);
        bisqEasyPublicChannelSelectionMenu = new BisqEasyPublicChannelSelectionMenu(applicationService);
        bisqEasyPrivateChannelSelectionMenu = new BisqEasyPrivateChannelSelectionMenu(applicationService);
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
                tradeAssistantController.getView());
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

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getChannelIconNode().set(null);
                return;
            }
            model.getOfferOnlyVisible().set(isBisqEasyPublicChatChannel);
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

                BisqEasyPrivateTradeChatChannel privateChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                applyPeersIcon(privateChannel);

                model.getChannelTitle().set(chatService.findChatChannelService(chatChannel)
                        .map(service -> Res.get("chat.bisqEasy.trade.channelTitle", service.getChannelTitle(Objects.requireNonNull(chatChannel))))
                        .orElse(""));

                if (!settingsService.getTradeRulesConfirmed().get()) {
                    Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
                }

                tradeAssistantController.setBisqEasyPrivateTradeChatChannel(privateChannel);
            } else if (isTwoPartyPrivateChatChannel) {
                bisqEasyPublicChannelSelectionMenu.deSelectChannel();
                bisqEasyPrivateChannelSelectionMenu.deSelectChannel();

                TwoPartyPrivateChatChannel privateChannel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(privateChannel);
            }
        });
    }
}
