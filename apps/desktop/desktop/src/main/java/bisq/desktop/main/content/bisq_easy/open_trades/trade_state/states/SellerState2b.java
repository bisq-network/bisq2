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

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.WrappingText;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState2b extends BaseState {
    private final Controller controller;

    public SellerState2b(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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

        private void onConfirmFiatReceipt() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.seller.phase2b.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getFormattedQuoteAmount()));
            bisqEasyTradeService.sellerConfirmFiatReceipt(model.getBisqEasyTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final WrappingText headline;
        private final Button fiatReceivedButton;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = FormUtils.getHeadline();
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.seller.phase2b.info"));
            fiatReceivedButton = new Button();
            fiatReceivedButton.setDefaultButton(true);
            VBox.setMargin(fiatReceivedButton, new Insets(5, 0, 10, 0));
            root.getChildren().addAll(headline, info, fiatReceivedButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("bisqEasy.tradeState.info.seller.phase2b.headline", model.getFormattedQuoteAmount(), model.getBisqEasyTrade().getShortId()));
            fiatReceivedButton.setText(Res.get("bisqEasy.tradeState.info.seller.phase2b.fiatReceivedButton", model.getFormattedQuoteAmount()));
            fiatReceivedButton.setOnAction(e -> controller.onConfirmFiatReceipt());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            fiatReceivedButton.setOnAction(null);
        }
    }
}
