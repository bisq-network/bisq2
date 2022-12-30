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
import bisq.chat.channel.Channel;
import bisq.chat.channel.ChannelDomain;
import bisq.chat.message.ChatMessage;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicTradeChannelSelection;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideController;
import bisq.desktop.primary.overlay.createOffer.CreateOfferController;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.Optional;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final PublicTradeChannelService publicTradeChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final SettingsService settingsService;
    private final MediationService mediationService;
    private PublicTradeChannelSelection publicTradeChannelSelection;
    private Pin offerOnlySettingsPin;
    private Pin inMediationPin;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChannelDomain.TRADE, NavigationTarget.BISQ_EASY_CHAT);

        publicTradeChannelService = chatService.getPublicTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
        settingsService = applicationService.getSettingsService();
        mediationService = applicationService.getSupportService().getMediationService();
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
        if (inMediationPin != null) {
            inMediationPin.unbind();
        }
        resetSelectedChildTarget();
    }

    @Override
    public void createComponents() {
        publicTradeChannelSelection = new PublicTradeChannelSelection(applicationService);
    }

    @Override
    public BisqEasyChatModel getChatModel(ChannelDomain channelDomain) {
        return new BisqEasyChatModel(channelDomain);
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
        UIThread.run(() -> {
            if (channel == null) {
                return;
            }
            if (channel instanceof PrivateTradeChannel) {
                PrivateTradeChannel privateChannel = (PrivateTradeChannel) channel;
                applyPeersIcon(privateChannel);

                if (inMediationPin != null) {
                    inMediationPin.unbind();
                }
                inMediationPin = privateChannel.getInMediation().addObserver(mediationActivated ->
                        model.getActionButtonVisible().set(!mediationActivated &&
                                !privateChannel.isMediator()));


                publicTradeChannelSelection.deSelectChannel();
                model.getActionButtonText().set(Res.get("bisqEasy.openDispute"));

                Navigation.navigateTo(NavigationTarget.TRADE_GUIDE);
            } else {
                resetSelectedChildTarget();
                model.getActionButtonVisible().set(true);
                model.getActionButtonText().set(Res.get("createOffer"));
                privateChannelSelection.deSelectChannel();

                Market market = ((PublicTradeChannel) channel).getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarket(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase()).getFirst();

                //todo get larger icons and dont use scaling
                marketsImage.setScaleX(1.25);
                marketsImage.setScaleY(1.25);
                model.getChannelIcon().set(marketsImage);
            }
        });
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

    void onActionButtonClicked() {
        Channel<? extends ChatMessage> channel = model.getSelectedChannel().get();
        if (channel instanceof PrivateTradeChannel) {
            PrivateTradeChannel privateTradeChannel = (PrivateTradeChannel) channel;
            if (privateTradeChannel.getMediator().isPresent()) {
                new Popup().headLine(Res.get("bisqEasy.requestMediation.confirm.popup.headline"))
                        .information(Res.get("bisqEasy.requestMediation.confirm.popup.msg"))
                        .actionButtonText(Res.get("bisqEasy.requestMediation.confirm.popup.openMediation"))
                        .onAction(() -> {
                            mediationService.requestMediation(privateTradeChannel.getMyUserIdentity(), privateTradeChannel.getPeer(), privateTradeChannel.getMediator().get());
                            new Popup().headLine(Res.get("bisqEasy.requestMediation.popup.headline"))
                                    .feedback(Res.get("bisqEasy.requestMediation.popup.msg")).show();
                        })
                        .closeButtonText(Res.get("cancel"))
                        .show();
            } else {
                new Popup().warning(Res.get("bisqEasy.requestMediation.popup.noMediatorAvailable")).show();
            }
        } else {
            Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
        }
    }
}
