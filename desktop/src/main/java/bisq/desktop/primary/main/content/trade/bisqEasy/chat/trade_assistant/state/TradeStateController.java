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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.state;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeStateController implements Controller {
    @Getter
    private final TradeStateView view;
    private final TradeStateModel model;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;

    public TradeStateController(DefaultApplicationService applicationService) {
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        marketPriceService = applicationService.getOracleService().getMarketPriceService();

        model = new TradeStateModel();
        view = new TradeStateView(model, this);
    }

    public void selectChannel(BisqEasyPrivateTradeChatChannel channel) {
        BisqEasyOffer bisqEasyOffer = channel.getBisqEasyOffer();
        model.setBisqEasyOffer(bisqEasyOffer);

        boolean isBuyer = bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds()) ?
                bisqEasyOffer.getMakersDirection().isBuy() :
                bisqEasyOffer.getTakersDirection().isBuy();

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

    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.TRADE_DETAILS);
    }

    void onAction() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateConfirmButtonText();
        }
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }

    void onOpenDispute() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateConfirmButtonText();
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
