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
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final HBox phaseAndInfoHBox, cancelledHBox, interruptedHBox, errorHBox, isInMediationHBox;
    private final Button cancelButton, closeTradeButton, exportButton, reportToMediatorButton, acceptSellersPriceButton,
            rejectPriceButton;
    private final Label cancelledInfo, errorMessage;
    private VBox sellerPriceApprovalOverlay;
    private Pane sellerPriceApprovalContent;
    private Subscription stateInfoVBoxPin, showSellersPriceApprovalOverlayPin, sellerPriceApprovalContentPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradePhaseBox,
                          HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        cancelButton = new Button();
        cancelButton.setMinWidth(160);
        cancelButton.getStyleClass().add("outlined-button");

        rejectPriceButton = new Button();
        rejectPriceButton.setMinWidth(160);
        rejectPriceButton.setDefaultButton(true);

        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), cancelButton);


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
        tradeInterruptedIcon.setMinWidth(16);
        cancelledInfo = new Label();
        cancelledInfo.getStyleClass().add("bisq-easy-trade-interrupted-headline");

        exportButton = new Button(Res.get("bisqEasy.openTrades.exportTrade"));
        exportButton.setMinWidth(180);

        reportToMediatorButton = new Button(Res.get("bisqEasy.openTrades.reportToMediator"));
        reportToMediatorButton.getStyleClass().add("outlined-button");

        closeTradeButton = new Button(Res.get("bisqEasy.openTrades.closeTrade"));
        closeTradeButton.setMinWidth(160);
        closeTradeButton.setDefaultButton(true);

        cancelledHBox = new HBox(10, tradeInterruptedIcon, cancelledInfo);
        cancelledHBox.setAlignment(Pos.CENTER_LEFT);

        Label errorIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        errorIcon.getStyleClass().add("bisq-text-error");
        errorIcon.setMinWidth(16);
        errorMessage = new Label();
        errorMessage.getStyleClass().add("bisq-easy-trade-failed-headline");
        errorHBox = new HBox(10, errorIcon, errorMessage);
        errorHBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttonHBox = new HBox(10, reportToMediatorButton, exportButton, closeTradeButton);
        interruptedHBox = new HBox(10, cancelledHBox, errorHBox, Spacer.fillHBox(), buttonHBox);
        interruptedHBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(isInMediationHBox, new Insets(20, 30, 0, 30));
        VBox.setMargin(interruptedHBox, new Insets(20, 30, 20, 30));
        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox content = new VBox(tradeDataHeader, Layout.hLine(), isInMediationHBox, interruptedHBox, phaseAndInfoHBox);
        content.getStyleClass().add("bisq-easy-container");

        acceptSellersPriceButton = new Button(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.button.accept"));
        acceptSellersPriceButton.getStyleClass().add("outlined-button");
        setUpSellerPriceApprovalOverlay();

        StackPane layeredContent = new StackPane(content, sellerPriceApprovalOverlay);
        root.getChildren().add(layeredContent);
    }

    @Override
    protected void onViewAttached() {
        reportToMediatorButton.visibleProperty().bind(model.getShowReportToMediatorButton());
        reportToMediatorButton.managedProperty().bind(model.getShowReportToMediatorButton());
        isInMediationHBox.visibleProperty().bind(model.getIsInMediation());
        isInMediationHBox.managedProperty().bind(model.getIsInMediation());
        cancelledHBox.visibleProperty().bind(model.getCancelled());
        cancelledHBox.managedProperty().bind(model.getCancelled());
        interruptedHBox.visibleProperty().bind(model.getCancelled().or(model.getError()));
        interruptedHBox.managedProperty().bind(model.getCancelled().or(model.getError()));
        errorHBox.visibleProperty().bind(model.getError());
        errorHBox.managedProperty().bind(model.getError());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoVisible());

        cancelledInfo.textProperty().bind(model.getTradeInterruptedInfo());
        errorMessage.textProperty().bind(model.getErrorMessage());

        cancelButton.textProperty().bind(model.getInterruptTradeButtonText());
        cancelButton.visibleProperty().bind(model.getCancelButtonVisible());
        cancelButton.managedProperty().bind(model.getCancelButtonVisible());

        rejectPriceButton.textProperty().bind(model.getInterruptTradeButtonText());
        rejectPriceButton.visibleProperty().bind(model.getCancelButtonVisible());
        rejectPriceButton.managedProperty().bind(model.getCancelButtonVisible());

        stateInfoVBoxPin = EasyBind.subscribe(model.getStateInfoVBox(), stateInfoVBox -> {
            if (phaseAndInfoHBox.getChildren().size() == 2) {
                phaseAndInfoHBox.getChildren().remove(1);
            }
            if (stateInfoVBox != null) {
                HBox.setHgrow(stateInfoVBox, Priority.ALWAYS);
                HBox.setMargin(stateInfoVBox, new Insets(20, 0, 0, 0));
                phaseAndInfoHBox.getChildren().add(stateInfoVBox);
            }
        });

        showSellersPriceApprovalOverlayPin = EasyBind.subscribe(model.getShouldShowSellerPriceApprovalOverlay(), shouldShow -> {
            if (shouldShow) {
                sellerPriceApprovalOverlay.setVisible(true);
                sellerPriceApprovalOverlay.setManaged(true);
                Transitions.blurStrong(phaseAndInfoHBox, 0);
                Transitions.slideInTop(sellerPriceApprovalOverlay, 450);
            } else {
                sellerPriceApprovalOverlay.setVisible(false);
                sellerPriceApprovalOverlay.setManaged(false);
                Transitions.removeEffect(phaseAndInfoHBox);
            }
        });

        sellerPriceApprovalContentPin = EasyBind.subscribe(model.getSellerPriceApprovalContent(), content -> {
            if (content != null) {
                content.getStyleClass().setAll("seller-price-approval-content");
                sellerPriceApprovalContent.getChildren().setAll(content);
            }
        });

        cancelButton.setOnAction(e -> controller.onInterruptTrade());
        closeTradeButton.setOnAction(e -> controller.onCloseTrade());
        exportButton.setOnAction(e -> controller.onExportTrade());
        rejectPriceButton.setOnAction(e -> controller.onInterruptTrade());
        reportToMediatorButton.setOnAction(e -> controller.onReportToMediator());
        acceptSellersPriceButton.setOnAction(e -> controller.onAcceptSellersPriceButton());
    }

    @Override
    protected void onViewDetached() {
        reportToMediatorButton.visibleProperty().unbind();
        reportToMediatorButton.managedProperty().unbind();
        isInMediationHBox.visibleProperty().unbind();
        isInMediationHBox.managedProperty().unbind();
        cancelledHBox.visibleProperty().unbind();
        cancelledHBox.managedProperty().unbind();
        interruptedHBox.visibleProperty().unbind();
        interruptedHBox.managedProperty().unbind();
        errorHBox.visibleProperty().unbind();
        errorHBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();

        cancelledInfo.textProperty().unbind();
        errorMessage.textProperty().unbind();

        cancelButton.textProperty().unbind();
        cancelButton.visibleProperty().unbind();
        cancelButton.managedProperty().unbind();

        rejectPriceButton.textProperty().unbind();
        rejectPriceButton.visibleProperty().unbind();
        rejectPriceButton.managedProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
        showSellersPriceApprovalOverlayPin.unsubscribe();
        sellerPriceApprovalContentPin.unsubscribe();

        cancelButton.setOnAction(null);
        closeTradeButton.setOnAction(null);
        exportButton.setOnAction(null);
        rejectPriceButton.setOnAction(null);
        reportToMediatorButton.setOnAction(null);
        acceptSellersPriceButton.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }

    private void setUpSellerPriceApprovalOverlay() {
        sellerPriceApprovalOverlay = new VBox();
        sellerPriceApprovalOverlay.setAlignment(Pos.TOP_LEFT);
        sellerPriceApprovalOverlay.getStyleClass().addAll("trade-wizard-feedback-bg", "seller-price-approval-popup");
        sellerPriceApprovalOverlay.visibleProperty().set(false);
        sellerPriceApprovalOverlay.managedProperty().set(false);

        Label sellerPriceApprovalTitleLabel = new Label(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.title"));
        sellerPriceApprovalTitleLabel.getStyleClass().addAll("seller-price-approval-title", "large-text", "font-default");
        sellerPriceApprovalContent = new Pane();
        sellerPriceApprovalContent.getStyleClass().addAll("seller-price-approval-description", "normal-text", "font-default");
        Label sellerPriceApprovalQuestionLabel = new Label(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.question"));
        sellerPriceApprovalQuestionLabel.getStyleClass().addAll("seller-price-approval-description", "normal-text",
                "font-default", "seller-price-approval-question");
        HBox sellerPriceApprovalButtons = new HBox(10, acceptSellersPriceButton, rejectPriceButton);
        sellerPriceApprovalButtons.setAlignment(Pos.BOTTOM_RIGHT);

        sellerPriceApprovalOverlay.getChildren().addAll(sellerPriceApprovalTitleLabel, sellerPriceApprovalContent,
                sellerPriceApprovalQuestionLabel, Spacer.fillVBox(), sellerPriceApprovalButtons);
        StackPane.setAlignment(sellerPriceApprovalOverlay, Pos.TOP_CENTER);
        StackPane.setMargin(sellerPriceApprovalOverlay, new Insets(63, 0, 0, 0));
    }
}
