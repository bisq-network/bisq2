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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
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
class TradeWizardReviewView extends View<StackPane, TradeWizardReviewModel, TradeWizardReviewController> {
    private final static int FEEDBACK_WIDTH = 700;

    private final Label subtitle;
    private final VBox takeOfferSuccess;
    private final Button takeOfferSuccessButton;
    private final Label amounts;
    private final Label toPay;
    private final Label toReceive;
    private final Label method;
    private final Label sellersPrice;
    private final Label sellersPriceDetails;
    private final Label sellersPremium;
    private final GridPane gridPane;
    private Subscription showTakeOfferSuccessPin;

    TradeWizardReviewView(TradeWizardReviewModel model, TradeWizardReviewController controller) {
        super(new StackPane(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25);
        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);
        gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

        String descriptionStyle = "take-offer-review-description";
        String valueStyle = "take-offer-review-value";
        String valueDetailsStyle = "take-offer-review-value-details";

        int rowIndex = 0;
        Label headline = new Label(Res.get("bisqEasy.tradeWizard.review.headline"));
        headline.getStyleClass().add("take-offer-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        // GridPane.setMargin(headline, new Insets(-TakeOfferView.TOP_PANE_HEIGHT + 5, 20, 20, 0));
        GridPane.setMargin(headline, new Insets(0, 20, 10, 0));
        GridPane.setRowIndex(headline, rowIndex);
        GridPane.setColumnIndex(headline, 1);
        GridPane.setColumnSpan(headline, 2);
        gridPane.getChildren().add(headline);
        gridPane.setMouseTransparent(true);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setRowIndex(line1, rowIndex);
        GridPane.setColumnSpan(line1, 4);
        gridPane.getChildren().add(line1);

        rowIndex++;
        subtitle = new Label();
        subtitle.getStyleClass().addAll("take-offer-review-subtitle");
        GridPane.setMargin(subtitle, new Insets(16, 0, 0, 0));
        GridPane.setRowIndex(subtitle, rowIndex);
        GridPane.setColumnSpan(subtitle, 4);
        gridPane.getChildren().add(subtitle);

        rowIndex++;
        amounts = new Label();
        amounts.getStyleClass().add("take-offer-review-subtitle-value");
        GridPane.setMargin(amounts, new Insets(-7, 0, 17, 0));
        GridPane.setRowIndex(amounts, rowIndex);
        GridPane.setColumnSpan(amounts, 4);
        gridPane.getChildren().add(amounts);

        rowIndex++;
        Label gridPaneHeadline = new Label(Res.get("bisqEasy.tradeWizard.review.gridPaneHeadline").toUpperCase());
        gridPaneHeadline.getStyleClass().add("take-offer-review-grid-headline");
        GridPane.setMargin(gridPaneHeadline, new Insets(0, 0, -2, 0));
        GridPane.setRowIndex(gridPaneHeadline, rowIndex);
        GridPane.setColumnSpan(gridPaneHeadline, 4);
        gridPane.getChildren().add(gridPaneHeadline);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(0, 0, 3, 0));
        GridPane.setRowIndex(line2, rowIndex);
        GridPane.setColumnSpan(line2, 4);
        gridPane.getChildren().add(line2);

        rowIndex++;
        Label toPayDescription = new Label(Res.get("bisqEasy.tradeWizard.review.toPay"));
        toPayDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(toPayDescription, rowIndex);
        GridPane.setColumnIndex(toPayDescription, 0);
        gridPane.getChildren().add(toPayDescription);

        toPay = new Label();
        toPay.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(toPay, rowIndex);
        GridPane.setColumnIndex(toPay, 1);
        gridPane.getChildren().add(toPay);

        rowIndex++;
        Label toReceiveDescription = new Label(Res.get("bisqEasy.tradeWizard.review.toReceive"));
        toReceiveDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(toReceiveDescription, rowIndex);
        GridPane.setColumnIndex(toReceiveDescription, 0);
        gridPane.getChildren().add(toReceiveDescription);

        toReceive = new Label();
        toReceive.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(toReceive, rowIndex);
        GridPane.setColumnIndex(toReceive, 1);
        gridPane.getChildren().add(toReceive);

        rowIndex++;
        Label methodDescription = new Label(Res.get("bisqEasy.tradeWizard.review.method"));
        methodDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(methodDescription, rowIndex);
        GridPane.setColumnIndex(methodDescription, 0);
        gridPane.getChildren().add(methodDescription);

        method = new Label();
        method.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(method, rowIndex);
        GridPane.setColumnIndex(method, 1);
        gridPane.getChildren().add(method);

        rowIndex++;
        Label sellersPriceDescription = new Label(Res.get("bisqEasy.tradeWizard.review.price.sellersPrice"));
        sellersPriceDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(sellersPriceDescription, rowIndex);
        GridPane.setColumnIndex(sellersPriceDescription, 0);
        gridPane.getChildren().add(sellersPriceDescription);

        sellersPrice = new Label();
        sellersPrice.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(sellersPrice, rowIndex);
        GridPane.setColumnIndex(sellersPrice, 1);
        gridPane.getChildren().add(sellersPrice);

        sellersPriceDetails = new Label();
        sellersPriceDetails.getStyleClass().add(valueDetailsStyle);
        GridPane.setRowIndex(sellersPriceDetails, rowIndex);
        GridPane.setColumnIndex(sellersPriceDetails, 2);
        GridPane.setColumnSpan(sellersPriceDetails, 2);
        gridPane.getChildren().add(sellersPriceDetails);

        rowIndex++;
        Label sellersPremiumDescription = new Label(Res.get("bisqEasy.tradeWizard.review.sellersPremium"));
        sellersPremiumDescription.getStyleClass().add(descriptionStyle);
        GridPane.setRowIndex(sellersPremiumDescription, rowIndex);
        GridPane.setColumnIndex(sellersPremiumDescription, 0);
        gridPane.getChildren().add(sellersPremiumDescription);

        sellersPremium = new Label();
        sellersPremium.getStyleClass().add(valueStyle);
        GridPane.setRowIndex(sellersPremium, rowIndex);
        GridPane.setColumnIndex(sellersPremium, 1);
        gridPane.getChildren().add(sellersPremium);

        Label sellersPremiumDetails = new Label(Res.get("bisqEasy.tradeWizard.review.sellersPremium.details"));
        sellersPremiumDetails.getStyleClass().add(valueDetailsStyle);
        GridPane.setRowIndex(sellersPremiumDetails, rowIndex);
        GridPane.setColumnIndex(sellersPremiumDetails, 2);
        GridPane.setColumnSpan(sellersPremiumDetails, 2);
        gridPane.getChildren().add(sellersPremiumDetails);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setMargin(line3, new Insets(2, 0, 0, 0));
        GridPane.setRowIndex(line3, rowIndex);
        GridPane.setColumnSpan(line3, 4);
        gridPane.getChildren().add(line3);

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.takeOfferSuccessButton"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        StackPane.setMargin(gridPane, new Insets(40));
        root.getChildren().addAll(gridPane, takeOfferSuccess);
    }

    @Override
    protected void onViewAttached() {
        subtitle.textProperty().bind(model.getSubtitle());
        amounts.textProperty().bind(model.getAmountDescription());
        toPay.textProperty().bind(model.getToPay());
        toReceive.textProperty().bind(model.getToReceive());
        method.textProperty().bind(model.getFiatPaymentMethodDisplayString());
        sellersPrice.textProperty().bind(model.getSellersPrice());
        sellersPriceDetails.textProperty().bind(model.getSellersPriceDetails());
        sellersPremium.textProperty().bind(model.getSellersPremium());

        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        showTakeOfferSuccessPin = EasyBind.subscribe(model.getShowTakeOfferSuccess(),
                show -> {
                    takeOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(gridPane, 0);
                        Transitions.slideInTop(takeOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(gridPane);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        subtitle.textProperty().unbind();
        amounts.textProperty().unbind();
        toPay.textProperty().unbind();
        toReceive.textProperty().unbind();
        method.textProperty().unbind();
        sellersPrice.textProperty().unbind();
        sellersPriceDetails.textProperty().unbind();
        sellersPremium.textProperty().unbind();
        //sellersPremiumValueDetails.textProperty().unbind();

        takeOfferSuccessButton.setOnAction(null);

        showTakeOfferSuccessPin.unsubscribe();
    }

    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setVisible(false);
        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.tradeWizard.review.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.review.takeOfferSuccess.subTitle"));
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
