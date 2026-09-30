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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_state;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MuSigTradeStateView extends View<VBox, MuSigTradeStateModel, MuSigTradeStateController> {
    private final HBox phaseAndInfoHBox, errorHBox, isInMediationHBox, isInArbitrationHBox;
    private final Button closeTradeButton, exportButton, reportToMediatorButton,
            tradeDetailsButton, acceptMediationResultButton, rejectMediationResultButton;
    private final Label errorMessage, mediationBannerLabel, arbitrationBannerLabel;
    private final VBox tradePhaseBox, tradeDataHeaderBox;
    private final BisqMenuItem tryAgainMenuItem;
    private Subscription stateInfoVBoxPin, shouldShowTryRequestMediationAgainPin;

    public MuSigTradeStateView(MuSigTradeStateModel model,
                               MuSigTradeStateController controller,
                               VBox tradePhaseBox,
                               HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        this.tradePhaseBox = tradePhaseBox;

        tradeDetailsButton = new Button(Res.get("muSig.trade.details.button"));
        tradeDetailsButton.getStyleClass().addAll("grey-transparent-outlined-button");
        tradeDetailsButton.setMinWidth(160);

        HBox.setMargin(tradeDetailsButton, new Insets(0, -20, 0, 0));
        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), tradeDetailsButton);
        tradeDataHeaderBox = new VBox(tradeDataHeader, Layout.hLine());

        Label isInMediationIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        isInMediationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        mediationBannerLabel = new Label();
        mediationBannerLabel.getStyleClass().add("bisq-easy-trade-isInMediation-headline");
        mediationBannerLabel.setWrapText(true);
        mediationBannerLabel.setMaxWidth(Double.MAX_VALUE);
        mediationBannerLabel.setMinHeight(Region.USE_PREF_SIZE);

        tryAgainMenuItem = new BisqMenuItem("try-again-dark", "try-again-white");
        tryAgainMenuItem.useIconOnly(22);
        tryAgainMenuItem.setTooltip(new BisqTooltip(Res.get("muSig.trade.requestMediation.resendRequest.tooltip")));
        isInMediationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        acceptMediationResultButton = new Button(Res.get("muSig.mediation.result.accept"));
        acceptMediationResultButton.getStyleClass().add("accept-button");
        acceptMediationResultButton.setVisible(false);
        acceptMediationResultButton.setManaged(false);
        acceptMediationResultButton.setMinWidth(90);

        rejectMediationResultButton = new Button(Res.get("muSig.mediation.result.reject"));
        rejectMediationResultButton.getStyleClass().add("reject-button");
        rejectMediationResultButton.setVisible(false);
        rejectMediationResultButton.setManaged(false);
        rejectMediationResultButton.setMinWidth(90);

        HBox mediationResultActionsHBox = new HBox(10, acceptMediationResultButton, rejectMediationResultButton);
        mediationResultActionsHBox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(mediationResultActionsHBox, new Insets(4, 0, 0, 0));
        VBox mediationBannerContentVBox = new VBox(6, mediationBannerLabel, mediationResultActionsHBox);
        mediationBannerContentVBox.setFillWidth(true);
        HBox.setHgrow(mediationBannerContentVBox, Priority.ALWAYS);

        isInMediationHBox = new HBox(10,
                isInMediationIcon,
                mediationBannerContentVBox,
                tryAgainMenuItem);
        isInMediationHBox.setAlignment(Pos.TOP_LEFT);
        isInMediationHBox.setPadding(new Insets(10));
        isInMediationHBox.getStyleClass().add("bisq-easy-trade-isInMediation-bg");

        Label isInArbitrationIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        isInArbitrationIcon.getStyleClass().add("bisq-easy-trade-isInMediation-headline");

        arbitrationBannerLabel = new Label();
        arbitrationBannerLabel.getStyleClass().add("bisq-easy-trade-isInMediation-headline");
        arbitrationBannerLabel.setWrapText(true);
        arbitrationBannerLabel.setMaxWidth(Double.MAX_VALUE);
        arbitrationBannerLabel.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(arbitrationBannerLabel, Priority.ALWAYS);

        isInArbitrationHBox = new HBox(10, isInArbitrationIcon, arbitrationBannerLabel);
        isInArbitrationHBox.setAlignment(Pos.TOP_LEFT);
        isInArbitrationHBox.setPadding(new Insets(10));
        isInArbitrationHBox.getStyleClass().add("bisq-easy-trade-isInMediation-bg");

        exportButton = new Button(Res.get("muSig.trade.pending.exportTrade"));
        exportButton.setMinWidth(180);

        reportToMediatorButton = new Button(Res.get("muSig.trade.pending.reportToMediator"));
        reportToMediatorButton.getStyleClass().add("outlined-button");

        closeTradeButton = new Button(Res.get("muSig.trade.pending.closeTrade"));
        closeTradeButton.setMinWidth(160);
        closeTradeButton.setDefaultButton(true);

        Label errorIcon = Icons.getIcon(AwesomeIcon.WARNING_SIGN);
        errorIcon.getStyleClass().add("bisq-text-error");
        errorIcon.setMinWidth(16);
        errorMessage = new Label();
        errorMessage.getStyleClass().add("bisq-easy-trade-failed-headline");
        errorHBox = new HBox(10, errorIcon, errorMessage, Spacer.fillHBox(), reportToMediatorButton, exportButton, closeTradeButton);
        errorHBox.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(tradePhaseBox, Priority.ALWAYS);
        phaseAndInfoHBox = new HBox(tradePhaseBox);

        VBox.setMargin(isInMediationHBox, new Insets(20, 30, 0, 30));
        VBox.setMargin(isInArbitrationHBox, new Insets(20, 30, 0, 30));
        VBox.setMargin(errorHBox, new Insets(20, 30, 20, 30));
        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox content = new VBox(tradeDataHeaderBox, isInMediationHBox, isInArbitrationHBox, errorHBox, phaseAndInfoHBox);
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
        isInArbitrationHBox.visibleProperty().bind(model.getIsInArbitration());
        isInArbitrationHBox.managedProperty().bind(model.getIsInArbitration());
        errorHBox.visibleProperty().bind(model.getError());
        errorHBox.managedProperty().bind(model.getError());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoVisible());
        acceptMediationResultButton.disableProperty().bind(
                model.getMediationResultAcceptanceAvailable().not());
        rejectMediationResultButton.disableProperty().bind(
                model.getIsTradeCompleted());
        acceptMediationResultButton.visibleProperty().bind(model.getShowMediationResultDecisionButtons());
        acceptMediationResultButton.managedProperty().bind(model.getShowMediationResultDecisionButtons());
        rejectMediationResultButton.visibleProperty().bind(model.getShowMediationResultDecisionButtons());
        rejectMediationResultButton.managedProperty().bind(model.getShowMediationResultDecisionButtons());

        errorMessage.textProperty().bind(model.getErrorMessage());
        mediationBannerLabel.textProperty().bind(model.getMediationBannerText());
        arbitrationBannerLabel.textProperty().bind(model.getArbitrationBannerText());

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

        shouldShowTryRequestMediationAgainPin = EasyBind.subscribe(model.getShouldShowTryRequestMediationAgain(),
                showTryAgain -> {
                    tryAgainMenuItem.setVisible(showTryAgain);
                    tryAgainMenuItem.setManaged(showTryAgain);
                });

        tradeDetailsButton.setOnAction(e -> controller.onShowTradeDetails());
        closeTradeButton.setOnAction(e -> controller.onCloseTrade());
        exportButton.setOnAction(e -> controller.onExportTrade());
        reportToMediatorButton.setOnAction(e -> controller.onRequestMediation());
        tryAgainMenuItem.setOnAction(e -> controller.onResendMediationRequest());
        acceptMediationResultButton.setOnAction(e -> controller.onAcceptMediationResult());
        rejectMediationResultButton.setOnAction(e -> controller.onRejectMediationResult());
    }

    @Override
    protected void onViewDetached() {
        tradePhaseBox.visibleProperty().unbind();
        tradePhaseBox.managedProperty().unbind();
        tradeDataHeaderBox.visibleProperty().unbind();
        tradeDataHeaderBox.managedProperty().unbind();
        reportToMediatorButton.visibleProperty().unbind();
        reportToMediatorButton.managedProperty().unbind();
        isInMediationHBox.visibleProperty().unbind();
        isInMediationHBox.managedProperty().unbind();
        isInArbitrationHBox.visibleProperty().unbind();
        isInArbitrationHBox.managedProperty().unbind();
        errorHBox.visibleProperty().unbind();
        errorHBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();
        acceptMediationResultButton.disableProperty().unbind();
        rejectMediationResultButton.disableProperty().unbind();
        acceptMediationResultButton.visibleProperty().unbind();
        acceptMediationResultButton.managedProperty().unbind();
        rejectMediationResultButton.visibleProperty().unbind();
        rejectMediationResultButton.managedProperty().unbind();

        errorMessage.textProperty().unbind();
        mediationBannerLabel.textProperty().unbind();
        arbitrationBannerLabel.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
        shouldShowTryRequestMediationAgainPin.unsubscribe();

        tradeDetailsButton.setOnAction(null);
        closeTradeButton.setOnAction(null);
        exportButton.setOnAction(null);
        reportToMediatorButton.setOnAction(null);
        tryAgainMenuItem.setOnAction(null);
        acceptMediationResultButton.setOnAction(null);
        rejectMediationResultButton.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }
}
