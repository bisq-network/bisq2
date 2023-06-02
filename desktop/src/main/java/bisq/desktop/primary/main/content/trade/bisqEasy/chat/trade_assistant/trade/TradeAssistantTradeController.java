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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.trade;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAssistantTradeController implements Controller {
    @Getter
    private final TradeAssistantTradeView view;
    private final TradeAssistantTradeModel model;
    private final DefaultApplicationService applicationService;

    public TradeAssistantTradeController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new TradeAssistantTradeModel();
        view = new TradeAssistantTradeView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateConfirmButtonText();
        }
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

    void onClose() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
        reset();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    private void reset() {
     /*   resetSelectedChildTarget();

        directionController.reset();
        marketController.reset();
        amountController.reset();
        paymentMethodController.reset();
        reviewOfferController.reset();

        model.reset();*/
    }

    private void updateConfirmButtonText() {

        //tradeInfo.phase.negotiation.confirmed=Trade terms agreed
        //tradeInfo.phase.fiat.sent=Fiat amount sent
        //tradeInfo.phase.fiat.received=Fiat amount received
        //tradeInfo.phase.btc.sent=Bitcoin sent
        //tradeInfo.phase.btc.received=Bitcoin received

        //model.getBisqEasyTrade().getBaseSideAmount()
        String text = Res.get("tradeAssistant.phase.negotiation.confirmed");
        model.getConfirmButtonText().set(text);
                
       /* if (NavigationTarget.CREATE_OFFER_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(marketController.getMarket().get() == null);
        } else if (NavigationTarget.CREATE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(paymentMethodController.getPaymentMethods().isEmpty());
        } else {
            model.getNextButtonDisabled().set(false);
        }*/
    }

    private void setButtonsVisible(boolean value) {
       /* model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_DIRECTION);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_REVIEW_OFFER);
        model.getCloseButtonVisible().set(value);*/
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisqeasy");
    }
}
