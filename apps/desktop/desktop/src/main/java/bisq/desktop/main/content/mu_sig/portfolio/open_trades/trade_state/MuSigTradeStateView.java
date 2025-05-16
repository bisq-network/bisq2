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

package bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
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
public class MuSigTradeStateView extends View<VBox, MuSigTradeStateModel, MuSigTradeStateController> {
    private final HBox phaseAndInfoHBox, topHBox, errorHBox, isInMediationHBox;
    private final Button closeTradeButton, exportButton, reportToMediatorButton,
            tradeDetailsButton;
    private final Label errorMessage, mediationBannerLabel;
    private final VBox tradePhaseBox, tradeDataHeaderBox;
    private final BisqMenuItem tryAgainMenuItem;
    private Subscription stateInfoVBoxPin, requestMediationDeliveryStatusPin,
            shouldShowTryRequestMediationAgainPin;

    public MuSigTradeStateView(MuSigTradeStateModel model,
                               MuSigTradeStateController controller,
                               VBox tradePhaseBox,
                               HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        this.tradePhaseBox = tradePhaseBox;

        tradeDetailsButton = new Button(Res.get("bisqEasy.openTrades.tradeDetails.button"));
        tradeDetailsButton.getStyleClass().addAll("grey-transparent-outlined-button");
        tradeDetailsButton.setMinWidth(160);

        HBox.setMargin(tradeDetailsButton, new Insets(0, -20, 0, 0));
        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), tradeDetailsButton);
        tradeDataHeaderBox = new VBox(tradeDataHeader, Layout.hLine());

        Label isInMediationIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        isInMediationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        mediationBannerLabel = new Label();
        mediationBannerLabel.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        tryAgainMenuItem = new BisqMenuItem("try-again-dark", "try-again-white");
        tryAgainMenuItem.useIconOnly(22);
        tryAgainMenuItem.setTooltip(new BisqTooltip(Res.get("bisqEasy.tradeState.requestMediation.resendRequest.tooltip")));
        isInMediationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        isInMediationHBox = new HBox(10, isInMediationIcon, mediationBannerLabel, tryAgainMenuItem);
        isInMediationHBox.setAlignment(Pos.CENTER_LEFT);
        isInMediationHBox.setPadding(new Insets(10));
        isInMediationHBox.getStyleClass().add("bisq-easy-trade-isInMediation-bg");

        exportButton = new Button(Res.get("bisqEasy.openTrades.exportTrade"));
        exportButton.setMinWidth(180);

        reportToMediatorButton = new Button(Res.get("bisqEasy.openTrades.reportToMediator"));
        reportToMediatorButton.getStyleClass().add("outlined-button");

        closeTradeButton = new Button(Res.get("bisqEasy.openTrades.closeTrade"));
        closeTradeButton.setMinWidth(160);
        closeTradeButton.setDefaultButton(true);

        Label errorIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        errorIcon.getStyleClass().add("bisq-text-error");
        errorIcon.setMinWidth(16);
        errorMessage = new Label();
        errorMessage.getStyleClass().add("bisq-easy-trade-failed-headline");
        errorHBox = new HBox(10, errorIcon, errorMessage);
        errorHBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttonHBox = new HBox(10, reportToMediatorButton, exportButton, closeTradeButton);
        topHBox = new HBox(10, errorHBox, Spacer.fillHBox(), buttonHBox);
        topHBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(isInMediationHBox, new Insets(20, 30, 0, 30));
        VBox.setMargin(topHBox, new Insets(20, 30, 20, 30));
        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox content = new VBox(tradeDataHeaderBox, isInMediationHBox, topHBox, phaseAndInfoHBox);
        content.getStyleClass().add("bisq-easy-container");

        root.getChildren().add(content);
    }

    @Override
    protected void onViewAttached() {
        tradePhaseBox.visibleProperty().bind(model.getIsTradeCompleted().not());
        tradePhaseBox.managedProperty().bind(model.getIsTradeCompleted().not());
        tradeDataHeaderBox.visibleProperty().bind(model.getIsTradeCompleted().not());
        tradeDataHeaderBox.managedProperty().bind(model.getIsTradeCompleted().not());
        reportToMediatorButton.visibleProperty().bind(model.getShowReportToMediatorButton());
        reportToMediatorButton.managedProperty().bind(model.getShowReportToMediatorButton());
        isInMediationHBox.visibleProperty().bind(model.getIsInMediation());
        isInMediationHBox.managedProperty().bind(model.getIsInMediation());
        errorHBox.visibleProperty().bind(model.getError());
        errorHBox.managedProperty().bind(model.getError());
        topHBox.visibleProperty().bind(model.getIsTradeCompleted().not().or(model.getError()));
        topHBox.managedProperty().bind(topHBox.visibleProperty());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoVisible());

        errorMessage.textProperty().bind(model.getErrorMessage());

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

        shouldShowTryRequestMediationAgainPin = EasyBind.subscribe(model.getShouldShowTryRequestMediationAgain(), showTryAgain -> {
            tryAgainMenuItem.setVisible(showTryAgain);
            tryAgainMenuItem.setManaged(showTryAgain);
        });

        requestMediationDeliveryStatusPin = EasyBind.subscribe(model.getRequestMediationDeliveryStatus(),
                status -> {
                    // If the peer had sent the request we do not get any requestMediationDeliveryStatus status is null.
                    if (status == null || status == MessageDeliveryStatus.ACK_RECEIVED || status == MessageDeliveryStatus.MAILBOX_MSG_RECEIVED) {
                        mediationBannerLabel.setText(Res.get("bisqEasy.openTrades.inMediation.info"));
                    } else {
                        String resendRequest = model.getShouldShowTryRequestMediationAgain().get()
                                ? Res.get("bisqEasy.tradeState.requestMediation.resendRequest")
                                : "";
                        String key = "bisqEasy.tradeState.requestMediation.deliveryState." + status.name();
                        String deliveryStatus = Res.get(key, resendRequest);
                        if (status == MessageDeliveryStatus.FAILED) {
                            mediationBannerLabel.setText(deliveryStatus);
                        } else {
                            mediationBannerLabel.setText(Res.get("bisqEasy.openTrades.inMediation.requestSent", deliveryStatus));
                        }
                    }
                });

        tradeDetailsButton.setOnAction(e -> controller.onShowTradeDetails());
        closeTradeButton.setOnAction(e -> controller.onCloseTrade());
        exportButton.setOnAction(e -> controller.onExportTrade());
        reportToMediatorButton.setOnAction(e -> controller.onRequestMediation());
        tryAgainMenuItem.setOnAction(e -> controller.onResendMediationRequest());
    }

    @Override
    protected void onViewDetached() {
        tradePhaseBox.visibleProperty().unbind();
        tradePhaseBox.managedProperty().unbind();
        tradeDataHeaderBox.visibleProperty().unbind();
        tradeDataHeaderBox.managedProperty().unbind();
        topHBox.visibleProperty().unbind();
        topHBox.managedProperty().unbind();
        reportToMediatorButton.visibleProperty().unbind();
        reportToMediatorButton.managedProperty().unbind();
        isInMediationHBox.visibleProperty().unbind();
        isInMediationHBox.managedProperty().unbind();
        errorHBox.visibleProperty().unbind();
        errorHBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();

        errorMessage.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
        requestMediationDeliveryStatusPin.unsubscribe();
        shouldShowTryRequestMediationAgainPin.unsubscribe();

        tradeDetailsButton.setOnAction(null);
        closeTradeButton.setOnAction(null);
        exportButton.setOnAction(null);
        reportToMediatorButton.setOnAction(null);
        tryAgainMenuItem.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }
}
