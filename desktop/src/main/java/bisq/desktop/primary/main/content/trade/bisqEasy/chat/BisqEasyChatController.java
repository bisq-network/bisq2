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
import bisq.chat.bisqeasy.message.BisqEasyOffer;
import bisq.chat.bisqeasy.message.BisqEasyPrivateTradeChatMessage;
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
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.chat.ChatController;
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPrivateTradeChatChannelSelection;
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPublicChatChannelSelection;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideController;
import bisq.desktop.primary.overlay.bisqeasy.createoffer.CreateOfferController;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.profile.UserProfile;
import bisq.wallets.core.WalletService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final SettingsService settingsService;
    private final MediationService mediationService;
    private final Optional<WalletService> walletService;
    private BisqEasyPublicChatChannelSelection bisqEasyPublicChatChannelSelection;
    private BisqEasyPrivateTradeChatChannelSelection bisqEasyPrivateTradeChatChannelSelection;
    BisqEasyPrivateTradeChatMessage lastOfferMessage;

    private Pin offerOnlySettingsPin, inMediationPin, chatMessagesPin, bisqEasyPrivateTradeChatChannelsPin;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_CHAT);

        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        settingsService = applicationService.getSettingsService();
        mediationService = applicationService.getSupportService().getMediationService();
        walletService = applicationService.getWalletService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = bisqEasyChatChannelSelectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyPrivateTradeChatChannel> bisqEasyPrivateTradeChatChannels = chatService.getBisqEasyPrivateTradeChatChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyPrivateTradeChatChannels.addListener(() -> {
            model.getIsBisqEasyPrivateTradeChannelSelectionVisible().set(!bisqEasyPrivateTradeChatChannels.isEmpty());
        });
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();
        if (inMediationPin != null) {
            inMediationPin.unbind();
        }
        resetSelectedChildTarget();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
        bisqEasyPublicChatChannelSelection = new BisqEasyPublicChatChannelSelection(applicationService);
        bisqEasyPrivateTradeChatChannelSelection = new BisqEasyPrivateTradeChatChannelSelection(applicationService);
    }

    @Override
    public BisqEasyChatModel getChatModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyChatModel(chatChannelDomain);
    }

    @Override
    public BisqEasyChatView getChatView() {
        return new BisqEasyChatView(model,
                this,
                bisqEasyPublicChatChannelSelection.getRoot(),
                bisqEasyPrivateTradeChatChannelSelection.getRoot(),
                twoPartyPrivateChatChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }
        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getChannelIcon().set(null);
                return;
            }
            if (chatChannel instanceof BisqEasyPrivateTradeChatChannel) {
                BisqEasyPrivateTradeChatChannel privateChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                applyPeersIcon(privateChannel);

                if (inMediationPin != null) {
                    inMediationPin.unbind();
                }
                inMediationPin = privateChannel.getIsInMediation().addObserver(mediationActivated ->
                        model.getOpenDisputeDisabled().set(mediationActivated ||
                                privateChannel.isMediator()));


                bisqEasyPublicChatChannelSelection.deSelectChannel();
                twoPartyPrivateChatChannelSelection.deSelectChannel();
                model.getCreateOfferButtonVisible().set(false);
                model.getTradeHelpersVisible().set(true);

                chatMessagesPin = bisqEasyChatChannelSelectionService.getSelectedChannel().get().getChatMessages().addListener(this::updateLastOfferMessage);
                updateLastOfferMessage();

                Navigation.navigateTo(NavigationTarget.TRADE_GUIDE);
            } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                TwoPartyPrivateChatChannel privateChannel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(privateChannel);
                bisqEasyPublicChatChannelSelection.deSelectChannel();
                bisqEasyPrivateTradeChatChannelSelection.deSelectChannel();
                model.getTradeHelpersVisible().set(false);
                model.getCreateOfferButtonVisible().set(false);
            } else {
                // BisqEasyPublicChatChannel case
                resetSelectedChildTarget();
                model.getTradeHelpersVisible().set(false);
                model.getCreateOfferButtonVisible().set(true);
                model.getActionButtonText().set(Res.get("createOffer"));
                twoPartyPrivateChatChannelSelection.deSelectChannel();

                Market market = ((BisqEasyPublicChatChannel) chatChannel).getMarket();
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
        BisqEasyPrivateTradeChatMessage newLastOfferMessage = bisqEasyChatChannelSelectionService.getSelectedChannel().get().getChatMessages().stream()
                .filter(chatMessage -> chatMessage instanceof BisqEasyPrivateTradeChatMessage)
                .map(chatMessage -> (BisqEasyPrivateTradeChatMessage) chatMessage)
                .filter(message -> message.getChannelId().equals(bisqEasyChatChannelSelectionService.getSelectedChannel().get().getId()))
                .filter(BisqEasyPrivateTradeChatMessage::hasTradeChatOffer)
                .max(Comparator.comparing(BisqEasyPrivateTradeChatMessage::getDate))
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
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPublicChatChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
    }

    void onOpenMediation() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPrivateTradeChatChannel,
                "channel must be instanceof BisqEasyPrivateTradeChatChannel at onOpenMediation");
        BisqEasyPrivateTradeChatChannel privateTradeChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
        Optional<UserProfile> mediator = privateTradeChannel.findMediator();
        if (mediator.isPresent()) {
            new Popup().headLine(Res.get("bisqEasy.requestMediation.confirm.popup.headline"))
                    .information(Res.get("bisqEasy.requestMediation.confirm.popup.msg"))
                    .actionButtonText(Res.get("bisqEasy.requestMediation.confirm.popup.openMediation"))
                    .onAction(() -> {
                        mediationService.requestMediation(privateTradeChannel);
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
        BisqEasyPrivateTradeChatMessage chatMessage = lastOfferMessage;
        if (chatMessage == null) return;

        BisqEasyOffer offer = chatMessage.getBisqEasyOffer().orElse(null);
        if (offer == null) return;

        if (isMyMessage(chatMessage) == offer.getDirection().isSell()) {
            if (walletService.isEmpty()) return;

            // I'm buying BTC, send unused address
            walletService.get().getUnusedAddress().
                    thenAccept(receiveAddress -> UIThread.run(() -> {
                        if (receiveAddress == null) return;

                        chatService.getBisqEasyPrivateTradeChatChannelService().findChannel(chatMessage).ifPresent(channel -> {
                                    String message = Res.get("bisqEasy.completeOffer.sendRequest", receiveAddress);
                                    chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
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
        return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorUserProfileId());
    }

    boolean isBtcSeller() {
        checkArgument(lastOfferMessage.getBisqEasyOffer().isPresent());
        var offer = lastOfferMessage.getBisqEasyOffer().get();

        return isMyMessage(lastOfferMessage) == offer.getDirection().isBuy();
    }

    boolean hasWallet() {
        return walletService.isPresent();
    }
}
