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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_state;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.wallets.core.WalletService;
import javafx.scene.input.KeyCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final Optional<WalletService> walletService;
    private final AccountService accountService;
    private final ChatService chatService;
    private final MediationService mediationService;
    private final SettingsService settingsService;

    public TradeStateController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        chatService = applicationService.getChatService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        walletService = applicationService.getWalletService();
        accountService = applicationService.getAccountService();
        settingsService = applicationService.getSettingsService();
        mediationService = applicationService.getSupportService().getMediationService();

        model = new TradeStateModel();
        view = new TradeStateView(model, this);
    }

    public void selectChannel(BisqEasyPrivateTradeChatChannel channel) {
        model.setSelectedChannel(channel);
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        model.setBisqEasyOffer(bisqEasyOffer);

        boolean isBuyer = isBuyer();
        boolean isSeller = !isBuyer;

        model.getPhase1Info().set(isBuyer ? Res.get("bisqEasy.trade.state.phase.buyer.phase1").toUpperCase() :
                Res.get("bisqEasy.trade.state.phase.seller.phase1").toUpperCase());
        model.getPhase2Info().set(isBuyer ? Res.get("bisqEasy.trade.state.phase.buyer.phase2").toUpperCase() :
                Res.get("bisqEasy.trade.state.phase.seller.phase2").toUpperCase());
        model.getPhase3Info().set(isBuyer ? Res.get("bisqEasy.trade.state.phase.buyer.phase3").toUpperCase() :
                Res.get("bisqEasy.trade.state.phase.seller.phase3").toUpperCase());
        model.getPhase4Info().set(isBuyer ? Res.get("bisqEasy.trade.state.phase.buyer.phase4").toUpperCase() :
                Res.get("bisqEasy.trade.state.phase.seller.phase4").toUpperCase());
        model.getPhase5Info().set(Res.get("bisqEasy.trade.state.phase.phase5").toUpperCase());


        String directionString = isBuyer ?
                Res.get("offer.buying").toUpperCase() :
                Res.get("offer.selling").toUpperCase();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        model.getQuoteCode().set(bisqEasyOffer.getMarket().getQuoteCurrencyCode());
        model.getBaseAmount().set(baseAmountString);
        String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        model.getQuoteAmount().set(quoteAmountString);
        FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
        String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
        String tradeInfo = Res.get("bisqEasy.trade.state.headline",
                directionString,
                baseAmountString,
                quoteAmountString,
                paymentMethodName);


        model.getTradeInfo().set(tradeInfo);

        //todo
        model.getSellersPaymentAccountData().set("IBAN: 123213123123\nBIC: 2112312\n Holdername: Bruno Ganz");
        model.getBuyersBtcAddress().set("2MwMapa5GYoWbXLhAui55ifCq3B9k1dyNAx");


        String fiatCode = bisqEasyOffer.getMarket().getQuoteCurrencyCode();

        EasyBind.subscribe(model.getPhaseIndex(), index -> {
            int phaseIndex = index.intValue();


            if (phaseIndex == 0) {
                // seller: send fiat account
                model.getActionButtonVisible().set(isSeller);
                model.getOpenDisputeButtonVisible().set(false);
                if (isSeller) {
                    model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_1);
                    model.getActionButtonText().set(Res.get("bisqEasy.trade.state.actionButton.seller.phase1"));
                } else {
                    model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_1);
                }
                // todo seller: show text field for payment account with button below
                model.getPhaseInfo().set(isBuyer ?
                        Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase1", fiatCode) :
                        Res.get("bisqEasy.trade.state.phaseInfo.seller.phase1", fiatCode)
                );
            } else if (phaseIndex == 1) {
                // buyer: send btc address, fiat sent
                model.getActionButtonVisible().set(isBuyer);
                model.getOpenDisputeButtonVisible().set(false);

                if (isBuyer) {
                    model.getActionButtonText().set(Res.get("bisqEasy.trade.state.actionButton.buyer.phase2"));
                    model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_2);
                } else {
                    model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_2);
                }
                // todo buyer show text field with sellers account data and the fiat amount to send w button below
                model.getPhaseInfo().set(isBuyer ?
                        Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase2.btcAddress", fiatCode) :
                        Res.get("bisqEasy.trade.state.phaseInfo.seller.phase2", fiatCode)
                );
            } else if (phaseIndex == 2) {
                // seller: confirm fiat receipt and sent BTC
                model.getActionButtonVisible().set(isSeller);
                model.getOpenDisputeButtonVisible().set(true);
                if (isSeller) {
                    model.getActionButtonText().set(Res.get("bisqEasy.trade.state.actionButton.seller.phase3", fiatCode));
                    model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_3);
                } else {
                    model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_3);
                }
                // todo seller: 
                model.getPhaseInfo().set(isBuyer ?
                        Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase3", fiatCode) :
                        Res.get("bisqEasy.trade.state.phaseInfo.seller.phase3.confirmFiatReceipt", quoteAmountString)
                );
            } else if (phaseIndex == 3) {
                // buyer: confirm BTC received
                model.getActionButtonVisible().set(isBuyer);
                model.getOpenDisputeButtonVisible().set(true);
                if (isBuyer) {
                    model.getActionButtonText().set(Res.get("bisqEasy.trade.state.actionButton.buyer.phase4"));
                    model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_4);
                } else {
                    model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_4);
                }
                model.getPhaseInfo().set(isBuyer ?
                        Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase4", fiatCode) :
                        Res.get("bisqEasy.trade.state.phaseInfo.seller.phase4")
                );
            } else if (phaseIndex == 4) {
                // btc received
                model.getActionButtonVisible().set(false);
                model.getOpenDisputeButtonVisible().set(false);
                model.getPhaseInfo().set(isBuyer ?
                        Res.get("bisqEasy.trade.state.phaseInfo.buyer.phase5") :
                        Res.get("bisqEasy.trade.state.phaseInfo.seller.phase5")
                );
                model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_5);
                model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_5);
            }
        });
    }

    @Override
    public void onActivate() {
        view.getRoot().getScene().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.A) {
                model.getPhaseIndex().set(Math.min(4, model.getPhaseIndex().get() + 1));
            }
            if (keyEvent.getCode() == KeyCode.S) {
                model.getPhaseIndex().set(Math.max(0, model.getPhaseIndex().get() - 1));
            }
        });

        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
    }

    void onAction() {
        boolean isBuyer = isBuyer();
        boolean isSeller = !isBuyer;
        int index = model.getPhaseIndex().get();
        if (index == 0 && isSeller) {
            onSendPaymentAccount();
            model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 1 && isBuyer) {
            sendMessage("Please send BTC to: 2MwMapa5GYoWbXLhAui55ifCq3B9k1dyNAx");
            model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 2 && isSeller) {
            sendMessage("I have received the EUR payment and initiated the BTC transfer.");
            model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 3 && isBuyer) {
            sendMessage("I confirm that I have received the BTC payment.");
            model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        }
    }


    @Override
    public void onDeactivate() {
    }

    void onExpand() {
        setIsCollapsed(false);
    }

    void onCollapse() {
        setIsCollapsed(true);
    }

    void onHeaderClicked() {
        setIsCollapsed(!model.getIsCollapsed().get());
    }

    private void setIsCollapsed(boolean value) {
        model.getIsCollapsed().set(value);
        settingsService.setCookie(CookieKey.TRADE_ASSISTANT_COLLAPSED, value);
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }

    void onOpenDispute() {
        BisqEasyPrivateTradeChatChannel channel = model.getSelectedChannel();
        Optional<UserProfile> mediator = channel.getMediator();
        if (mediator.isPresent()) {
            new Popup().headLine(Res.get("bisqEasy.mediation.request.confirm.headline"))
                    .information(Res.get("bisqEasy.mediation.request.confirm.msg"))
                    .actionButtonText(Res.get("bisqEasy.mediation.request.confirm.openMediation"))
                    .onAction(() -> {
                        channel.setIsInMediation(true);
                        mediationService.requestMediation(channel);
                        new Popup().headLine(Res.get("bisqEasy.mediation.request.feedback.headline"))
                                .feedback(Res.get("bisqEasy.mediation.request.feedback.msg")).show();
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        } else {
            new Popup().warning(Res.get("bisqEasy.mediation.request.feedback.noMediatorAvailable")).show();
        }
    }

    void sendMessage(String message) {
        chatService.getBisqEasyPrivateTradeChatChannelService()
                .sendTextMessage(message, model.getSelectedChannel());
    }

    void onSendBtcAddress() {
        sendMessage(Res.get("bisqEasy.sendBtcAddress.message", "2MwMapa5GYoWbXLhAui55ifCq3B9k1dyNAx"));
        
       /* checkArgument(walletService.isPresent());
        walletService.get().getUnusedAddress().
                thenAccept(receiveAddress -> UIThread.run(() -> {
                            if (receiveAddress == null) {
                                log.warn("receiveAddress from the wallet is null.");
                                return;
                            }
                            String message = Res.get("bisqEasy.sendBtcAddress.message", receiveAddress);
                            chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                                    Optional.empty(),
                                    model.getSelectedChannel());
                        }
                ));*/
    }

    void onSendPaymentAccount() {
        if (accountService.getAccounts().size() > 1) {
            //todo
            new Popup().information("TODO").show();
        } else {
            Account<?, ? extends PaymentMethod<?>> selectedAccount = accountService.getSelectedAccount();
            if (accountService.hasAccounts() && selectedAccount instanceof UserDefinedFiatAccount) {
                UserDefinedFiatAccountPayload accountPayload = ((UserDefinedFiatAccount) selectedAccount).getAccountPayload();
                String message = Res.get("bisqEasy.sendPaymentAccount.message", accountPayload.getAccountData());
                chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                        Optional.empty(),
                        model.getSelectedChannel());
            } else {
                if (!accountService.hasAccounts()) {
                    new Popup().information(Res.get("bisqEasy.sendPaymentAccount.noAccount.popup")).show();
                } else if (accountService.getAccountByNameMap().size() > 1) {
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

    void onPaymentAccountSelected(@Nullable Account<?, ? extends PaymentMethod<?>> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
        }
    }

    private boolean isBuyer() {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds()) ?
                bisqEasyOffer.getMakersDirection().isBuy() :
                bisqEasyOffer.getTakersDirection().isBuy();
    }

}
