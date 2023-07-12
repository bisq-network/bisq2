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

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final Label headline;
    private final HBox headerHBox;
    private final Button collapseButton, expandButton;
    private final HBox phaseAndInfoHBox;
    private final VBox tradeWelcome;
    private final Region hLine;
    private Subscription isCollapsedPin, stateInfoVBoxPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradeWelcome,
                          VBox tradePhaseBox) {
        super(new VBox(0), model, controller);
        this.tradeWelcome = tradeWelcome;

        this.root.getStyleClass().addAll("bisq-easy-trade-state-bg");
        this.root.setPadding(new Insets(15, 30, 20, 30));

        headline = new Label();
        headline.getStyleClass().add("bisq-easy-trade-state-headline");

        collapseButton = BisqIconButton.createIconButton("collapse");
        expandButton = BisqIconButton.createIconButton("expand");

        HBox.setMargin(collapseButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(expandButton, new Insets(-1, -15, 0, 0));
        HBox.setMargin(headline, new Insets(2.5, 0, 0, -2));
        headerHBox = new HBox(headline, Spacer.fillHBox(), collapseButton, expandButton);
        headerHBox.setCursor(Cursor.HAND);

        BisqTooltip tooltip = new BisqTooltip(Res.get("bisqEasy.tradeState.header.expandCollapse.tooltip"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(headerHBox, new Insets(0, 0, 17, 0));
        VBox.setVgrow(tradeWelcome, Priority.ALWAYS);
        hLine = Layout.hLine();
        root.getChildren().addAll(headerHBox, hLine, tradeWelcome, phaseAndInfoHBox);
    }

    @Override
    protected void onViewAttached() {
        hLine.visibleProperty().bind(model.getIsCollapsed().not());
        hLine.managedProperty().bind(model.getIsCollapsed().not());
        collapseButton.visibleProperty().bind(model.getIsCollapsed().not());
        collapseButton.managedProperty().bind(model.getIsCollapsed().not());
        expandButton.visibleProperty().bind(model.getIsCollapsed());
        expandButton.managedProperty().bind(model.getIsCollapsed());
        tradeWelcome.visibleProperty().bind(model.getTradeWelcomeVisible());
        tradeWelcome.managedProperty().bind(model.getTradeWelcomeVisible());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoBoxVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoBoxVisible());
        headline.textProperty().bind(model.getHeadline());


        collapseButton.setOnAction(e -> controller.onCollapse());
        expandButton.setOnAction(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());

        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(),
                isCollapsed -> VBox.setMargin(headerHBox, new Insets(0, 0, isCollapsed ? -5 : 17.5, 0)));

        stateInfoVBoxPin = EasyBind.subscribe(model.getStateInfoVBox(),
                stateInfoVBox -> {
                    if (phaseAndInfoHBox.getChildren().size() == 2) {
                        phaseAndInfoHBox.getChildren().remove(1);
                    }
                    if (stateInfoVBox != null) {
                        HBox.setHgrow(stateInfoVBox, Priority.ALWAYS);
                        HBox.setMargin(stateInfoVBox, new Insets(20, 0, 0, 0));
                        phaseAndInfoHBox.getChildren().add(stateInfoVBox);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        hLine.visibleProperty().unbind();
        hLine.managedProperty().unbind();
        collapseButton.visibleProperty().unbind();
        collapseButton.managedProperty().unbind();
        expandButton.visibleProperty().unbind();
        expandButton.managedProperty().unbind();
        tradeWelcome.visibleProperty().unbind();
        tradeWelcome.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();
        headline.textProperty().unbind();

        collapseButton.setOnAction(null);
        expandButton.setOnAction(null);
        headerHBox.setOnMouseClicked(null);

        isCollapsedPin.unsubscribe();
        stateInfoVBoxPin.unsubscribe();
    }
}
