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
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.notification.NotificationPanelView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class DashboardView extends View<ScrollPane, DashboardModel, DashboardController> {
    private static final int PADDING = 20;
    private static final Insets DEFAULT_PADDING = new Insets(30, 40, 40, 40);
    private static final Insets NOTIFICATION_PADDING = new Insets(10, 40, 40, 40);

    private final Button tradeProtocols, buildReputation;
    private final Label marketPriceLabel, marketCodeLabel, offersOnlineLabel, activeUsersLabel;
    private final GridPane gridPane;
    private Subscription isNotificationVisiblePin;

    public DashboardView(DashboardModel model, DashboardController controller) {
        super(new ScrollPane(), model, controller);

        gridPane = new GridPane();

        gridPane.setHgap(PADDING);
        gridPane.setVgap(PADDING);
        GridPaneUtil.setGridPaneTwoColumnsConstraints(gridPane);

        //First row
        Triple<VBox, Label, Label> priceTriple = getPriceBox(Res.get("dashboard.marketPrice"));
        VBox marketPrice = priceTriple.getFirst();
        marketPrice.setPrefWidth(350);
        marketPriceLabel = priceTriple.getSecond();
        marketCodeLabel = priceTriple.getThird();

        Pair<VBox, Label> offersPair = getValueBox(Res.get("dashboard.offersOnline"));
        VBox offersOnline = offersPair.getFirst();
        offersOnlineLabel = offersPair.getSecond();

        Pair<VBox, Label> usersPair = getValueBox(Res.get("dashboard.activeUsers"),
                Optional.of(Res.get("dashboard.activeUsers.tooltip")));
        VBox activeUsers = usersPair.getFirst();
        activeUsersLabel = usersPair.getSecond();

        HBox.setMargin(marketPrice, new Insets(0, -100, 0, -30));
        HBox hBox = new HBox(16, marketPrice, offersOnline, activeUsers);
        hBox.getStyleClass().add("bisq-box-2");
        hBox.setPadding(new Insets(20, 40, 20, 40));
        gridPane.add(hBox, 0, 0, 2, 1);

        //Second row
        VBox firstBox = getBigWidgetBox();
        VBox.setMargin(firstBox, new Insets(0, 0, 0, 0));
        VBox.setVgrow(firstBox, Priority.NEVER);
        gridPane.add(firstBox, 0, 1, 2, 1);

        //Third row
        Insets gridPaneInsets = new Insets(0, 0, -7.5, 0);
        GridPane subGridPane = GridPaneUtil.getTwoColumnsGridPane(PADDING, 15, gridPaneInsets);

        gridPane.add(subGridPane, 0, 2, 2, 1);

        String groupPaneStyleClass = "bisq-box-1";
        String headlineLabelStyleClass = "bisq-text-headline-2";
        String infoLabelStyleClass = "bisq-text-3";
        String buttonStyleClass = "large-button";
        Insets groupInsets = new Insets(36, 48, 44, 48);
        Insets headlineInsets = new Insets(36, 48, 0, 48);
        Insets infoInsets = new Insets(0, 48, 0, 48);
        Insets buttonInsets = new Insets(10, 48, 44, 48);

        tradeProtocols = new Button(Res.get("dashboard.second.button"));
        GridPaneUtil.fillColumn(subGridPane,
                0,
                tradeProtocols,
                buttonStyleClass,
                buttonInsets,
                Res.get("dashboard.second.headline"),
                headlineLabelStyleClass,
                "fiat-btc",
                16d,
                headlineInsets,
                Res.get("dashboard.second.content"),
                infoLabelStyleClass,
                infoInsets,
                0d,
                groupPaneStyleClass,
                groupInsets);

        buildReputation = new Button(Res.get("dashboard.third.button"));
        GridPaneUtil.fillColumn(subGridPane,
                1,
                buildReputation,
                buttonStyleClass,
                buttonInsets,
                Res.get("dashboard.third.headline"),
                headlineLabelStyleClass,
                "reputation-ribbon",
                16d,
                headlineInsets,
                Res.get("dashboard.third.content"),
                infoLabelStyleClass,
                infoInsets,
                0d,
                groupPaneStyleClass,
                groupInsets);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
        root.setContent(gridPane);
    }

    @Override
    protected void onViewAttached() {
        marketPriceLabel.textProperty().bind(model.getMarketPrice());
        marketCodeLabel.textProperty().bind(model.getMarketCode());
        offersOnlineLabel.textProperty().bind(model.getOffersOnline());
        activeUsersLabel.textProperty().bind(model.getActiveUsers());

        isNotificationVisiblePin = EasyBind.subscribe(model.getIsNotificationVisible(), visible -> {
            if (!visible) {
                UIScheduler.run(() -> gridPane.setPadding(DEFAULT_PADDING))
                        .after(NotificationPanelView.DURATION);
            } else {
                gridPane.setPadding(NOTIFICATION_PADDING);
            }
        });

        tradeProtocols.setOnAction(e -> controller.onOpenTradeOverview());
        buildReputation.setOnAction(e -> controller.onBuildReputation());
    }

    @Override
    protected void onViewDetached() {
        marketPriceLabel.textProperty().unbind();
        marketCodeLabel.textProperty().unbind();
        offersOnlineLabel.textProperty().unbind();
        activeUsersLabel.textProperty().unbind();

        isNotificationVisiblePin.unsubscribe();

        tradeProtocols.setOnAction(null);
        buildReputation.setOnAction(null);
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
        return getValueBox(title, Optional.empty());
    }

    private Pair<VBox, Label> getValueBox(String title, Optional<String> tooltipText) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9");
        HBox titleHBox = new HBox(titleLabel);
        titleHBox.setAlignment(Pos.CENTER);

        if (tooltipText.isPresent()) {
            ImageView tooltipIcon = ImageUtil.getImageViewById("info");
            tooltipIcon.setOpacity(0.6);
            BisqTooltip tooltip = new BisqTooltip(tooltipText.get());
            Tooltip.install(tooltipIcon, tooltip);
            titleHBox.getChildren().add(tooltipIcon);
            titleHBox.setSpacing(5);
        }

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-headline-3");

        VBox box = new VBox(titleHBox, valueLabel);
        box.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        return new Pair<>(box, valueLabel);
    }

    private VBox getBigWidgetBox() {
        Label headlineLabel = GridPaneUtil.getHeadline(Res.get("dashboard.main.headline"),
                "bisq-text-headline-4",
                null,
                0d);

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
                GridPaneUtil.getIconAndText(iconTxtStyle,
                        Res.get("dashboard.main.content1"),
                        "onboarding-2-offer-white"),
                GridPaneUtil.getIconAndText(iconTxtStyle,
                        Res.get("dashboard.main.content2"),
                        "onboarding-2-chat-white"),
                GridPaneUtil.getIconAndText(iconTxtStyle,
                        Res.get("dashboard.main.content3"),
                        "reputation-white"),
                button);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(30, 48, 44, 48));
        return vBox;
    }
}
