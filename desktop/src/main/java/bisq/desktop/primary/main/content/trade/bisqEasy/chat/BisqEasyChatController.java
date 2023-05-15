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

import bisq.account.bisqeasy.BisqEasyPaymentAccount;
import bisq.account.bisqeasy.BisqEasyPaymentAccountService;
import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
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
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPrivateChannelSelectionMenu;
import bisq.desktop.primary.main.content.chat.channels.BisqEasyPublicChannelSelectionMenu;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideController;
import bisq.desktop.primary.overlay.bisqeasy.createoffer.CreateOfferController;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.profile.UserProfile;
import bisq.wallets.core.WalletService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> {
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final SettingsService settingsService;
    private final MediationService mediationService;
    private final Optional<WalletService> walletService;
    private final BisqEasyPaymentAccountService bisqEasyPaymentAccountService;
    private BisqEasyPublicChannelSelectionMenu bisqEasyPublicChannelSelectionMenu;
    private BisqEasyPrivateChannelSelectionMenu bisqEasyPrivateChannelSelectionMenu;

    private Pin offerOnlySettingsPin, inMediationPin, chatMessagesPin,
            bisqEasyPrivateTradeChatChannelsPin, selectedPaymentAccountPin, paymentAccountsPin;
    private Subscription selectedPaymentAccountSubscription;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_CHAT);

        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        settingsService = applicationService.getSettingsService();
        bisqEasyPaymentAccountService = applicationService.getAccountService().getBisqEasyPaymentAccountService();

        mediationService = applicationService.getSupportService().getMediationService();
        walletService = applicationService.getWalletService();
    }

    @Override
    public void createServices(ChatChannelDomain chatChannelDomain) {
        bisqEasyPublicChannelSelectionMenu = new BisqEasyPublicChannelSelectionMenu(applicationService);
        bisqEasyPrivateChannelSelectionMenu = new BisqEasyPrivateChannelSelectionMenu(applicationService);
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
                chatMessagesComponent.getBottomHBox(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        model.getPaymentAccounts().setAll(bisqEasyPaymentAccountService.getAccounts());

        selectedChannelPin = bisqEasyChatChannelSelectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyPrivateTradeChatChannel> bisqEasyPrivateTradeChatChannels = chatService.getBisqEasyPrivateTradeChatChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyPrivateTradeChatChannels.addListener(() -> {
            model.getIsTradeChannelVisible().set(!bisqEasyPrivateTradeChatChannels.isEmpty());
        });

        paymentAccountsPin = bisqEasyPaymentAccountService.getAccounts().addListener(this::updatePaymentAccountSelectionVisibleState);

        selectedPaymentAccountPin = FxBindings.bind(model.selectedAccountProperty())
                .to(bisqEasyPaymentAccountService.selectedAccountAsObservable());

        selectedPaymentAccountSubscription = EasyBind.subscribe(model.selectedAccountProperty(),
                selectedAccount -> {
                    if (selectedAccount != null) {
                        bisqEasyPaymentAccountService.setSelectedAccount(selectedAccount);
                    }
                });
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();
        selectedPaymentAccountPin.unbind();
        paymentAccountsPin.unbind();
        selectedPaymentAccountSubscription.unsubscribe();

        if (inMediationPin != null) {
            inMediationPin.unbind();
        }
        resetSelectedChildTarget();
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }

        boolean isBisqEasyPublicChatChannel = chatChannel instanceof BisqEasyPublicChatChannel;
        boolean isBisqEasyPrivateTradeChatChannel = chatChannel instanceof BisqEasyPrivateTradeChatChannel;
        boolean isTwoPartyPrivateChatChannel = chatChannel instanceof TwoPartyPrivateChatChannel;

        if (isBisqEasyPrivateTradeChatChannel) {
            chatMessagesPin = chatChannel.getChatMessages().addListener(() -> privateTradeMessagesChangedHandler((BisqEasyPrivateTradeChatChannel) chatChannel));
        } else {
            model.getSendBtcAddressButtonVisible().set(false);
            model.getSendPaymentAccountButtonVisible().set(false);
        }

        updatePaymentAccountSelectionVisibleState();

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getChannelIconNode().set(null);
                return;
            }
            model.getOfferOnlyVisible().set(isBisqEasyPublicChatChannel);
            model.getCreateOfferButtonVisible().set(isBisqEasyPublicChatChannel);
            model.getOpenDisputeButtonVisible().set(isBisqEasyPrivateTradeChatChannel);
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

                if (inMediationPin != null) {
                    inMediationPin.unbind();
                }
                inMediationPin = privateChannel.isInMediationObservable().addObserver(isInMediation ->
                        model.getOpenDisputeDisabled().set(isInMediation ||
                                privateChannel.isMediator()));

                Navigation.navigateTo(NavigationTarget.TRADE_GUIDE);
            } else if (isTwoPartyPrivateChatChannel) {
                bisqEasyPublicChannelSelectionMenu.deSelectChannel();
                bisqEasyPrivateChannelSelectionMenu.deSelectChannel();

                TwoPartyPrivateChatChannel privateChannel = (TwoPartyPrivateChatChannel) chatChannel;
                applyPeersIcon(privateChannel);
            }
        });
    }

    void onCreateOffer() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPublicChatChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
    }

    void onRequestMediation() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPrivateTradeChatChannel,
                "channel must be instanceof BisqEasyPrivateTradeChatChannel at onOpenMediation");
        BisqEasyPrivateTradeChatChannel privateTradeChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
        Optional<UserProfile> mediator = privateTradeChannel.getMediator();
        if (mediator.isPresent()) {
            new Popup().headLine(Res.get("bisqEasy.requestMediation.confirm.popup.headline"))
                    .information(Res.get("bisqEasy.requestMediation.confirm.popup.msg"))
                    .actionButtonText(Res.get("bisqEasy.requestMediation.confirm.popup.openMediation"))
                    .onAction(() -> {
                        privateTradeChannel.setIsInMediation(true);
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

    void onSendBtcAddress() {
        checkArgument(walletService.isPresent());
        ChatChannel<? extends ChatMessage> channel = bisqEasyChatChannelSelectionService.getSelectedChannel().get();
        checkArgument(channel instanceof BisqEasyPrivateTradeChatChannel);
        BisqEasyPrivateTradeChatChannel tradeChatChannel = (BisqEasyPrivateTradeChatChannel) channel;
        walletService.get().getUnusedAddress().
                thenAccept(receiveAddress -> UIThread.run(() -> {
                            if (receiveAddress == null) {
                                log.warn("receiveAddress from the wallet is null.");
                                return;
                            }
                            String message = Res.get("bisqEasy.sendBtcAddress.message", receiveAddress);
                            chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                                    Optional.empty(),
                                    tradeChatChannel);
                        }
                ));
    }

    void onPaymentAccountSelected(@Nullable BisqEasyPaymentAccount account) {
        if (account != null) {
            bisqEasyPaymentAccountService.setSelectedAccount(account);
        }
    }

    void onSendPaymentAccount() {
        if (bisqEasyPaymentAccountService.getAccounts().size() > 1) {
            //todo
            new Popup().information("TODO").show();
        } else {
            BisqEasyPaymentAccount selectedAccount = bisqEasyPaymentAccountService.getSelectedAccount();
            if (bisqEasyPaymentAccountService.hasAccounts() && selectedAccount != null) {
                ChatChannel<? extends ChatMessage> channel = bisqEasyChatChannelSelectionService.getSelectedChannel().get();
                checkArgument(channel instanceof BisqEasyPrivateTradeChatChannel);
                BisqEasyPrivateTradeChatChannel chatChannel = (BisqEasyPrivateTradeChatChannel) channel;
                String message = Res.get("bisqEasy.sendPaymentAccount.message", selectedAccount.getData());
                chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                        Optional.empty(),
                        chatChannel);
            } else {
                if (!bisqEasyPaymentAccountService.hasAccounts()) {
                    new Popup().information(Res.get("bisqEasy.sendPaymentAccount.noAccount.popup")).show();
                } else if (bisqEasyPaymentAccountService.getAccountByNameMap().size() > 1) {
                    String key = "bisqEasy.sendPaymentAccount.multipleAccounts";
                    if (DontShowAgainService.showAgain(key)) {
                        new Popup().information(Res.get("bisqEasy.sendPaymentAccount.multipleAccounts.popup"))
                                .dontShowAgainId(key)
                                .show();
                    }
                }
            }
        }
    }

    private void privateTradeMessagesChangedHandler(BisqEasyPrivateTradeChatChannel chatChannel) {
        boolean isMyOffer = chatChannel.getChatMessages().stream()
                .filter(BisqEasyPrivateTradeChatMessage::hasTradeChatOffer)
                .filter(message -> message.getCitation().isPresent())
                .anyMatch(message -> isOfferAuthor(message.getCitation().get().getAuthorUserProfileId()));
        boolean isSeller = isMyOffer ?
                chatChannel.getBisqEasyOffer().getDirection().isSell() :
                chatChannel.getBisqEasyOffer().getDirection().isBuy();
        UIThread.runOnNextRenderFrame(() -> {
            if (isSeller) {
                model.getSendPaymentAccountButtonVisible().set(true);
                model.getSendBtcAddressButtonVisible().set(false);
            } else {
                model.getSendPaymentAccountButtonVisible().set(false);
                model.getSendBtcAddressButtonVisible().set(walletService.isPresent());
            }
        });
    }

    private void updatePaymentAccountSelectionVisibleState() {
        UIThread.run(() ->
                model.getPaymentAccountSelectionVisible().set(bisqEasyChatChannelSelectionService.getSelectedChannel().get() instanceof BisqEasyPrivateTradeChatChannel && bisqEasyPaymentAccountService.getAccounts().size() > 1));
    }

    private boolean isOfferAuthor(String offerAuthorUserProfileId) {
        return userIdentityService.isUserIdentityPresent(offerAuthorUserProfileId);
    }
}
