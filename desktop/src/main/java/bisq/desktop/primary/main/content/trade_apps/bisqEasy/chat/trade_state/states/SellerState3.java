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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState3 extends BaseState {
    private final Controller controller;

    public SellerState3(DefaultApplicationService applicationService, BisqEasyOffer bisqEasyOffer, UserIdentity myUserIdentity) {
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

        private void onBtcSent() {
            String txId = model.getTxId().get();
            sendChatBotMessage(Res.get("bisqEasy.tradeState.info.seller.phase3.chatBotMessage", txId));
            try {
                bisqEasyTradeService.sellerConfirmBtcSent(model.getBisqEasyTradeModel(), txId);
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        private final StringProperty txId = new SimpleStringProperty();
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField txId;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.seller.phase3.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.txId"), "", true);
            txId.textProperty().bindBidirectional(model.getTxId());
            txId.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase3.txId.prompt"));

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase3.buttonText"));
            button.setDefaultButton(true);

            VBox.setMargin(button, new Insets(5, 0, 0, 0));
            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.seller.phase3.sendBtc", model.getQuoteCode(), model.getFormattedBaseAmount())),
                    FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.baseAmount"), model.getFormattedBaseAmount(), false),
                    FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase3.btcAddress"), model.getBtcAddress(), false),
                    txId,
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            txId.textProperty().bindBidirectional(model.getTxId());
            button.disableProperty().bind(txId.textProperty().isEmpty());
            button.setOnAction(e -> controller.onBtcSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.disableProperty().unbind();
        }
    }
}