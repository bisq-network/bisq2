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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenTradesWelcome {
    private final Controller controller;

    public OpenTradesWelcome() {
        controller = new Controller();
    }

    public View getView() {
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

        private final Button button;

        public View(Model model, Controller controller) {
            super(new VBox(15), model, controller);

            root.getStyleClass().add("bisq-easy-open-trades-welcome-bg");
            root.setPadding(new Insets(30, 48, 44, 48));
            root.setAlignment(Pos.TOP_LEFT);

            Label headlineLabel = new Label(Res.get("bisqEasy.openTrades.welcome.headline"));
            headlineLabel.setWrapText(true);
            headlineLabel.getStyleClass().add("bisq-text-headline-4");

            Label infoHeadline = new Label(Res.get("bisqEasy.openTrades.welcome.info"));
            infoHeadline.setWrapText(true);
            infoHeadline.setAlignment(Pos.TOP_LEFT);
            infoHeadline.getStyleClass().add("bisq-easy-open-trades-welcome-info");
            // todo container structure does not guarantee the wrapping
            infoHeadline.setMinHeight(80);

            HBox line1 = getIconAndText(Res.get("bisqEasy.openTrades.welcome.line1"), "reputation");
            HBox line2 = getIconAndText(Res.get("bisqEasy.openTrades.welcome.line2"), "fiat-btc-small");
            HBox line3 = getIconAndText(Res.get("bisqEasy.openTrades.welcome.line3"), "thumbs-up");

            button = new Button(Res.get("bisqEasy.tradeGuide.open"));
            button.setDefaultButton(true);
            button.getStyleClass().add("super-large-button");
            button.setMaxWidth(Double.MAX_VALUE);

            VBox.setVgrow(infoHeadline, Priority.ALWAYS);
            VBox.setMargin(infoHeadline, new Insets(0, 0, -20, 0));
            VBox.setMargin(button, new Insets(20, 0, 0, 0));
            root.getChildren().addAll(headlineLabel, infoHeadline, line1, line2, line3, button);
        }

        @Override
        protected void onViewAttached() {
            button.setOnAction(e -> controller.onOpenTradeGuide());
        }

        @Override
        protected void onViewDetached() {
            button.setOnAction(null);
        }

        private HBox getIconAndText(String text, String imageId) {
            Label label = new Label(text);
            label.getStyleClass().add("bisq-easy-open-trades-welcome-lines");
            label.setWrapText(true);
            ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
            HBox.setMargin(bulletPoint, new Insets(-2, 0, 0, 4));
            HBox hBox = new HBox(15, bulletPoint, label);
            hBox.setAlignment(Pos.CENTER_LEFT);
            return hBox;
        }
    }
}