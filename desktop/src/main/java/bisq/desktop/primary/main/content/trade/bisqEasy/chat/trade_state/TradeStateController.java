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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

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

        boolean isBuyer = bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds()) ?
                bisqEasyOffer.getMakersDirection().isBuy() :
                bisqEasyOffer.getTakersDirection().isBuy();

        model.getPhaseInfo().set(Res.get("bisqEasy.assistant.tradeState.phaseInfo.phase1"));
        model.getPhase2().set(isBuyer ? Res.get("bisqEasy.assistant.tradeState.phase.buyer.phase2").toUpperCase() :
                Res.get("bisqEasy.assistant.tradeState.phase.seller.phase2").toUpperCase());
        model.getPhase3().set(isBuyer ? Res.get("bisqEasy.assistant.tradeState.phase.buyer.phase3").toUpperCase() :
                Res.get("bisqEasy.assistant.tradeState.phase.seller.phase3").toUpperCase());

        //todo


        model.getActionButtonVisible().set(true);
        model.getOpenDisputeButtonVisible().set(true);
        model.getActionButtonText().set(isBuyer ?
                Res.get("bisqEasy.assistant.tradeState.actionButton.sendBitcoinAddress") :
                Res.get("bisqEasy.assistant.tradeState.actionButton.sendFiatAccountData")
        );
        model.getActivePhaseIndex().set(2);


        String directionString = isBuyer ?
                Res.get("offer.buying").toUpperCase() :
                Res.get("offer.selling").toUpperCase();
        AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String amountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
        // String amountString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), hasAmountRange, true);
        FiatPaymentMethodSpec fiatPaymentMethodSpec = bisqEasyOffer.getQuoteSidePaymentMethodSpecs().get(0);
        String paymentMethodName = fiatPaymentMethodSpec.getPaymentMethod().getDisplayString();
        String tradeInfo = Res.get("bisqEasy.assistant.tradeState.headline",
                directionString,
                amountString,
                paymentMethodName);

        model.getTradeInfo().set(tradeInfo);
    }

    @Override
    public void onActivate() {
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
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

    void onAction() {
        //todo
        onSendPaymentAccount();
        //onSendBtcAddress()
    }

    void onSendBtcAddress() {
        checkArgument(walletService.isPresent());
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
                ));
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

    private void updateConfirmButtonText() {

        //tradeInfo.phase.negotiation.confirmed=Trade terms agreed
        //tradeInfo.phase.fiat.sent=Fiat amount sent
        //tradeInfo.phase.fiat.received=Fiat amount received
        //tradeInfo.phase.btc.sent=Bitcoin sent
        //tradeInfo.phase.btc.received=Bitcoin received

        //model.getBisqEasyTrade().getBaseSideAmount()
        String text = Res.get("tradeAssistant.phase.negotiation.confirmed");
        model.getActionButtonText().set(text);
                
       /* if (NavigationTarget.CREATE_OFFER_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(marketController.getMarket().get() == null);
        } else if (NavigationTarget.CREATE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(paymentMethodController.getPaymentMethods().isEmpty());
        } else {
            model.getNextButtonDisabled().set(false);
        }*/
    }


}
