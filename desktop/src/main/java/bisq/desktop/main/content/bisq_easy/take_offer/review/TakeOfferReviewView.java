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

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MultiStyleLabelPane;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferView;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class TakeOfferReviewView extends View<StackPane, TakeOfferReviewModel, TakeOfferReviewController> {
    private final static int FEEDBACK_WIDTH = 700;

    private final VBox takeOfferSuccess;
    private final Button takeOfferSuccessButton;
    private final Label amounts, toPay, toReceive, method, sellersPrice, sellersPriceDetails, fee;
    private final GridPane content;
    private final MultiStyleLabelPane directionHeadline;
    private Subscription showTakeOfferSuccessPin;

    TakeOfferReviewView(TakeOfferReviewModel model, TakeOfferReviewController controller) {
        super(new StackPane(), model, controller);

        content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);
        content.getColumnConstraints().addAll(col1, col2, col3, col4);

        String descriptionStyle = "trade-wizard-review-description";
        String valueStyle = "trade-wizard-review-value";
        String valueDetailsStyle = "trade-wizard-review-details";

        int rowIndex = 0;
        Label headline = new Label(Res.get("bisqEasy.takeOffer.review.headline"));
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        // GridPane.setMargin(headline, new Insets(-TakeOfferView.TOP_PANE_HEIGHT + 5, 20, 20, 0));
        GridPane.setMargin(headline, new Insets(0, 20, 10, 0));
        GridPane.setRowIndex(headline, rowIndex);
        GridPane.setColumnIndex(headline, 1);
        GridPane.setColumnSpan(headline, 2);
        content.getChildren().add(headline);
        content.setMouseTransparent(true);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setRowIndex(line1, rowIndex);
        GridPane.setColumnSpan(line1, 4);
        content.getChildren().add(line1);

        rowIndex++;
        directionHeadline = new MultiStyleLabelPane();
        directionHeadline.getStyleClass().add("trade-wizard-review-direction");
        GridPane.setMargin(directionHeadline, new Insets(16, 0, 10, 0));
        GridPane.setColumnSpan(directionHeadline, 4);
        content.add(directionHeadline, 0, rowIndex);

        rowIndex++;
        amounts = new Label();
        amounts.getStyleClass().add("trade-wizard-review-fix-amounts");
        GridPane.setMargin(amounts, new Insets(-7, 0, 17, 0));
        GridPane.setRowIndex(amounts, rowIndex);
        GridPane.setColumnSpan(amounts, 4);
        content.getChildren().add(amounts);

        rowIndex++;
        Label gridPaneHeadline = new Label(Res.get("bisqEasy.takeOffer.review.gridPaneHeadline").toUpperCase());
        gridPaneHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(gridPaneHeadline, new Insets(0, 0, -2, 0));
        GridPane.setRowIndex(gridPaneHeadline, rowIndex);
        GridPane.setColumnSpan(gridPaneHeadline, 4);
        content.getChildren().add(gridPaneHeadline);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(0, 0, 3, 0));
        GridPane.setRowIndex(line2, rowIndex);
        GridPane.setColumnSpan(line2, 4);
        content.getChildren().add(line2);

        rowIndex++;
        Label toPayDescription = new Label(Res.get("bisqEasy.takeOffer.review.toPay"));
        toPayDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(toPayDescription, rowIndex);
        GridPane.setColumnIndex(toPayDescription, 0);
        content.getChildren().add(toPayDescription);

        toPay = new Label();
        toPay.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(toPay, rowIndex);
        GridPane.setColumnIndex(toPay, 1);
        content.getChildren().add(toPay);

        rowIndex++;
        Label toReceiveDescription = new Label(Res.get("bisqEasy.takeOffer.review.toReceive"));
        toReceiveDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(toReceiveDescription, rowIndex);
        GridPane.setColumnIndex(toReceiveDescription, 0);
        content.getChildren().add(toReceiveDescription);

        toReceive = new Label();
        toReceive.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(toReceive, rowIndex);
        GridPane.setColumnIndex(toReceive, 1);
        content.getChildren().add(toReceive);

        rowIndex++;
        Label methodDescription = new Label(Res.get("bisqEasy.takeOffer.review.method"));
        methodDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(methodDescription, rowIndex);
        GridPane.setColumnIndex(methodDescription, 0);
        content.getChildren().add(methodDescription);

        method = new Label();
        method.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(method, rowIndex);
        GridPane.setColumnIndex(method, 1);
        content.getChildren().add(method);

        rowIndex++;
        Label sellersPriceDescription = new Label(Res.get("bisqEasy.takeOffer.review.price.sellersPrice"));
        sellersPriceDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(sellersPriceDescription, rowIndex);
        GridPane.setColumnIndex(sellersPriceDescription, 0);
        content.getChildren().add(sellersPriceDescription);

        sellersPrice = new Label();
        sellersPrice.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(sellersPrice, rowIndex);
        GridPane.setColumnIndex(sellersPrice, 1);
        content.getChildren().add(sellersPrice);

        sellersPriceDetails = new Label();
        sellersPriceDetails.getStyleClass().add(valueDetailsStyle);
        GridPane.setColumnSpan(sellersPriceDetails, 3);
        content.add(sellersPriceDetails, 2, rowIndex);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.takeOffer.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(descriptionStyle);
        content.add(feeInfoDescription, 0, rowIndex);

        fee = new Label();
        fee.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(fee, 3);
        content.add(fee, 1, rowIndex);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setMargin(line3, new Insets(2, 0, 0, 0));
        GridPane.setRowIndex(line3, rowIndex);
        GridPane.setColumnSpan(line3, 4);
        content.getChildren().add(line3);

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOfferSuccessButton"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(content, new Insets(40));
        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, takeOfferSuccess);
    }


    @Override
    protected void onViewAttached() {
        directionHeadline.textProperty().bind(model.getDirectionHeadline());
        amounts.textProperty().bind(model.getAmountDescription());
        toPay.textProperty().bind(model.getToPay());
        toReceive.textProperty().bind(model.getToReceive());
        method.textProperty().bind(model.getMethod());
        sellersPrice.textProperty().bind(model.getSellersPrice());
        sellersPriceDetails.textProperty().bind(model.getSellersPriceDetails());
        fee.setText(model.getFee());

        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        showTakeOfferSuccessPin = EasyBind.subscribe(model.getShowTakeOfferSuccess(),
                show -> {
                    takeOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(takeOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        directionHeadline.textProperty().unbind();
        amounts.textProperty().unbind();
        toPay.textProperty().unbind();
        toReceive.textProperty().unbind();
        method.textProperty().unbind();
        sellersPrice.textProperty().unbind();
        sellersPriceDetails.textProperty().unbind();

        takeOfferSuccessButton.setOnAction(null);

        showTakeOfferSuccessPin.unsubscribe();
    }

    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setVisible(false);
        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.takeOffer.review.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.takeOffer.review.takeOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        takeOfferSuccessButton.setDefaultButton(true);
        VBox.setMargin(takeOfferSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, takeOfferSuccessButton);
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
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-21");
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
