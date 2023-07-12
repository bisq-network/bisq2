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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class TradeWelcome {
    private final Controller controller;

    TradeWelcome() {
        controller = new Controller();
    }

    View getView() {
        return controller.getView();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller() {

            model = new Model();
            view = new View(model, this);
        }


        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        void onOpenTradeGuide() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final Button openTradeGuideButton;

        public View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setAlignment(Pos.CENTER);

            Label welcomeHeadline = new Label(Res.get("bisqEasy.tradeState.welcome.headline"));
            welcomeHeadline.getStyleClass().add("bisq-easy-trade-state-welcome-headline");

            MultiLineLabel infoHeadline = new MultiLineLabel(Res.get("bisqEasy.tradeState.welcome.info"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-text");
            infoHeadline.setTextAlignment(TextAlignment.CENTER);

            openTradeGuideButton = new Button(Res.get("bisqEasy.tradeState.openTradeGuide"));
            openTradeGuideButton.setDefaultButton(true);

            VBox.setMargin(welcomeHeadline, new Insets(20, 0, 20, 0));
            VBox.setMargin(openTradeGuideButton, new Insets(30, 0, 20, 0));
            root.getChildren().addAll(welcomeHeadline, infoHeadline, openTradeGuideButton);
        }

        @Override
        protected void onViewAttached() {
            openTradeGuideButton.setOnAction(e -> controller.onOpenTradeGuide());
        }

        @Override
        protected void onViewDetached() {
            openTradeGuideButton.setOnAction(null);
        }
    }
}