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
import bisq.chat.trade.TradeChatOffer;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChatMessage;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.chat.BaseChatController;
import bisq.desktop.primary.main.content.chat.channels.PublicTradeChannelSelection;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideController;
import bisq.desktop.primary.overlay.createOffer.CreateOfferController;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.wallets.core.WalletService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyChatController extends BaseChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private final SettingsService settingsService;
    private final MediationService mediationService;
    private final Optional<WalletService> walletService;
    private PublicTradeChannelSelection publicTradeChannelSelection;
    PrivateTradeChatMessage lastOfferMessage;

    private Pin offerOnlySettingsPin;
    private Pin inMediationPin;
    private Pin chatMessagesPin;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChannelDomain.TRADE, NavigationTarget.BISQ_EASY_CHAT);

        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
        settingsService = applicationService.getSettingsService();
        mediationService = applicationService.getSupportService().getMediationService();
        walletService = applicationService.getWalletService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

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
    public void createDependencies() {
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

        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }
        UIThread.run(() -> {
            if (channel == null) {
                model.getChannelIcon().set(null);
                return;
            }
            if (channel instanceof PrivateTradeChannel) {
                PrivateTradeChannel privateChannel = (PrivateTradeChannel) channel;
                applyPeersIcon(privateChannel);

                if (inMediationPin != null) {
                    inMediationPin.unbind();
                }
                inMediationPin = privateChannel.getInMediation().addObserver(mediationActivated ->
                        model.getOpenDisputeDisabled().set(mediationActivated ||
                                privateChannel.isMediator()));


                publicTradeChannelSelection.deSelectChannel();
                model.getCreateOfferButtonVisible().set(false);
                model.getTradeHelpersVisible().set(true);

                chatMessagesPin = tradeChannelSelectionService.getSelectedChannel().get().getChatMessages().addListener(this::updateLastOfferMessage);
                updateLastOfferMessage();

                Navigation.navigateTo(NavigationTarget.TRADE_GUIDE);
            } else {
                resetSelectedChildTarget();
                model.getTradeHelpersVisible().set(false);
                model.getCreateOfferButtonVisible().set(true);
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

    private void updateLastOfferMessage() {
        PrivateTradeChatMessage newLastOfferMessage = tradeChannelSelectionService.getSelectedChannel().get().getChatMessages().stream()
                .filter(chatMessage -> chatMessage instanceof PrivateTradeChatMessage)
                .map(chatMessage -> (PrivateTradeChatMessage) chatMessage)
                .filter(message -> message.getChannelName().equals(tradeChannelSelectionService.getSelectedChannel().get().getChannelName()))
                .filter(PrivateTradeChatMessage::hasTradeChatOffer)
                .max(Comparator.comparing(PrivateTradeChatMessage::getDate))
                .orElse(null);

        if (newLastOfferMessage == lastOfferMessage) return;

        lastOfferMessage = newLastOfferMessage;
        updateTradeHelper();
    }

    private void updateTradeHelper() {
        UIThread.runOnNextRenderFrame(() -> {
            // Don't show complete trade button for BTC sellers or chat with no offers
            if (lastOfferMessage == null) {
                model.getCompleteTradeDisabled().set(true);
                model.getCompleteTradeTooltip().set(Res.get("na"));
                return;
            }

            model.getCompleteTradeDisabled().set(isBtcSeller() || walletService.isEmpty());
            String tooltip = isBtcSeller() ? "Coming soon" :
                    hasWallet() ?
                            Res.get("bisqEasy.completeOffer.tooltip") :
                            Res.get("wallet.unavailable");
            model.getCompleteTradeTooltip().set(tooltip);
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

    void onCreateOfferButtonClicked() {
        Channel<? extends ChatMessage> channel = model.getSelectedChannel().get();
        if (channel instanceof PrivateTradeChannel) return;

        Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
    }

    void onOpenMediation() {
        Channel<? extends ChatMessage> channel = model.getSelectedChannel().get();
        if (!(channel instanceof PrivateTradeChannel)) return;

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
    }

    void onCompleteTrade() {
        PrivateTradeChatMessage chatMessage = lastOfferMessage;
        if (chatMessage == null) return;

        TradeChatOffer offer = chatMessage.getTradeChatOffer().orElse(null);
        if (offer == null) return;

        if (isMyMessage(chatMessage) == offer.getDirection().isSell()) {
            if (walletService.isEmpty()) return;

            // I'm buying BTC, send unused address
            walletService.get().getUnusedAddress().
                    thenAccept(receiveAddress -> UIThread.run(() -> {
                        if (receiveAddress == null) return;

                        chatService.getPrivateTradeChannelService().findChannelForMessage(chatMessage).ifPresent(channel -> {
                                    String message = Res.get("bisqEasy.completeOffer.sendRequest", receiveAddress);
                                    chatService.getPrivateTradeChannelService().sendPrivateChatMessage(message,
                                            Optional.empty(),
                                            channel);
                                }
                        );
                    }));
        } else {
            // I'm selling BTC, send fiat info
            log.info("Send fiat info");
            // TODO: Implement fiat info profile
        }
    }

    boolean isMyMessage(ChatMessage chatMessage) {
        return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
    }

    boolean isBtcSeller() {
        checkArgument(lastOfferMessage.getTradeChatOffer().isPresent());
        var offer = lastOfferMessage.getTradeChatOffer().get();

        return isMyMessage(lastOfferMessage) == offer.getDirection().isBuy();
    }

    boolean hasWallet() {
        return walletService.isPresent();
    }
}
