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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.TakeOfferView;
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

    private final Label subtitle;
    private final VBox content, takeOfferSuccess;
    private final Button takeOfferSuccessButton;
    private final Label amounts;
    private final Label payValue;
    private final Label receiveValue;
    private final Label methodValue;
    private final Label sellersPriceValue;
    private final Label sellersPriceValueDetails;
    private final Label sellersPremiumValue;
    private final Label sellersPremiumValueDetails;
    private Subscription showTakeOfferSuccessPin;

    TakeOfferReviewView(TakeOfferReviewModel model, TakeOfferReviewController controller, Pane sellersPriceComponent) {
        super(new StackPane(), model, controller);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(20);
        gridPane.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col1, col2, col3);

        content = new VBox(10);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(0, 30, 0, 30));

        String descriptionStyle = "take-offer-review-description";
        String valueStyle = "take-offer-review-value";
        String valueDetailsStyle = "take-offer-review-value-details";

        int rowIndex = 0;
        Label headline = new Label(Res.get("bisqEasy.takeOffer.review.headline"));
        headline.getStyleClass().add("take-offer-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(-55, 0, 20, 0));
        GridPane.setRowIndex(headline, rowIndex);
        GridPane.setColumnSpan(headline, 3);
        gridPane.getChildren().add(headline);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setRowIndex(line1, rowIndex);
        GridPane.setColumnSpan(line1, 3);
        gridPane.getChildren().add(line1);

        rowIndex++;
        subtitle = new Label();
        subtitle.getStyleClass().addAll("take-offer-review-subtitle");
        GridPane.setMargin(subtitle, new Insets(16, 0, 0, 0));
        GridPane.setRowIndex(subtitle, rowIndex);
        GridPane.setColumnSpan(subtitle, 3);
        gridPane.getChildren().add(subtitle);

        rowIndex++;
        amounts = new Label();
        amounts.getStyleClass().add("take-offer-review-subtitle-value");
        GridPane.setMargin(amounts, new Insets(-7, 0, 17, 0));
        GridPane.setRowIndex(amounts, rowIndex);
        GridPane.setColumnSpan(amounts, 3);
        gridPane.getChildren().add(amounts);

        rowIndex++;
        Label gridPaneHeadline = new Label(Res.get("bisqEasy.takeOffer.review.gridPaneHeadline").toUpperCase());
        gridPaneHeadline.getStyleClass().add("take-offer-review-grid-headline");
        GridPane.setMargin(gridPaneHeadline, new Insets(0, 0, -2, 0));
        GridPane.setRowIndex(gridPaneHeadline, rowIndex);
        GridPane.setColumnSpan(gridPaneHeadline, 3);
        gridPane.getChildren().add(gridPaneHeadline);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(0, 0, 3, 0));
        GridPane.setRowIndex(line2, rowIndex);
        GridPane.setColumnSpan(line2, 3);
        gridPane.getChildren().add(line2);

        rowIndex++;
        Label pay = new Label(Res.get("bisqEasy.takeOffer.review.pay"));
        pay.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(pay, rowIndex);
        GridPane.setColumnIndex(pay, 0);
        gridPane.getChildren().add(pay);

        payValue = new Label();
        payValue.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(payValue, rowIndex);
        GridPane.setColumnIndex(payValue, 1);
        gridPane.getChildren().add(payValue);

        rowIndex++;
        Label receive = new Label(Res.get("bisqEasy.takeOffer.review.receive"));
        receive.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(receive, rowIndex);
        GridPane.setColumnIndex(receive, 0);
        gridPane.getChildren().add(receive);

        receiveValue = new Label();
        receiveValue.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(receiveValue, rowIndex);
        GridPane.setColumnIndex(receiveValue, 1);
        gridPane.getChildren().add(receiveValue);

        rowIndex++;
        Label method = new Label(Res.get("bisqEasy.takeOffer.review.method"));
        method.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(method, rowIndex);
        GridPane.setColumnIndex(method, 0);
        gridPane.getChildren().add(method);

        methodValue = new Label();
        methodValue.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(methodValue, rowIndex);
        GridPane.setColumnIndex(methodValue, 1);
        gridPane.getChildren().add(methodValue);

        rowIndex++;
        Label sellersPrice = new Label(Res.get("bisqEasy.takeOffer.review.sellersPrice"));
        sellersPrice.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(sellersPrice, rowIndex);
        GridPane.setColumnIndex(sellersPrice, 0);
        gridPane.getChildren().add(sellersPrice);

        sellersPriceValue = new Label();
        sellersPriceValue.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(sellersPriceValue, rowIndex);
        GridPane.setColumnIndex(sellersPriceValue, 1);
        gridPane.getChildren().add(sellersPriceValue);

        sellersPriceValueDetails = new Label();
        sellersPriceValueDetails.getStyleClass().add(valueDetailsStyle);
        GridPane.setRowIndex(sellersPriceValueDetails, rowIndex);
        GridPane.setColumnIndex(sellersPriceValueDetails, 2);
        gridPane.getChildren().add(sellersPriceValueDetails);

        rowIndex++;
        Label sellersPremium = new Label(Res.get("bisqEasy.takeOffer.review.sellersPremium"));
        sellersPremium.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(sellersPremium, rowIndex);
        GridPane.setColumnIndex(sellersPremium, 0);
        gridPane.getChildren().add(sellersPremium);

        sellersPremiumValue = new Label();
        sellersPremiumValue.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(sellersPremiumValue, rowIndex);
        GridPane.setColumnIndex(sellersPremiumValue, 1);
        gridPane.getChildren().add(sellersPremiumValue);

        sellersPremiumValueDetails = new Label(Res.get("bisqEasy.takeOffer.review.sellersPremium.details"));
        sellersPremiumValueDetails.getStyleClass().add(valueDetailsStyle);
        GridPane.setRowIndex(sellersPremiumValueDetails, rowIndex);
        GridPane.setColumnIndex(sellersPremiumValueDetails, 2);
        gridPane.getChildren().add(sellersPremiumValueDetails);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setMargin(line3, new Insets(2, 0, 0, 0));
        GridPane.setRowIndex(line3, rowIndex);
        GridPane.setColumnSpan(line3, 3);
        gridPane.getChildren().add(line3);

        content.getChildren().addAll(Spacer.fillVBox(), gridPane, Spacer.fillVBox());

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.takeOffer.review.takeOfferSuccessButton"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, takeOfferSuccess);
    }


    @Override
    protected void onViewAttached() {
        subtitle.textProperty().bind(model.getSubtitle());
        amounts.textProperty().bind(model.getAmounts());
        payValue.textProperty().bind(model.getPayValue());
        receiveValue.textProperty().bind(model.getReceiveValue());
        methodValue.textProperty().bind(model.getMethodValue());
        sellersPriceValue.textProperty().bind(model.getSellersPriceValue());
        sellersPriceValueDetails.textProperty().bind(model.getSellersPriceValueDetails());
        sellersPremiumValue.textProperty().bind(model.getSellersPremiumValue());
        // sellersPremiumValueDetails.textProperty().bind(model.getSellersPremiumValueDetails());

        takeOfferSuccessButton.setOnAction(e -> controller.onOpenPrivateChat());

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
        subtitle.textProperty().unbind();
        amounts.textProperty().unbind();
        payValue.textProperty().unbind();
        receiveValue.textProperty().unbind();
        methodValue.textProperty().unbind();
        sellersPriceValue.textProperty().unbind();
        sellersPriceValueDetails.textProperty().unbind();
        sellersPremiumValue.textProperty().unbind();
        //sellersPremiumValueDetails.textProperty().unbind();

        takeOfferSuccessButton.setOnAction(null);

        showTakeOfferSuccessPin.unsubscribe();
    }

    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setVisible(false);
        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        takeOfferSuccessButton.setDefaultButton(true);
        VBox.setMargin(takeOfferSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, takeOfferSuccessButton);
        takeOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("create-offer-feedback-bg");
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
        subtitleLabel.getStyleClass().addAll("bisq-text-21", "wrap-text");
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
