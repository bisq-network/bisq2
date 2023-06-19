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
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState4 extends BaseState {
    private final Controller controller;

    public BuyerState4(DefaultApplicationService applicationService, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(applicationService, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(DefaultApplicationService applicationService, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(applicationService, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.setTxId(model.getBisqEasyTrade().getTxId().get());
            model.setBtcAddress(model.getBisqEasyTrade().getBtcAddress().get());
            model.getBtcBalance().set("");
            model.getConfirmations().set(Res.get("bisqEasy.tradeState.info.phase4.balance.help.notInMempoolYet"));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onComplete() {
            try {
                bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        @Setter
        protected String txId;
        private final StringProperty btcBalance = new SimpleStringProperty();
        private final StringProperty confirmations = new SimpleStringProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField txId, btcBalance;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase4.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase4.txId"), "", false);
            btcBalance = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase4.balance"), "", false);
            btcBalance.setHelpText(Res.get("bisqEasy.tradeState.info.phase4.balance.help.notInMempoolYet"));

            button = new Button(Res.get("bisqEasy.tradeState.info.phase4.buttonText"));
            button.setDefaultButton(true);

            VBox.setMargin(button, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    infoHeadline,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase4.info")),
                    txId,
                    btcBalance,
                    button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            txId.setText(model.getTxId());
            btcBalance.textProperty().bind(model.getBtcBalance());
            btcBalance.helpHelpProperty().bind(model.getConfirmations());
            button.setOnAction(e -> controller.onComplete());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            btcBalance.textProperty().unbind();
            btcBalance.helpHelpProperty().unbind();
            button.setOnAction(null);
        }
    }
}