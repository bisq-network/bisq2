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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState2b extends BaseState {
    private final Controller controller;

    public BuyerState2b(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
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

        private void onConfirmFiatSent() {
            sendSystemMessage(Res.get("bisqEasy.tradeState.info.buyer.phase2b.systemMessage", model.getQuoteCode()));
            try {
                bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTrade());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }
    }

    @Getter
    private static class Model extends BaseState.Model {

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton;
        private final MaterialTextArea account;
        private final MaterialTextField quoteAmount;
        private final WrappingText headline;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = FormUtils.getHeadline();

            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2b.quoteAmount"), "", false);
            account = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2b.sellersAccount"), "", false);
            account.setHelpText(Res.get("bisqEasy.tradeState.info.buyer.phase2b.reasonForPaymentInfo"));

            confirmFiatSentButton = new Button();
            confirmFiatSentButton.setDefaultButton(true);
            VBox.setMargin(confirmFiatSentButton, new Insets(0, 0, 5, 0));

            root.getChildren().addAll(
                    headline,
                    quoteAmount,
                    account,
                    confirmFiatSentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2b.headline", model.getFormattedQuoteAmount()));
            quoteAmount.setText(model.getFormattedQuoteAmount());
            account.setText(model.getBisqEasyTrade().getPaymentAccountData().get());
            confirmFiatSentButton.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2b.confirmFiatSent", model.getFormattedQuoteAmount()));
            confirmFiatSentButton.setOnAction(e -> controller.onConfirmFiatSent());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmFiatSentButton.setOnAction(null);
        }
    }
}
