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
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.common.monetary.Coin;
import bisq.common.observable.Pin;
import bisq.presentation.formatters.AmountFormatter;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.MuSigTrade;
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
    private final HBox phaseAndInfoHBox, errorHBox, isInMediationHBox;
    private final Button closeTradeButton, exportButton, reportToMediatorButton,
            tradeDetailsButton, acceptMediationResultButton, rejectMediationResultButton;
    private final Label errorMessage, mediationBannerLabel;
    private final VBox tradePhaseBox, tradeDataHeaderBox;
    private final BisqMenuItem tryAgainMenuItem;
    private Pin disputeStatePin;
    private Subscription stateInfoVBoxPin, requestMediationDeliveryStatusPin,
            shouldShowTryRequestMediationAgainPin, tradePin, mediationResultAcceptedPin;

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
        HBox.setHgrow(mediationBannerContentVBox, Priority.ALWAYS);

        isInMediationHBox = new HBox(10,
                isInMediationIcon,
                mediationBannerContentVBox,
                tryAgainMenuItem);
        isInMediationHBox.setAlignment(Pos.TOP_LEFT);
        isInMediationHBox.setPadding(new Insets(10));
        isInMediationHBox.getStyleClass().add("bisq-easy-trade-isInMediation-bg");

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
        VBox.setMargin(errorHBox, new Insets(20, 30, 20, 30));
        VBox.setMargin(phaseAndInfoHBox, new Insets(0, 30, 15, 30));
        VBox content = new VBox(tradeDataHeaderBox, isInMediationHBox, errorHBox, phaseAndInfoHBox);
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

        shouldShowTryRequestMediationAgainPin = EasyBind.subscribe(model.getShouldShowTryRequestMediationAgain(),
                showTryAgain -> {
                    tryAgainMenuItem.setVisible(showTryAgain);
                    tryAgainMenuItem.setManaged(showTryAgain);
                });

        requestMediationDeliveryStatusPin = EasyBind.subscribe(model.getRequestMediationDeliveryStatus(),
                this::updateMediationBannerLabel);
        mediationResultAcceptedPin = EasyBind.subscribe(model.getMyMediationResultAccepted(),
                update -> updateMediationBannerLabel(model.getRequestMediationDeliveryStatus().get()));
        tradePin = EasyBind.subscribe(model.getTrade(), trade -> {
            if (disputeStatePin != null) {
                disputeStatePin.unbind();
                disputeStatePin = null;
            }
            if (trade != null) {
                disputeStatePin = trade.disputeStateObservable().addObserver(
                        disputeState -> UIThread.run(() ->
                                updateMediationBannerLabel(model.getRequestMediationDeliveryStatus().get())));
            } else {
                updateMediationBannerLabel(model.getRequestMediationDeliveryStatus().get());
            }
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
        errorHBox.visibleProperty().unbind();
        errorHBox.managedProperty().unbind();
        phaseAndInfoHBox.visibleProperty().unbind();
        phaseAndInfoHBox.managedProperty().unbind();

        errorMessage.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
        requestMediationDeliveryStatusPin.unsubscribe();
        shouldShowTryRequestMediationAgainPin.unsubscribe();
        tradePin.unsubscribe();
        mediationResultAcceptedPin.unsubscribe();
        if (disputeStatePin != null) {
            disputeStatePin.unbind();
            disputeStatePin = null;
        }

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

    private static String getMessageDeliveryStatusDisplayString(MessageDeliveryStatus status) {
        return switch (status) {
            case CONNECTING -> Res.get("muSig.trade.requestMediation.deliveryState.CONNECTING");
            case SENT -> Res.get("muSig.trade.requestMediation.deliveryState.SENT");
            case ACK_RECEIVED -> Res.get("muSig.trade.requestMediation.deliveryState.ACK_RECEIVED");
            case TRY_ADD_TO_MAILBOX -> Res.get("muSig.trade.requestMediation.deliveryState.TRY_ADD_TO_MAILBOX");
            case ADDED_TO_MAILBOX -> Res.get("muSig.trade.requestMediation.deliveryState.ADDED_TO_MAILBOX");
            case MAILBOX_MSG_RECEIVED -> Res.get("muSig.trade.requestMediation.deliveryState.MAILBOX_MSG_RECEIVED");
            case FAILED -> Res.get("muSig.trade.requestMediation.deliveryState.FAILED");
        };
    }

    private void updateMediationBannerLabel(MessageDeliveryStatus status) {
        MuSigTrade trade = model.getTrade().get();
        updateMediationResultDecisionControls(trade);
        if (trade != null) {
            MuSigDisputeState disputeState = trade.getDisputeState();
            if (disputeState == MuSigDisputeState.MEDIATION_CLOSED) {
                String details = trade.getMuSigMediationResult()
                        .map(result -> getMediationResultDetailsText(trade, result))
                        .orElse(Res.get("data.na"));
                String text = Res.get("muSig.trade.pending.inMediation.closed", details);
                mediationBannerLabel.setText(text);
                return;
            } else if (disputeState == MuSigDisputeState.MEDIATION_RE_OPENED) {
                String details = trade.getMuSigMediationResult()
                        .map(result -> getMediationResultDetailsText(trade, result))
                        .orElse(Res.get("data.na"));
                String text = Res.get("muSig.trade.pending.inMediation.reOpened", details);
                mediationBannerLabel.setText(text);
                return;
            } else if (disputeState == MuSigDisputeState.MEDIATION_OPEN) {
                mediationBannerLabel.setText(Res.get("muSig.trade.pending.inMediation.info"));
                return;
            } else if (disputeState != MuSigDisputeState.MEDIATION_REQUESTED) {
                mediationBannerLabel.setText(Res.get("muSig.trade.pending.inMediation.info"));
                return;
            }
        }

        // In MEDIATION_REQUESTED we reflect transport status of the request message.
        // If the peer had sent the request we do not get any requestMediationDeliveryStatus; status is null.
        if (status == null || status == MessageDeliveryStatus.ACK_RECEIVED || status == MessageDeliveryStatus.MAILBOX_MSG_RECEIVED) {
            mediationBannerLabel.setText(Res.get("muSig.trade.pending.inMediation.requested"));
        } else {
            String deliveryStatus = getMessageDeliveryStatusDisplayString(status);
            if (status == MessageDeliveryStatus.FAILED) {
                String resendRequest = model.getShouldShowTryRequestMediationAgain().get()
                        ? " " + Res.get("muSig.trade.requestMediation.resendRequest")
                        : "";
                mediationBannerLabel.setText(deliveryStatus + resendRequest);
            } else {
                mediationBannerLabel.setText(Res.get("muSig.trade.pending.inMediation.requestSent", deliveryStatus));
            }
        }
    }

    private void updateMediationResultDecisionControls(MuSigTrade trade) {
        boolean showDecisionControls = false;
        if (trade != null) {
            MuSigDisputeState disputeState = trade.getDisputeState();
            showDecisionControls = disputeState == MuSigDisputeState.MEDIATION_CLOSED ||
                    disputeState == MuSigDisputeState.MEDIATION_RE_OPENED;
        }
        boolean myDecisionKnown = model.getMyMediationResultAccepted().get().isPresent();

        boolean showDecisionButtons = showDecisionControls && !myDecisionKnown;
        acceptMediationResultButton.setVisible(showDecisionButtons);
        acceptMediationResultButton.setManaged(showDecisionButtons);
        rejectMediationResultButton.setVisible(showDecisionButtons);
        rejectMediationResultButton.setManaged(showDecisionButtons);
        acceptMediationResultButton.setDisable(false);
        rejectMediationResultButton.setDisable(false);
    }

    private static String getMediationResultDetailsText(MuSigTrade trade, MuSigMediationResult result) {
        if (result.getProposedBuyerPayoutAmount().isEmpty() || result.getProposedSellerPayoutAmount().isEmpty()) {
            if (result.getMediationPayoutDistributionType() == MediationPayoutDistributionType.NO_PAYOUT) {
                return Res.get("muSig.trade.pending.inMediation.resultDetails.noPayout");
            }
            return Res.get("muSig.trade.pending.inMediation.resultDetails", Res.get("data.na"), Res.get("data.na"));
        }

        String myPayoutAmount = trade.isBuyer()
                ? AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedBuyerPayoutAmount().orElseThrow()))
                : AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedSellerPayoutAmount().orElseThrow()));
        String peerPayoutAmount = trade.isBuyer()
                ? AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedSellerPayoutAmount().orElseThrow()))
                : AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(result.getProposedBuyerPayoutAmount().orElseThrow()));
        return Res.get("muSig.trade.pending.inMediation.resultDetails",
                myPayoutAmount,
                peerPayoutAmount);
    }
}
