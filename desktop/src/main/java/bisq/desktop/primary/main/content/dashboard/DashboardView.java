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

package bisq.desktop.primary.main.content.dashboard;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashboardView extends View<VBox, DashboardModel, DashboardController> {
    public DashboardView(DashboardModel model, DashboardController controller) {
        super(new VBox(16), model, controller);

        VBox tradesBox = getValueBox(Res.get("dashboard.marketPrice"), "3249.34 BTC/EUR");
        VBox peersBox1 = getValueBox(Res.get("dashboard.offersOnline"), "231");
        VBox peersBox2 = getValueBox(Res.get("dashboard.topReputation"), "8911");
        root.getChildren().add(new HBox(16, tradesBox, peersBox1, peersBox2));

        VBox firstBitcoinBox = getBigWidgetBox(
                Res.get("dashboard.myFirstBitcoin.headline"),
                Res.get("dashboard.myFirstBitcoin.content"),
                Res.get("dashboard.myFirstBitcoin.button"),
                controller::onOpenTradeOverview
        );
        VBox.setMargin(firstBitcoinBox, new Insets(20, 0, 0, 0));

        root.getChildren().add(firstBitcoinBox);

        VBox communityBox = getWidgetBox(
                "welcome-community",
                Res.get("dashboard.explore.headline"),
                Res.get("dashboard.explore.content"),
                Res.get("dashboard.explore.button"),
                controller::onOpenBisqEasy
        );

        VBox profileBox = getWidgetBox(
                "welcome-profile",
                Res.get("dashboard.newOffer.headline"),
                Res.get("dashboard.newOffer.content"),
                Res.get("dashboard.newOffer.button"),
                controller::onOpenOnboardingPopup
        );
        root.getChildren().add(new HBox(16, communityBox, profileBox));
    }

    private VBox getValueBox(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("bisq-text-7");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox getBigWidgetBox(String headline, String content, String buttonLabel, Runnable onAction) {
        Label headlineLabel = new Label(headline);
        headlineLabel.getStyleClass().add("bisq-text-headline-4");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().addAll("bisq-text-6", "wrap-text");
        contentLabel.setAlignment(Pos.TOP_LEFT);
        contentLabel.setMaxWidth(600);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("bisq-big-green-button");
        button.setOnAction(e -> onAction.run());
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(contentLabel, new Insets(0,0,10,0));
        VBox box = new VBox(8, headlineLabel, contentLabel, button);
        box.getStyleClass().add("bisq-box-2");
        box.setPadding(new Insets(30, 48, 44, 48));

        return box;
    }

    private VBox getWidgetBox(String imageId, String headline, String content, String buttonLabel, Runnable onAction) {
        Label headlineLabel = new Label(headline, ImageUtil.getImageViewById(imageId));
        headlineLabel.setGraphicTextGap(16.0);
        headlineLabel.setMaxWidth(284);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        contentLabel.setMaxWidth(600);
        contentLabel.setAlignment(Pos.TOP_LEFT);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("bisq-big-green-button");
        button.setOnAction(e -> onAction.run());
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(contentLabel, new Insets(0,0,10,0));
        VBox box = new VBox(16, headlineLabel, contentLabel, button);
        box.getStyleClass().add("bisq-box-1");
        box.setPadding(new Insets(36, 48, 52, 48));
        box.setMinWidth(420);
        box.setPrefWidth(420);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
