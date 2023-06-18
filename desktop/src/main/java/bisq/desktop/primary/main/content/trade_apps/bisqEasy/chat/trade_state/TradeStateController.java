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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states.SellerState1;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.trade.Trade;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

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
    private final BisqEasyTradeService bisqEasyTradeService;
    private final SellerState1 sellerState1;
    private Pin tradeRulesConfirmedPin;
    private Subscription phaseIndexPin, isCollapsedPin;
    private Pin bisqEasyTradeStatePin;

    public TradeStateController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        chatService = applicationService.getChatService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        walletService = applicationService.getWalletService();
        accountService = applicationService.getAccountService();
        settingsService = applicationService.getSettingsService();
        mediationService = applicationService.getSupportService().getMediationService();
        bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();

        sellerState1 = new SellerState1(applicationService);

        model = new TradeStateModel();
        view = new TradeStateView(model, this, sellerState1.getView().getRoot());
    }

    public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        sellerState1.setSelectedChannel(channel);

        model.setAppliedPhaseIndex(-1);
        if (bisqEasyTradeStatePin != null) {
            bisqEasyTradeStatePin.unbind();
        }
        model.setSelectedChannel(channel);
        String tradeId = Trade.createId(channel.getBisqEasyOffer().getId(), channel.getPeer().getId());
        bisqEasyTradeService.findTrade(tradeId)
                .ifPresent(bisqEasyTradeModel -> {
                    model.setBisqEasyTradeModel(bisqEasyTradeModel);
                    bisqEasyTradeStatePin = bisqEasyTradeModel.tradeStateObservable().addObserver(state -> {
                        switch (state) {
                            case INIT:
                                break;
                            case TAKER_SEND_TAKE_OFFER_REQUEST:
                                break;
                            case MAKER_RECEIVED_TAKE_OFFER_REQUEST:
                                break;
                        }
                    });
                 /*   BisqEasyOffer bisqEasyOffer = bisqEasyTradeModel.getOffer();
                    boolean isMaker = isMaker(bisqEasyOffer);
                    boolean isBuyer = isBuyer(bisqEasyOffer);

                    if (isBuyer) {
                        if (isMaker) {

                        } else {

                        }
                    } else {
                        if (isMaker) {

                        } else {

                        }
                    }*/

                });

        model.setBisqEasyOffer(channel.getBisqEasyOffer());
        applyData();
        phaseIndexPin = EasyBind.subscribe(model.getPhaseIndex(), index -> phaseIndexChanged(index.intValue()));
    }

    @Override
    public void onActivate() {
        model.setAppliedPhaseIndex(-1);
       /* view.getRoot().getScene().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.A) {
                model.getPhaseIndex().set(Math.min(4, model.getPhaseIndex().get() + 1));
            }
            if (keyEvent.getCode() == KeyCode.S) {
                model.getPhaseIndex().set(Math.max(0, model.getPhaseIndex().get() - 1));
            }
        });*/

        tradeRulesConfirmedPin = settingsService.getTradeRulesConfirmed().addObserver(tradeRulesConfirmed -> {
            int phaseIndex = model.getPhaseIndex().get();
            if (phaseIndex == 0 && tradeRulesConfirmed) {
                phaseIndexChanged(phaseIndex);
            }
        });
        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), isCollapsed -> {
            applyFirstTimeItemsVisible();
            applyPhaseAndInfoBoxVisible();
        });
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
        applyPhaseAndInfoBoxVisible();
    }

    @Override
    public void onDeactivate() {
        if (phaseIndexPin != null) {
            phaseIndexPin.unsubscribe();
        }
        if (tradeRulesConfirmedPin != null) {
            tradeRulesConfirmedPin.unbind();
        }
        isCollapsedPin.unsubscribe();
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

    void onAction() {
        boolean isBuyer = isBuyer();
        boolean isSeller = !isBuyer;
        int index = model.getPhaseIndex().get();
        if (index == 0 && isSeller) {
            sellerSendsPaymentAccount();
            // model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
            //model.getBisqEasyTrade().sendPaymentAccount();
        } else if (index == 1 && isBuyer) {
            buyerConfirmFiatSent();
            //model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 2 && isSeller) {
            sellerConfirmBtcSent();
            // model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 3) {
            //  model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        } else if (index == 4) {
            tradeCompleted();
            //  model.getPhaseIndex().set(model.getPhaseIndex().get() + 1);
        }
    }

    void sendChatBotMessage(String message) {
        chatService.getBisqEasyPrivateTradeChatChannelService()
                .sendTextMessage(message, model.getSelectedChannel());
    }


    void sellerSendsPaymentAccount() {
        String message = Res.get("bisqEasy.tradeState.info.seller.phase1.chatBotMessage", model.getSellersPaymentAccountData().get());
        chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                Optional.empty(),
                model.getSelectedChannel());

        try {
            bisqEasyTradeService.sellerSendsPaymentAccount(model.getBisqEasyTradeModel(), model.getSellersPaymentAccountData().get());
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    void buyerConfirmFiatSent() {
        sendChatBotMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2.chatBotMessage", model.getQuoteCode().get(), model.getBuyersBtcAddress().get()));

        try {
            bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTradeModel(), model.getBuyersBtcAddress().get());
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    void sellerConfirmBtcSent() {
        sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3.chatBotMessage", model.getTxId().get()));
        try {
            bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTradeModel(), model.getTxId().get());
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    void btcConfirmed() {
        try {
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTradeModel());
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    void tradeCompleted() {
        try {
            bisqEasyTradeService.tradeCompleted(model.getBisqEasyTradeModel());
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    /* if (accountService.getAccounts().size() > 1) {
         //todo
         new Popup().information("TODO").show();
     } else {
        
         
         Account<?, ? extends PaymentMethod<?>> selectedAccount = accountService.getSelectedAccount();
         if (accountService.hasAccounts() && selectedAccount instanceof UserDefinedFiatAccount) {
         } else {
             if (!accountService.hasAccounts()) {
                 new Popup().information(Res.get("bisqEasy.tradeState.info.seller.phase1.popup.noAccount")).show();
             } else if (accountService.getAccountByNameMap().size() > 1) {
                 String key = "bisqEasy.sendPaymentAccount.multipleAccounts";
                 if (DontShowAgainService.showAgain(key)) {
                     new Popup().information(Res.get("bisqEasy.tradeState.info.seller.phase1.popup.multipleAccounts"))
                             .dontShowAgainId(key)
                             .show();
                 }
             }
         }
     }*/
    private Optional<String> findUsersAccountData() {
        return Optional.ofNullable(accountService.getSelectedAccount()).stream()
                .filter(account -> account instanceof UserDefinedFiatAccount)
                .map(account -> (UserDefinedFiatAccount) account)
                .map(account -> account.getAccountPayload().getAccountData())
                .findFirst();
    }

    void onPaymentAccountSelected(@Nullable Account<?, ? extends PaymentMethod<?>> account) {
        if (account != null) {
            accountService.setSelectedAccount(account);
        }
    }

    private void applyData() {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        boolean isBuyer = isBuyer();

        model.getPhase1Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase1").toUpperCase() :
                Res.get("bisqEasy.tradeState.phase.seller.phase1").toUpperCase());
        model.getPhase2Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase2").toUpperCase() :
                Res.get("bisqEasy.tradeState.phase.seller.phase2").toUpperCase());
        model.getPhase3Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase3").toUpperCase() :
                Res.get("bisqEasy.tradeState.phase.seller.phase3").toUpperCase());
        model.getPhase4Info().set(isBuyer ? Res.get("bisqEasy.tradeState.phase.buyer.phase4").toUpperCase() :
                Res.get("bisqEasy.tradeState.phase.seller.phase4").toUpperCase());
        model.getPhase5Info().set(Res.get("bisqEasy.tradeState.phase.phase5").toUpperCase());

        String directionString = isBuyer ?
                Res.get("offer.buying").toUpperCase() :
                Res.get("offer.selling").toUpperCase();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        model.getQuoteCode().set(bisqEasyOffer.getMarket().getQuoteCurrencyCode());
        model.getFormattedBaseAmount().set(baseAmountString);
        String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        model.getFormattedQuoteAmount().set(quoteAmountString);
        FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
        String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
        String tradeInfo = Res.get("bisqEasy.tradeState.header.headline",
                directionString,
                baseAmountString,
                quoteAmountString,
                paymentMethodName);

        model.getTradeInfo().set(tradeInfo);
        if (!isBuyer) {
            findUsersAccountData().ifPresent(accountData -> model.getSellersPaymentAccountData().set(accountData));
        }
    }

    private void phaseIndexChanged(int phaseIndex) {
        BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
        if (bisqEasyOffer == null) {
            return;
        }
        if (model.getAppliedPhaseIndex() == phaseIndex) {
            return;
        }
        model.setAppliedPhaseIndex(phaseIndex);
        boolean tradeRulesConfirmed = getTradeRulesConfirmed();
        boolean showFirstTimeItems = phaseIndex == 0 && !tradeRulesConfirmed;
        applyFirstTimeItemsVisible();
        applyPhaseAndInfoBoxVisible();
        if (showFirstTimeItems) {
            return;
        }

        boolean isBuyer = isBuyer();
        boolean isSeller = !isBuyer;
        String fiatCode = bisqEasyOffer.getMarket().getQuoteCurrencyCode();

        if (phaseIndex == 0) {
            // seller: send fiat account
            model.getActionButtonVisible().set(isSeller);
            model.getOpenDisputeButtonVisible().set(false);
            if (isSeller) {
                model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_1);
                model.getActionButtonText().set(Res.get("bisqEasy.tradeState.info.seller.phase1.buttonText"));
            } else {
                model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_1);
            }
            // todo seller: show text field for payment account with button below
            model.getPhaseInfo().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.info.buyer.phase1.headline", fiatCode) :
                    Res.get("bisqEasy.tradeState.info.seller.phase1.headline")
            );
        } else if (phaseIndex == 1) {
            // buyer: send btc address, fiat sent
            model.getActionButtonVisible().set(isBuyer);
            model.getOpenDisputeButtonVisible().set(false);

            if (isBuyer) {
                model.getActionButtonText().set(Res.get("bisqEasy.tradeState.info.buyer.phase2.buttonText"));
                model.getSellersPaymentAccountData().set("IBAN: 122312\n" +
                        "BIC: 32432131\n" +
                        "Name: Hal Finney"); //todo
                model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_2);
            } else {
                model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_2);
            }
            // todo buyer show text field with sellers account data and the fiat amount to send w button below
            model.getPhaseInfo().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.info.buyer.phase2.headline", model.getFormattedQuoteAmount().get()) :
                    Res.get("bisqEasy.tradeState.info.seller.phase2.headline", fiatCode)
            );
        } else if (phaseIndex == 2) {
            // seller: confirm fiat receipt and sent BTC
            model.getActionButtonVisible().set(isSeller);
            model.getOpenDisputeButtonVisible().set(true);
            if (isSeller) {
                model.getBuyersBtcAddress().set("2MwMapa5GYoWbXLhAui55ifCq3B9k1dyNAx");
                model.getActionButtonText().set(Res.get("bisqEasy.tradeState.info.seller.phase3.buttonText"));
                model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_3);
            } else {
                model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_3);
            }
            // todo seller: 
            model.getPhaseInfo().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.info.buyer.phase3.headline", fiatCode) :
                    Res.get("bisqEasy.tradeState.info.seller.phase3.headline", model.getFormattedQuoteAmount().get())
            );
        } else if (phaseIndex == 3) {
            // buyer: confirm BTC received
            model.getActionButtonVisible().set(true);
            model.getOpenDisputeButtonVisible().set(true);
            model.getActionButtonText().set(Res.get("bisqEasy.tradeState.info.phase4.buttonText"));
            if (isBuyer) {
                model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_4);
            } else {
                model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_4);
            }
            model.getPhaseInfo().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.info.buyer.phase4.headline") :
                    Res.get("bisqEasy.tradeState.info.seller.phase4.headline")
            );

            model.getActionButtonDisabled().set(true);
            model.getBuyersBtcBalance().set(Res.get("bisqEasy.tradeState.info.phase4.balance.value.notInMempoolYet"));
            UIScheduler.run(() ->
                            model.getBuyersBtcBalance().set(Res.get("bisqEasy.tradeState.info.phase4.balance.value",
                                    model.getFormattedBaseAmount().get(), "0"))).
                    after(2000);
            UIScheduler.run(() -> {
                                model.getActionButtonDisabled().set(false);
                                sendChatBotMessage(Res.get("bisqEasy.tradeState.info.phase4.chatBotMessage",
                                        model.getFormattedBaseAmount().get(), model.getBuyersBtcAddress().get()));
                        model.getBuyersBtcBalance().set(Res.get("bisqEasy.tradeState.info.phase4.balance.value",
                                model.getFormattedBaseAmount().get(), "1"));
                        btcConfirmed();
                            }
                    ).
                    after(4000);
        } else if (phaseIndex == 4) {
            // btc received
            model.getActionButtonVisible().set(false);
            model.getOpenDisputeButtonVisible().set(false);
            model.getPhaseInfo().set(isBuyer ?
                    Res.get("bisqEasy.tradeState.info.buyer.phase5.headline") :
                    Res.get("bisqEasy.tradeState.info.seller.phase5.headline")
            );
            model.getPhase().set(TradeStateModel.Phase.BUYER_PHASE_5);
            model.getPhase().set(TradeStateModel.Phase.SELLER_PHASE_5);
        }
    }

    private boolean isBuyer() {
        return isBuyer(model.getBisqEasyOffer());
    }

    private boolean isBuyer(BisqEasyOffer bisqEasyOffer) {
        return isMaker(bisqEasyOffer) ?
                bisqEasyOffer.getMakersDirection().isBuy() :
                bisqEasyOffer.getTakersDirection().isBuy();
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    private void applyFirstTimeItemsVisible() {
        model.getFirstTimeItemsVisible().set(!model.getIsCollapsed().get() && model.getPhaseIndex().get() == 0 && !getTradeRulesConfirmed());
    }

    private void applyPhaseAndInfoBoxVisible() {
        model.getPhaseAndInfoBoxVisible().set(!model.getIsCollapsed().get() && !model.getFirstTimeItemsVisible().get());
    }

    private Boolean getTradeRulesConfirmed() {
        return settingsService.getTradeRulesConfirmed().get();
    }
}
