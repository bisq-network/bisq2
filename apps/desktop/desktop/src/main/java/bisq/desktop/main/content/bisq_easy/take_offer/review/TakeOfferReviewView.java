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

package bisq.desktop.main.content.bisq_easy.take_offer.review;

import bisq.common.application.DevMode;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
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
class TakeOfferReviewView extends View<StackPane, TakeOfferReviewModel, TakeOfferReviewController> {
    public static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    public static final String VALUE_STYLE = "trade-wizard-review-value";
    public static final String DETAILS_STYLE = "trade-wizard-review-details";

    private final Button takeOfferSuccessButton;
    private final Label priceDetails, bitcoinPaymentMethod, fiatPaymentMethod, fee, feeDetails;
    private final GridPane content;
    private final TextFlow price;
    private final WaitingAnimation takeOfferSendMessageWaitingAnimation;
    private final WizardOverlay takeOfferSuccessOverlay, sendTakeOfferMessageOverlay;
    private Subscription takeOfferStatusPin;
    private boolean minWaitingTimePassed = false;

    TakeOfferReviewView(TakeOfferReviewModel model, TakeOfferReviewController controller, HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        content.setMouseTransparent(true);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);
        content.getColumnConstraints().addAll(col1, col2, col3, col4);

        int rowIndex = 0;
        Label headline = new Label(Res.get("bisqEasy.takeOffer.review.headline"));
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(10, 0, 30, 0));
        GridPane.setColumnSpan(headline, 4);
        content.add(headline, 0, rowIndex);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setColumnSpan(line1, 4);
        content.add(line1, 0, rowIndex);

        rowIndex++;
        GridPane.setColumnSpan(reviewDataDisplay, 4);
        GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 10, 0));
        content.add(reviewDataDisplay, 0, rowIndex);

        rowIndex++;
        Label detailsHeadline = new Label(Res.get("bisqEasy.takeOffer.review.detailsHeadline").toUpperCase());
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setColumnSpan(detailsHeadline, 4);
        content.add(detailsHeadline, 0, rowIndex);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        GridPane.setColumnSpan(line2, 4);
        content.add(line2, 0, rowIndex);

        rowIndex++;
        Label priceDescription = new Label(Res.get("bisqEasy.takeOffer.review.price.price"));
        priceDescription.getStyleClass().add(DESCRIPTION_STYLE);
        content.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(VALUE_STYLE);
        content.add(price, 1, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(DETAILS_STYLE);
        GridPane.setColumnSpan(priceDetails, 2);
        content.add(priceDetails, 2, rowIndex);

        rowIndex++;
        Label bitcoinPaymentMethodDescription = new Label(Res.get("bisqEasy.takeOffer.review.method.bitcoin"));
        bitcoinPaymentMethodDescription.getStyleClass().add(DESCRIPTION_STYLE);
        content.add(bitcoinPaymentMethodDescription, 0, rowIndex);

        bitcoinPaymentMethod = new Label();
        bitcoinPaymentMethod.getStyleClass().add(VALUE_STYLE);
        content.add(bitcoinPaymentMethod, 1, rowIndex);

        rowIndex++;
        Label fiatPaymentMethodDescription = new Label(Res.get("bisqEasy.takeOffer.review.method.fiat"));
        fiatPaymentMethodDescription.getStyleClass().add(DESCRIPTION_STYLE);
        content.add(fiatPaymentMethodDescription, 0, rowIndex);

        fiatPaymentMethod = new Label();
        fiatPaymentMethod.getStyleClass().add(VALUE_STYLE);
        content.add(fiatPaymentMethod, 1, rowIndex);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.tradeWizard.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(DESCRIPTION_STYLE);
        content.add(feeInfoDescription, 0, rowIndex);

        fee = new Label();
        fee.getStyleClass().add(VALUE_STYLE);
        content.add(fee, 1, rowIndex);

        feeDetails = new Label();
        feeDetails.getStyleClass().add(DETAILS_STYLE);
        GridPane.setColumnSpan(feeDetails, 2);
        content.add(feeDetails, 2, rowIndex);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setColumnSpan(line3, 4);
        content.add(line3, 0, rowIndex);

        // Overlays
        takeOfferSendMessageWaitingAnimation = new WaitingAnimation(WaitingState.TAKE_BISQ_EASY_OFFER);
        sendTakeOfferMessageOverlay = new WizardOverlay(root)
                .headlineIcon(takeOfferSendMessageWaitingAnimation)
                .headline("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline")
                .descriptionFromI18nKeys("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle",
                        "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info")
                .build();

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOfferSuccessButton"));
        takeOfferSuccessButton.setDefaultButton(true);
        takeOfferSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("bisqEasy.takeOffer.review.takeOfferSuccess.headline")
                .descriptionFromI18nKey("bisqEasy.takeOffer.review.takeOfferSuccess.subTitle")
                .buttons(takeOfferSuccessButton)
                .build();

        StackPane.setMargin(content, new Insets(40));
        root.getChildren().addAll(content, sendTakeOfferMessageOverlay, takeOfferSuccessOverlay);
    }

    @Override
    protected void onViewAttached() {
        TextFlowUtils.updateTextFlow(price, model.getPriceWithCode());
        priceDetails.setText(model.getPriceDetails());

        fiatPaymentMethod.setText(model.getFiatPaymentMethod());
        bitcoinPaymentMethod.setText(model.getBitcoinPaymentMethod());

        feeDetails.setVisible(model.isFeeDetailsVisible());
        feeDetails.setManaged(model.isFeeDetailsVisible());

        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        takeOfferStatusPin = EasyBind.subscribe(model.getTakeOfferStatus(), this::showTakeOfferStatusFeedback);
    }


    @Override
    protected void onViewDetached() {
        takeOfferSuccessButton.setOnAction(null);
        takeOfferStatusPin.unsubscribe();
        takeOfferSendMessageWaitingAnimation.stop();
    }

    private void showTakeOfferStatusFeedback(TakeOfferReviewModel.TakeOfferStatus status) {
        if (status == TakeOfferReviewModel.TakeOfferStatus.SENT) {
            sendTakeOfferMessageOverlay.setVisible(true);

            Transitions.blurStrong(content, 0);
            Transitions.slideInTop(sendTakeOfferMessageOverlay, 450);
            takeOfferSendMessageWaitingAnimation.playIndefinitely();

            UIScheduler.run(() -> {
                minWaitingTimePassed = true;
                if (model.getTakeOfferStatus().get() == TakeOfferReviewModel.TakeOfferStatus.SUCCESS) {
                    sendTakeOfferMessageOverlay.setVisible(false);
                    takeOfferSuccessOverlay.setVisible(true);
                    takeOfferSendMessageWaitingAnimation.stop();
                }
            }).after(DevMode.isDevMode() ? 500 : 4000);
        } else if (status == TakeOfferReviewModel.TakeOfferStatus.SUCCESS && minWaitingTimePassed) {
            sendTakeOfferMessageOverlay.setVisible(false);
            takeOfferSuccessOverlay.setVisible(true);
            takeOfferSendMessageWaitingAnimation.stop();
        } else if (status == TakeOfferReviewModel.TakeOfferStatus.NOT_STARTED) {
            sendTakeOfferMessageOverlay.setVisible(false);
            Transitions.removeEffect(content);
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
