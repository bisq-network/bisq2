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
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeStateView extends View<VBox, TradeStateModel, TradeStateController> {
    private final HBox phaseAndInfoHBox, cancelledHBox, interruptedHBox, errorHBox, isInMediationHBox;
    private final Button interruptTradeButton, closeTradeButton, exportButton, reportToMediatorButton, acceptSellersPriceButton,
            rejectPriceButton, tradeDetailsButton;
    private final Label cancelledInfo, errorMessage, buyerPriceDescriptionApprovalOverlay,
            sellerPriceDescriptionApprovalOverlay, mediationBannerLabel;
    private final VBox tradePhaseBox, tradeDataHeaderBox, sellerPriceApprovalOverlay;
    private final Pane sellerPriceApprovalContent;
    private final BisqMenuItem tryAgainMenuItem;
    private Subscription stateInfoVBoxPin, showSellersPriceApprovalOverlayPin, requestMediationDeliveryStatusPin,
            shouldShowTryRequestMediationAgainPin;

    public TradeStateView(TradeStateModel model,
                          TradeStateController controller,
                          VBox tradePhaseBox,
                          HBox tradeDataHeader) {
        super(new VBox(0), model, controller);

        this.tradePhaseBox = tradePhaseBox;
        interruptTradeButton = new Button();
        interruptTradeButton.setMinWidth(160);
        interruptTradeButton.getStyleClass().add("outlined-button");

        tradeDetailsButton = new Button(Res.get("bisqEasy.openTrades.tradeDetails.button"));
        tradeDetailsButton.getStyleClass().addAll("grey-transparent-outlined-button");
        tradeDetailsButton.setMinWidth(160);

        HBox.setMargin(tradeDetailsButton, new Insets(0, -20, 0, 0));
        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), tradeDetailsButton, interruptTradeButton);
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
        VBox content = new VBox(tradeDataHeaderBox, isInMediationHBox, interruptedHBox, phaseAndInfoHBox);
        content.getStyleClass().add("bisq-easy-container");

        // Accept seller's price overlay
        buyerPriceDescriptionApprovalOverlay = new Label();
        sellerPriceDescriptionApprovalOverlay = new Label();
        VBox priceDescriptionBox = new VBox(buyerPriceDescriptionApprovalOverlay, sellerPriceDescriptionApprovalOverlay);
        priceDescriptionBox.getStyleClass().setAll("seller-price-approval-content");
        sellerPriceApprovalContent = new Pane(priceDescriptionBox);
        acceptSellersPriceButton = new Button(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.button.accept"));
        acceptSellersPriceButton.getStyleClass().add("outlined-button");
        rejectPriceButton = new Button();
        rejectPriceButton.setMinWidth(160);
        rejectPriceButton.setDefaultButton(true);
        sellerPriceApprovalOverlay = createAndGetSellerPriceApprovalOverlay();

        StackPane layeredContent = new StackPane(content, sellerPriceApprovalOverlay);
        root.getChildren().add(layeredContent);
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
        cancelledHBox.visibleProperty().bind(model.getInterruptedTradeInfo());
        cancelledHBox.managedProperty().bind(model.getInterruptedTradeInfo());
        interruptedHBox.visibleProperty().bind(model.getInterruptedTradeInfo().or(model.getError()));
        interruptedHBox.managedProperty().bind(model.getInterruptedTradeInfo().or(model.getError()));
        errorHBox.visibleProperty().bind(model.getError());
        errorHBox.managedProperty().bind(model.getError());
        phaseAndInfoHBox.visibleProperty().bind(model.getPhaseAndInfoVisible());
        phaseAndInfoHBox.managedProperty().bind(model.getPhaseAndInfoVisible());

        cancelledInfo.textProperty().bind(model.getTradeInterruptedInfo());
        errorMessage.textProperty().bind(model.getErrorMessage());

        interruptTradeButton.textProperty().bind(model.getInterruptTradeButtonText());
        interruptTradeButton.visibleProperty().bind(model.getInterruptTradeButtonVisible().and(model.getShouldShowSellerPriceApprovalOverlay().not()));
        interruptTradeButton.managedProperty().bind(model.getInterruptTradeButtonVisible().and(model.getShouldShowSellerPriceApprovalOverlay().not()));

        rejectPriceButton.textProperty().bind(model.getInterruptTradeButtonText());
        buyerPriceDescriptionApprovalOverlay.textProperty().bind(model.getBuyerPriceDescriptionApprovalOverlay());
        sellerPriceDescriptionApprovalOverlay.textProperty().bind(model.getSellerPriceDescriptionApprovalOverlay());

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

        interruptTradeButton.setOnAction(e -> controller.onInterruptTrade());
        tradeDetailsButton.setOnAction(e -> controller.onViewTradeDetails());
        closeTradeButton.setOnAction(e -> controller.onCloseTrade());
        exportButton.setOnAction(e -> controller.onExportTrade());
        rejectPriceButton.setOnAction(e -> controller.onRejectPrice());
        reportToMediatorButton.setOnAction(e -> controller.onRequestMediation());
        acceptSellersPriceButton.setOnAction(e -> controller.onAcceptSellersPriceButton());
        tryAgainMenuItem.setOnAction(e -> controller.onResendMediationRequest());
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

        interruptTradeButton.textProperty().unbind();
        interruptTradeButton.visibleProperty().unbind();
        interruptTradeButton.managedProperty().unbind();

        rejectPriceButton.textProperty().unbind();
        buyerPriceDescriptionApprovalOverlay.textProperty().unbind();
        sellerPriceDescriptionApprovalOverlay.textProperty().unbind();

        stateInfoVBoxPin.unsubscribe();
        showSellersPriceApprovalOverlayPin.unsubscribe();
        requestMediationDeliveryStatusPin.unsubscribe();
        shouldShowTryRequestMediationAgainPin.unsubscribe();

        interruptTradeButton.setOnAction(null);
        tradeDetailsButton.setOnAction(null);
        closeTradeButton.setOnAction(null);
        exportButton.setOnAction(null);
        rejectPriceButton.setOnAction(null);
        reportToMediatorButton.setOnAction(null);
        acceptSellersPriceButton.setOnAction(null);
        tryAgainMenuItem.setOnAction(null);

        if (phaseAndInfoHBox.getChildren().size() == 2) {
            phaseAndInfoHBox.getChildren().remove(1);
        }
    }

    private VBox createAndGetSellerPriceApprovalOverlay() {
        VBox overlay = new VBox(10);
        overlay.setAlignment(Pos.TOP_LEFT);
        overlay.getStyleClass().addAll("trade-wizard-feedback-bg", "seller-price-approval-popup");
        overlay.visibleProperty().set(false);
        overlay.managedProperty().set(false);

        Label titleLabel = new Label(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.title"));
        titleLabel.getStyleClass().addAll("seller-price-approval-title", "large-text", "font-default");

        sellerPriceApprovalContent.getStyleClass().addAll("seller-price-approval-description", "normal-text", "font-default");

        Label questionLabel = new Label(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.question"));
        questionLabel.getStyleClass().addAll("seller-price-approval-description", "normal-text",
                "font-default", "seller-price-approval-question");

        Label disclaimerLabel = new Label(Res.get("bisqEasy.tradeState.acceptOrRejectSellersPrice.description.disclaimer"));
        disclaimerLabel.getStyleClass().addAll("seller-price-approval-description", "small-text", "font-default",
                "seller-price-approval-disclaimer");

        HBox acceptOrRejectButtons = new HBox(10, acceptSellersPriceButton, rejectPriceButton);
        acceptOrRejectButtons.setAlignment(Pos.BOTTOM_RIGHT);

        overlay.getChildren().addAll(titleLabel, sellerPriceApprovalContent,
                questionLabel, disclaimerLabel, Spacer.fillVBox(), acceptOrRejectButtons);
        StackPane.setAlignment(overlay, Pos.TOP_CENTER);
        StackPane.setMargin(overlay, new Insets(63, 0, 0, 0));
        return overlay;
    }
}
