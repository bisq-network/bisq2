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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashboardView extends View<GridPane, DashboardModel, DashboardController> {
    private static final int PADDING = 20;

    public DashboardView(DashboardModel model, DashboardController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(PADDING);
        root.setVgap(PADDING);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        root.getColumnConstraints().addAll(col1, col2);

        VBox marketPrice = getPriceBox(Res.get("dashboard.marketPrice"), "32149.34", "BTC/EUR");
        VBox offersOnline = getValueBox(Res.get("dashboard.offersOnline"), "231");
        VBox activeUsers = getValueBox(Res.get("dashboard.activeUsers"), "181");
        HBox hBox = new HBox(16, marketPrice, offersOnline, activeUsers);
        root.add(hBox, 0, 0, 2, 1);

        VBox firstBox = getBigWidgetBox();
        VBox.setMargin(firstBox, new Insets(0, 0, 0, 0));
        VBox.setVgrow(firstBox, Priority.NEVER);
        root.add(firstBox, 0, 1, 2, 1);

        VBox secondBox = getWidgetBox(
                "intro-3",
                Res.get("dashboard.second.headline"),
                Res.get("dashboard.second.content"),
                Res.get("dashboard.second.button"),
                controller::onOpenTradeOverview
        );
        root.add(secondBox, 0, 2, 1, 1);

        VBox thirdBox = getWidgetBox(
                "intro-2",
                Res.get("dashboard.third.headline"),
                Res.get("dashboard.third.content"),
                Res.get("dashboard.third.button"),
                controller::onLearn
        );
        root.add(thirdBox, 1, 2, 1, 1);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private VBox getPriceBox(String title, String value, String code) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        Label codeLabel = new Label(code);
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(9, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_RIGHT);
        VBox box = new VBox(titleLabel, hBox);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox getValueBox(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox getBigWidgetBox() {
        Label headlineLabel = new Label(Res.get("dashboard.main.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-4");
        headlineLabel.setWrapText(true);

        Button button = new Button(Res.get("dashboard.main.button"));
        button.setDefaultButton(true);
        button.getStyleClass().add("super-large-button");
        button.setOnAction(e -> controller.onOpenBisqEasy());
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));
        VBox.setMargin(button, new Insets(20, 0, 0, 0));
        VBox vBox = new VBox(15,
                headlineLabel,
                getIconAndText(Res.get("dashboard.main.content1"), "onboarding-2-offer-white"),
                getIconAndText(Res.get("dashboard.main.content2"), "onboarding-2-chat-white"),
                getIconAndText(Res.get("dashboard.main.content3"), "onboarding-1-reputation-white"),
                button);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(30, 48, 44, 48));
        return vBox;
    }

    private HBox getIconAndText(String text, String imageId) {
        Label label = new Label(text);
        label.setId("bisq-easy-onboarding-label");
        label.setWrapText(true);
        ImageView bulletPoint = ImageUtil.getImageViewById(imageId);
        HBox.setMargin(bulletPoint, new Insets(-3, 0, 0, 4));
        HBox hBox = new HBox(15, bulletPoint, label);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private VBox getWidgetBox(String imageId, String headline, String content, String buttonLabel, Runnable onAction) {
        Label headlineLabel = new Label(headline, ImageUtil.getImageViewById(imageId));
        headlineLabel.setGraphicTextGap(16.0);
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2");
        headlineLabel.setWrapText(true);

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().addAll("bisq-text-3");
        contentLabel.setAlignment(Pos.TOP_LEFT);
        contentLabel.setWrapText(true);

        Button button = new Button(buttonLabel);
        button.getStyleClass().add("large-button");
        button.setOnAction(e -> onAction.run());
        button.setMaxWidth(Double.MAX_VALUE);

        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        VBox.setMargin(contentLabel, new Insets(0, 0, 10, 0));
        VBox vBox = new VBox(16, headlineLabel, contentLabel, button);
        vBox.getStyleClass().add("bisq-box-1");
        vBox.setPadding(new Insets(36, 48, 52, 48));
        return vBox;
    }
}
