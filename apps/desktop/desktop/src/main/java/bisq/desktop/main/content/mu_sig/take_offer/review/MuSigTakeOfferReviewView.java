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

package bisq.desktop.main.content.mu_sig.take_offer.review;

import bisq.common.application.DevMode;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.main.content.mu_sig.components.MuSigProtocolWaitingAnimation;
import bisq.desktop.main.content.mu_sig.components.MuSigProtocolWaitingState;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class MuSigTakeOfferReviewView extends View<StackPane, MuSigTakeOfferReviewModel, MuSigTakeOfferReviewController> {
    public static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    public static final String VALUE_STYLE = "trade-wizard-review-value";
    public static final String DETAILS_STYLE = "trade-wizard-review-details";

    private final WizardOverlay sendTakeOfferMessageOverlay, takeOfferSuccessOverlay;
    private final Button takeOfferSuccessButton;
    private final Label priceDetails, paymentMethod, paymentMethodDetails, securityDeposit, securityDepositDetails, fee, feeDetails;
    private final GridPane gridPane;
    private final TextFlow price;
    private final MuSigProtocolWaitingAnimation takeOfferSendMessageWaitingAnimation;
    private Subscription takeOfferStatusPin;
    private boolean minWaitingTimePassed = false;

    MuSigTakeOfferReviewView(MuSigTakeOfferReviewModel model,
                             MuSigTakeOfferReviewController controller,
                             HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setMouseTransparent(true);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);
        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        int rowIndex = 0;
        Label headline = new Label(Res.get("bisqEasy.takeOffer.review.headline"));
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(10, 0, 30, 0));
        gridPane.add(headline, 0, rowIndex, 4, 1);

        rowIndex++;
        Region line1 = getLine();
        gridPane.add(line1, 0, rowIndex, 4, 1);

        rowIndex++;
        GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 10, 0));
        gridPane.add(reviewDataDisplay, 0, rowIndex, 4, 1);

        rowIndex++;
        Label detailsHeadline = new Label(Res.get("bisqEasy.takeOffer.review.detailsHeadline").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, rowIndex, 4, 1);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        gridPane.add(line2, 0, rowIndex, 4, 1);

        rowIndex++;
        Label priceDescription = new Label(Res.get("bisqEasy.takeOffer.review.price.price"));
        priceDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(VALUE_STYLE);
        gridPane.add(price, 1, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(priceDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        Label paymentMethodDescription = new Label(Res.get("muSig.takeOffer.review.paymentMethod.description"));
        paymentMethodDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(paymentMethodDescription, 0, rowIndex);

        paymentMethod = new Label();
        paymentMethod.getStyleClass().add(VALUE_STYLE);
        gridPane.add(paymentMethod, 1, rowIndex);

        paymentMethodDetails = new Label();
        paymentMethodDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(paymentMethodDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        Label securityDepositDescription = new Label(Res.get("muSig.tradeWizard.review.securityDeposit.description"));
        securityDepositDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(securityDepositDescription, 0, rowIndex);

        securityDeposit = new Label();
        securityDeposit.getStyleClass().add(VALUE_STYLE);
        gridPane.add(securityDeposit, 1, rowIndex);

        securityDepositDetails = new Label();
        securityDepositDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(securityDepositDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.tradeWizard.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(feeInfoDescription, 0, rowIndex);

        fee = new Label();
        fee.getStyleClass().add(VALUE_STYLE);
        gridPane.add(fee, 1, rowIndex);

        feeDetails = new Label();
        feeDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(feeDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        Region line3 = getLine();
        gridPane.add(line3, 0, rowIndex, 4, 1);

        // Feedback overlay
        takeOfferSendMessageWaitingAnimation = new MuSigProtocolWaitingAnimation(MuSigProtocolWaitingState.TAKE_OFFER);
        sendTakeOfferMessageOverlay = new WizardOverlay(root)
                .headlineIcon(takeOfferSendMessageWaitingAnimation)
                .headline("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline")
                .description("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle",
                        "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info")
                .build();

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOfferSuccessButton"));
        takeOfferSuccessButton.setDefaultButton(true);
        takeOfferSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("bisqEasy.takeOffer.review.takeOfferSuccess.headline")
                .description("bisqEasy.takeOffer.review.takeOfferSuccess.subTitle")
                .buttons(takeOfferSuccessButton)
                .build();

        StackPane.setMargin(gridPane, new Insets(40));
        root.getChildren().addAll(gridPane, sendTakeOfferMessageOverlay, takeOfferSuccessOverlay);
    }

    @Override
    protected void onViewAttached() {
        TextFlowUtils.updateTextFlow(price, model.getPriceWithCode());
        priceDetails.setText(model.getPriceDetails());

        paymentMethod.setText(model.getPaymentMethodDisplayString());
        String paymentMethodDetailsValue = model.getPaymentMethodDetails();
        paymentMethodDetails.setText(paymentMethodDetailsValue);
        if (paymentMethodDetailsValue.length() > 50) {
            paymentMethodDetails.setTooltip(new BisqTooltip(paymentMethodDetailsValue));
        }

        securityDeposit.setText(model.getSecurityDepositAsPercent());
        securityDepositDetails.setText(model.getSecurityDepositAsBtc());

        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        takeOfferStatusPin = EasyBind.subscribe(model.getTakeOfferStatus(), this::showTakeOfferStatusFeedback);
    }

    @Override
    protected void onViewDetached() {
        paymentMethodDetails.setTooltip(null);
        takeOfferSuccessButton.setOnAction(null);
        takeOfferStatusPin.unsubscribe();
        takeOfferSendMessageWaitingAnimation.stop();
    }

    private void showTakeOfferStatusFeedback(MuSigTakeOfferReviewModel.TakeOfferStatus status) {
        if (status == MuSigTakeOfferReviewModel.TakeOfferStatus.SENT) {
            sendTakeOfferMessageOverlay.setVisible(true);

            Transitions.blurStrong(gridPane, 0);
            Transitions.slideInTop(sendTakeOfferMessageOverlay, 450);
            takeOfferSendMessageWaitingAnimation.playIndefinitely();

            UIScheduler.run(() -> {
                minWaitingTimePassed = true;
                if (model.getTakeOfferStatus().get() == MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS) {
                    sendTakeOfferMessageOverlay.setVisible(false);
                    takeOfferSuccessOverlay.setVisible(true);
                    takeOfferSendMessageWaitingAnimation.stop();
                }
            }).after(DevMode.isDevMode() ? 500 : 4000);
        } else if (status == MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS && minWaitingTimePassed) {
            sendTakeOfferMessageOverlay.setVisible(false);
            takeOfferSuccessOverlay.setVisible(true);
            takeOfferSendMessageWaitingAnimation.stop();
        } else if (status == MuSigTakeOfferReviewModel.TakeOfferStatus.NOT_STARTED) {
            sendTakeOfferMessageOverlay.setVisible(false);
            Transitions.removeEffect(gridPane);
        }
    }

    private Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
