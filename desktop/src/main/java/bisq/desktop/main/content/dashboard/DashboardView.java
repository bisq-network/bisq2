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

package bisq.desktop.main.content.dashboard;

import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.desktop.common.utils.TwoColumnsUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DashboardView extends View<GridPane, DashboardModel, DashboardController> {
    private static final int PADDING = 20;
    private final Button tradeProtocols, learnMore;
    private final Label marketPriceLabel, marketCodeLabel, offersOnlineLabel, activeUsersLabel;

    public DashboardView(DashboardModel model, DashboardController controller) {
        super(new GridPane(), model, controller);

        root.setHgap(PADDING);
        root.setVgap(PADDING);
        TwoColumnsUtil.setColumnConstraints50percent(root);

        //First row
        Triple<VBox, Label, Label> priceTriple = getPriceBox(Res.get("dashboard.marketPrice"));
        VBox marketPrice = priceTriple.getFirst();
        marketPrice.setPrefWidth(350);
        marketPriceLabel = priceTriple.getSecond();
        marketCodeLabel = priceTriple.getThird();

        Pair<VBox, Label> offersPair = getValueBox(Res.get("dashboard.offersOnline"));
        VBox offersOnline = offersPair.getFirst();
        offersOnlineLabel = offersPair.getSecond();

        Pair<VBox, Label> usersPair = getValueBox(Res.get("dashboard.activeUsers"));
        VBox activeUsers = usersPair.getFirst();
        activeUsersLabel = usersPair.getSecond();

        HBox.setMargin(marketPrice, new Insets(0, -100, 0, -30));
        HBox hBox = new HBox(16, marketPrice, offersOnline, activeUsers);
        root.add(hBox, 0, 0, 2, 1);

        //Second row
        VBox firstBox = getBigWidgetBox();
        VBox.setMargin(firstBox, new Insets(0, 0, 0, 0));
        VBox.setVgrow(firstBox, Priority.NEVER);
        root.add(firstBox, 0, 1, 2, 1);

        //Third row
        Insets gridPaneInsets = new Insets(36, 48, 44, 48);
        GridPane gridPane = TwoColumnsUtil.getWidgetBoxGridPane(116, 5, gridPaneInsets, 50, 50);
        root.add(gridPane, 0, 2, 2, 1);

        tradeProtocols = new Button(Res.get("dashboard.second.button"));
        TwoColumnsUtil.fillColumnStandardStyle(gridPane,
                0,
                tradeProtocols,
                Res.get("dashboard.second.headline"),
                "fiat-btc",
                Res.get("dashboard.second.content"));

        learnMore = new Button(Res.get("dashboard.third.button"));
        TwoColumnsUtil.fillColumnStandardStyle(gridPane,
                1,
                learnMore,
                Res.get("dashboard.third.headline"),
                "learn",
                Res.get("dashboard.third.content"));
    }

    @Override
    protected void onViewAttached() {
        marketPriceLabel.textProperty().bind(model.getMarketPrice());
        marketCodeLabel.textProperty().bind(model.getMarketCode());
        offersOnlineLabel.textProperty().bind(model.getOffersOnline());
        activeUsersLabel.textProperty().bind(model.getActiveUsers());

        tradeProtocols.setOnAction(e -> controller.onOpenTradeOverview());
        learnMore.setOnAction(e -> controller.onLearn());
    }

    @Override
    protected void onViewDetached() {
        marketPriceLabel.textProperty().unbind();
        marketCodeLabel.textProperty().unbind();
        offersOnlineLabel.textProperty().unbind();
        activeUsersLabel.textProperty().unbind();

        tradeProtocols.setOnAction(null);
        learnMore.setOnAction(null);
    }

    private Triple<VBox, Label, Label> getPriceBox(String title) {
        Label titleLabel = new Label(title);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        Label codeLabel = new Label();
        codeLabel.getStyleClass().addAll("bisq-text-12");

        HBox hBox = new HBox(9, valueLabel, codeLabel);
        hBox.setAlignment(Pos.BASELINE_CENTER);
        VBox.setMargin(titleLabel, new Insets(0, 100, 0, 0));
        VBox box = new VBox(titleLabel, hBox);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return new Triple<>(box, valueLabel, codeLabel);
    }

    private Pair<VBox, Label> getValueBox(String title) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return new Pair<>(box, valueLabel);
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
        String iconTxtStyle = "bisq-easy-onboarding-big-box-bullet-point";
        VBox vBox = new VBox(15,
                headlineLabel,
                TwoColumnsUtil.getIconAndText(iconTxtStyle, Res.get("dashboard.main.content1"), "onboarding-2-offer-white"),
                TwoColumnsUtil.getIconAndText(iconTxtStyle,Res.get("dashboard.main.content2"), "onboarding-2-chat-white"),
                TwoColumnsUtil.getIconAndText(iconTxtStyle,Res.get("dashboard.main.content3"), "reputation-white"),
                button);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(30, 48, 44, 48));
        return vBox;
    }
}
