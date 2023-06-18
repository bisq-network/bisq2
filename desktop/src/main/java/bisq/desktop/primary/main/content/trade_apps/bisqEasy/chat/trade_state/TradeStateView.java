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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state;

import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final Label headline;
    private final HBox headerHBox;
    private final Button openTradeGuideButton, collapseButton, expandButton;
    private final HBox phaseAndInfoHBox;
    private final VBox firstTimeUserVBox;
    private Subscription isCollapsedPin, stateInfoVBoxPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradePhaseBox) {
        super(new VBox(0), model, controller);

        this.root.getStyleClass().addAll("bisq-easy-trade-state-bg");
        this.root.setPadding(new Insets(15, 30, 20, 30));


        // Welcome
        Label welcomeHeadline = new Label(Res.get("bisqEasy.tradeState.welcome.headline"));
        welcomeHeadline.getStyleClass().add("bisq-easy-trade-state-welcome-headline");

        Label welcomeInfo = new Label(Res.get("bisqEasy.tradeState.welcome.info"));
        welcomeInfo.getStyleClass().add("bisq-easy-trade-state-welcome-sub-headline");

        openTradeGuideButton = new Button(Res.get("bisqEasy.tradeState.openTradeGuide"));
        openTradeGuideButton.setDefaultButton(true);

        VBox.setMargin(welcomeHeadline, new Insets(20, 0, 10, 0));
        VBox.setMargin(openTradeGuideButton, new Insets(20, 0, 20, 0));
        firstTimeUserVBox = new VBox(Layout.hLine(), welcomeHeadline, welcomeInfo, openTradeGuideButton);


        // Header
        headline = new Label();
        headline.getStyleClass().add("bisq-easy-trade-state-headline");

        collapseButton = BisqIconButton.createIconButton("collapse");
        expandButton = BisqIconButton.createIconButton("expand");

        HBox.setMargin(collapseButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(expandButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(headline, new Insets(0, 0, 0, -2));
        headerHBox = new HBox(headline, Spacer.fillHBox(), collapseButton, expandButton);
        headerHBox.setCursor(Cursor.HAND);

        Tooltip tooltip = new Tooltip(Res.get("bisqEasy.tradeState.header.expandCollapse.tooltip"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);


        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        VBox.setVgrow(firstTimeUserVBox, Priority.ALWAYS);
        this.root.getChildren().addAll(headerHBox, firstTimeUserVBox, phaseAndInfoHBox);
    }

    @Override
    protected void onViewAttached() {
        firstTimeUserVBox.visibleProperty().bind(model.getFirstTimeItemsVisible());
        firstTimeUserVBox.managedProperty().bind(model.getFirstTimeItemsVisible());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoBoxVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoBoxVisible());
        headline.textProperty().bind(model.getHeadline());

        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());
        openTradeGuideButton.setOnAction(e -> controller.onOpenTradeGuide());

        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(), this::isCollapsedChanged);

        stateInfoVBoxPin = EasyBind.subscribe(model.getStateInfoVBox(),
                stateInfoVBox -> {
                    if (phaseAndInfoHBox.getChildren().size() == 2) {
                        phaseAndInfoHBox.getChildren().remove(1);
                    }
                    if (stateInfoVBox != null) {
                        HBox.setHgrow(stateInfoVBox, Priority.ALWAYS);
                        phaseAndInfoHBox.getChildren().add(stateInfoVBox);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        firstTimeUserVBox.visibleProperty().unbind();
        firstTimeUserVBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();
        headline.textProperty().unbind();

        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        headerHBox.setOnMouseClicked(null);
        openTradeGuideButton.setOnAction(null);

        isCollapsedPin.unsubscribe();
        stateInfoVBoxPin.unsubscribe();
    }

    private void isCollapsedChanged(Boolean isCollapsed) {
        collapseButton.setManaged(!isCollapsed);
        collapseButton.setVisible(!isCollapsed);
        expandButton.setManaged(isCollapsed);
        expandButton.setVisible(isCollapsed);

        if (isCollapsed) {
            VBox.setMargin(headerHBox, new Insets(0, 0, -17, 0));
        } else {
            VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        }
    }
}
