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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.TradeException;
import bisq.user.identity.UserIdentity;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState2 extends BaseState {
    private final Controller controller;

    public BuyerState2(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, UserIdentity myUserIdentity) {
        controller = new Controller(applicationService, bisqEasyOffer, myUserIdentity);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, UserIdentity myUserIdentity) {
            super(applicationService, bisqEasyOffer, myUserIdentity);
        }

        @Override
        protected Model createModel() {
            return new Model();
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onFiatSent() {
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2.chatBotMessage", model.getQuoteCode(), model.getBtcAddress().get()));
            try {
                bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTradeModel(), model.getBtcAddress().get());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty btcAddress = new SimpleStringProperty();
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField btcAddress;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase2.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");


            button = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase1.buttonText"));
            button.setDefaultButton(true);

            btcAddress = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress"), "", true);
            btcAddress.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.prompt"));

            MaterialTextArea account = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2.sellersAccount"),
                    model.getBisqEasyTradeModel().getSeller().getPaymentAccountData().get(), false);

            VBox.setMargin(button, new Insets(5, 0, 0, 0));
            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2.quoteAmount"), model.getFormattedQuoteAmount(), false),
                    account,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase2.btcAddress.headline", model.getFormattedQuoteAmount())),
                    btcAddress,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase2.confirm", model.getFormattedQuoteAmount())),
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            btcAddress.textProperty().bindBidirectional(model.getBtcAddress());

            button.disableProperty().bind(btcAddress.textProperty().isEmpty());
            button.setOnAction(e -> controller.onFiatSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.disableProperty().unbind();
        }
    }
}