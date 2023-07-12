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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.WaitingAnimation;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState1 extends BaseState {
    private final Controller controller;

    public BuyerState1(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
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
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        public Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final WaitingAnimation waitingAnimation;

        private View(Model model, Controller controller) {
            super(model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.buyer.phase1.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            waitingAnimation = new WaitingAnimation();
            VBox.setMargin(waitingAnimation, new Insets(20, 0, 0, 20));
            root.getChildren().addAll(
                    infoHeadline,
                    FormUtils.getLabel(Res.get("bisqEasy.tradeState.info.buyer.phase1.info")),
                    waitingAnimation);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            waitingAnimation.stop();
        }
    }
}