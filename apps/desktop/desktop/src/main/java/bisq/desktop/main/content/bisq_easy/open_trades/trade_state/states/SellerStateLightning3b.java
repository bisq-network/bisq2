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
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerStateLightning3b extends BaseState {
    private final Controller controller;

    public SellerStateLightning3b(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
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

        private void onButtonClicked() {
            // todo should we send a system message? if so we should change the text
            //sendTradeLogMessage(Res.get("bisqEasy.tradeState.info.phase3b.tradeLogMessage", model.getChannel().getMyUserIdentity().getUserName()));
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final WaitingAnimation waitingAnimation;

        private View(Model model, Controller controller) {
            super(model, controller);

            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.seller.phase3b.headline.ln"));
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.seller.phase3b.info.ln"));
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase3b.confirmButton.ln"));
            button.setDefaultButton(true);
            VBox.setMargin(button, new Insets(15, 0, 5, 0));

            root.getChildren().addAll(waitingInfo, button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            button.setOnAction(e -> controller.onButtonClicked());
            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.setOnAction(null);
            waitingAnimation.stop();
        }
    }
}
