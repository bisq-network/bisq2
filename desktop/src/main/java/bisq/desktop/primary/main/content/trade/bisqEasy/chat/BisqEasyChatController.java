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
import bisq.chat.ChannelKind;
import bisq.chat.channel.Channel;
import bisq.chat.channel.PrivateChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicTradeChannelSelection;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideController;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final PublicTradeChannelService publicTradeChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final SettingsService settingsService;
    private PublicTradeChannelSelection publicTradeChannelSelection;
    private Pin offerOnlySettingsPin;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChannelKind.TRADE, NavigationTarget.BISQ_EASY_CHAT);

        publicTradeChannelService = chatService.getPublicTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
        settingsService = applicationService.getSettingsService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        notificationSettingSubscription = EasyBind.subscribe(channelSidebar.getSelectedNotificationType(),
                value -> {
                    Channel<? extends ChatMessage> channel = tradeChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicTradeChannelService.setNotificationSetting(channel, value);
                    }
                });
        selectedChannelPin = tradeChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        resetSelectedChildTarget();
    }

    @Override
    public void createComponents() {
        publicTradeChannelSelection = new PublicTradeChannelSelection(applicationService);
    }

    @Override
    public BisqEasyChatModel getChatModel(ChannelKind channelKind) {
        return new BisqEasyChatModel(channelKind);
    }

    @Override
    public BisqEasyChatView getChatView() {
        return new BisqEasyChatView(model,
                this,
                publicTradeChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateTradeChannel) {
            applyPeersIcon((PrivateChannel<?>) channel);

            model.getCreateOfferButtonVisible().set(false);
            publicTradeChannelSelection.deSelectChannel();

            Navigation.navigateTo(NavigationTarget.TRADE_GUIDE);
        } else {
            resetSelectedChildTarget();
            model.getCreateOfferButtonVisible().set(true);
            privateChannelSelection.deSelectChannel();

            Market market = ((PublicTradeChannel) channel).getMarket();
            StackPane marketsImage = MarketImageComposition.imageBoxForMarket(
                    market.getBaseCurrencyCode().toLowerCase(),
                    market.getQuoteCurrencyCode().toLowerCase());
            model.getChannelIcon().set(marketsImage);
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_GUIDE: {
                return Optional.of(new TradeGuideController(applicationService));
            }

            default: {
                return Optional.empty();
            }
        }
    }
}
