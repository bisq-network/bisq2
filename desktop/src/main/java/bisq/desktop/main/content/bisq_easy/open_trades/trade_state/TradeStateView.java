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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final HBox phaseAndInfoHBox, tradeInterruptedHBox, isInMediationHBox;
    private final Button interruptTrade, closeTradeButton, exportButton, reportToMediatorButton;
    private final Label tradeInterruptedInfo;
    private Subscription stateInfoVBoxPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradePhaseBox,
                          HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        interruptTrade = new Button();
        interruptTrade.setMinWidth(160);
        interruptTrade.getStyleClass().add("outlined-button");

        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), interruptTrade);


        Label isInMediationIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        isInMediationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        Label isInMediationInfo = new Label(Res.get("bisqEasy.openTrades.inMediation.info"));
        isInMediationInfo.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        isInMediationHBox = new HBox(10, isInMediationIcon, isInMediationInfo);
        isInMediationHBox.setAlignment(Pos.CENTER_LEFT);
        isInMediationHBox.setPadding(new Insets(10));
        isInMediationHBox.getStyleClass().add("bisq-easy-trade-isInMediation-bg");


        Label tradeInterruptedIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        tradeInterruptedIcon.getStyleClass().add("bisq-text-yellow");
        tradeInterruptedInfo = new Label();
        tradeInterruptedInfo.getStyleClass().add("bisq-easy-trade-interrupted-headline");

        exportButton = new Button(Res.get("bisqEasy.openTrades.exportTrade"));

        reportToMediatorButton = new Button(Res.get("bisqEasy.openTrades.reportToMediator"));
        reportToMediatorButton.getStyleClass().add("outlined-button");

        closeTradeButton = new Button(Res.get("bisqEasy.openTrades.closeTrade"));
        closeTradeButton.setMinWidth(160);
        closeTradeButton.setDefaultButton(true);

        tradeInterruptedHBox = new HBox(10, tradeInterruptedIcon, tradeInterruptedInfo, Spacer.fillHBox(),
                reportToMediatorButton, exportButton, closeTradeButton);
        tradeInterruptedHBox.setAlignment(Pos.CENTER_LEFT);


        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(isInMediationHBox, new Insets(20, 30, 0, 30));
        VBox.setMargin(tradeInterruptedHBox, new Insets(20, 30, 20, 30));
        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox vBox = new VBox(tradeDataHeader, Layout.hLine(), isInMediationHBox, tradeInterruptedHBox, phaseAndInfoHBox);
        vBox.getStyleClass().add("bisq-easy-container");

        root.getChildren().add(vBox);
    }

    @Override
    protected void onViewAttached() {
        reportToMediatorButton.visibleProperty().bind(model.getShowReportToMediatorButton());
        reportToMediatorButton.managedProperty().bind(model.getShowReportToMediatorButton());
        isInMediationHBox.visibleProperty().bind(model.getIsInMediation());
        isInMediationHBox.managedProperty().bind(model.getIsInMediation());
        tradeInterruptedHBox.visibleProperty().bind(model.getTradeInterrupted());
        tradeInterruptedHBox.managedProperty().bind(model.getTradeInterrupted());
        phaseAndInfoHBox.visibleProperty().bind(model.getTradeInterrupted().not());
        phaseAndInfoHBox.managedProperty().bind(model.getTradeInterrupted().not());

        tradeInterruptedInfo.textProperty().bind(model.getTradeInterruptedInfo());

        interruptTrade.textProperty().bind(model.getInterruptTradeButtonText());
        interruptTrade.visibleProperty().bind(model.getInterruptTradeButtonVisible());
        interruptTrade.managedProperty().bind(model.getInterruptTradeButtonVisible());

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

        interruptTrade.setOnAction(e -> controller.onInterruptTrade());
        closeTradeButton.setOnAction(e -> controller.onCloseTrade());
        exportButton.setOnAction(e -> controller.onExportTrade());
        reportToMediatorButton.setOnAction(e -> controller.onReportToMediator());
    }

    @Override
    protected void onViewDetached() {
        reportToMediatorButton.visibleProperty().unbind();
        reportToMediatorButton.managedProperty().unbind();
        isInMediationHBox.visibleProperty().unbind();
        isInMediationHBox.managedProperty().unbind();
        tradeInterruptedHBox.visibleProperty().unbind();
        tradeInterruptedHBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();

        tradeInterruptedInfo.textProperty().unbind();

        interruptTrade.textProperty().unbind();
        interruptTrade.visibleProperty().unbind();
        interruptTrade.managedProperty().unbind();

        stateInfoVBoxPin.unsubscribe();

        interruptTrade.setOnAction(null);
        closeTradeButton.setOnAction(null);
        exportButton.setOnAction(null);
        reportToMediatorButton.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }
}
