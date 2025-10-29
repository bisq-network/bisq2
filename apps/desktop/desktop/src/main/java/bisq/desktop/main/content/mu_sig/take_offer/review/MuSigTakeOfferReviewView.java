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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.mu_sig.components.MuSigProtocolWaitingAnimation;
import bisq.desktop.main.content.mu_sig.components.MuSigProtocolWaitingState;
import bisq.desktop.main.content.mu_sig.take_offer.MuSigTakeOfferView;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class MuSigTakeOfferReviewView extends View<StackPane, MuSigTakeOfferReviewModel, MuSigTakeOfferReviewController> {
    public static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    public static final String VALUE_STYLE = "trade-wizard-review-value";
    public static final String DETAILS_STYLE = "trade-wizard-review-details";
    private final static int FEEDBACK_WIDTH = 700;

    private final VBox takeOfferStatus, sendTakeOfferMessageFeedback, takeOfferSuccess;
    private final Button takeOfferSuccessButton;
    private final Label priceDetails,
            paymentMethod, paymentMethodDetails,
            fee, feeDetails;
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
        takeOfferStatus = new VBox();
        takeOfferStatus.setVisible(false);

        sendTakeOfferMessageFeedback = new VBox(20);
        takeOfferSendMessageWaitingAnimation = new MuSigProtocolWaitingAnimation(MuSigProtocolWaitingState.TAKE_OFFER);
        configSendTakeOfferMessageFeedback();

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOfferSuccessButton"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(gridPane, new Insets(40));
        StackPane.setMargin(takeOfferStatus, new Insets(-MuSigTakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(gridPane, takeOfferStatus);
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
            takeOfferStatus.getChildren().setAll(sendTakeOfferMessageFeedback, Spacer.fillVBox());
            takeOfferStatus.setVisible(true);

            Transitions.blurStrong(gridPane, 0);
            Transitions.slideInTop(takeOfferStatus, 450);
            takeOfferSendMessageWaitingAnimation.playIndefinitely();

            UIScheduler.run(() -> {
                minWaitingTimePassed = true;
                if (model.getTakeOfferStatus().get() == MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS) {
                    takeOfferStatus.getChildren().setAll(takeOfferSuccess, Spacer.fillVBox());
                    takeOfferSendMessageWaitingAnimation.stop();
                }
            }).after(DevMode.isDevMode() ? 500 : 4000);
        } else if (status == MuSigTakeOfferReviewModel.TakeOfferStatus.SUCCESS && minWaitingTimePassed) {
            takeOfferStatus.getChildren().setAll(takeOfferSuccess, Spacer.fillVBox());
            takeOfferSendMessageWaitingAnimation.stop();
        } else if (status == MuSigTakeOfferReviewModel.TakeOfferStatus.NOT_STARTED) {
            takeOfferStatus.getChildren().clear();
            takeOfferStatus.setVisible(false);
            Transitions.removeEffect(gridPane);
        }
    }

    private void configSendTakeOfferMessageFeedback() {
        VBox contentBox = getFeedbackContentBox();

        sendTakeOfferMessageFeedback.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline"));
        headlineLabel.getStyleClass().add("trade-wizard-take-offer-send-message-headline");
        HBox title = new HBox(10, takeOfferSendMessageWaitingAnimation, headlineLabel);
        title.setAlignment(Pos.CENTER);

        WrappingText subtitleLabel = new WrappingText(Res.get("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle"),
                "trade-wizard-take-offer-send-message-sub-headline");
        WrappingText info = new WrappingText(Res.get("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info"),
                "trade-wizard-take-offer-send-message-info");
        VBox subtitle = new VBox(10, subtitleLabel, info);
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        info.setTextAlignment(TextAlignment.CENTER);
        subtitle.setAlignment(Pos.CENTER);

        contentBox.getChildren().addAll(title, subtitle);
        sendTakeOfferMessageFeedback.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("bisqEasy.takeOffer.review.takeOfferSuccess.headline"));
        headlineLabel.getStyleClass().add("trade-wizard-take-offer-send-message-headline");

        Label subtitleLabel = new Label(Res.get("bisqEasy.takeOffer.review.takeOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        takeOfferSuccessButton.setDefaultButton(true);
        VBox.setMargin(takeOfferSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headlineLabel, subtitleLabel, takeOfferSuccessButton);
        takeOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(FEEDBACK_WIDTH);
        return contentBox;
    }

    private void configFeedbackSubtitleLabel(Label subtitleLabel) {
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 150);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("trade-wizard-take-offer-send-message-sub-headline");
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
