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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MultiStyleLabelPane;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferView;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
class TradeWizardReviewView extends View<StackPane, TradeWizardReviewModel, TradeWizardReviewController> {
    private final static int FEEDBACK_WIDTH = 700;

    private final Label headline, detailsHeadline,
            paymentMethod, paymentMethodDescription, fee, feeDetails,
            priceDetails, toReceiveAmountDescription, toSendAmountDescription, priceDescription;
    private final VBox createOfferSuccess, takeOfferSuccess;
    private final Button createOfferSuccessButton, takeOfferSuccessButton;
    private final GridPane content;
    private final StackPane paymentMethodValuePane;
    private final MultiStyleLabelPane directionHeadline, minAmountsHeadline, maxAmountsHeadline,
            fixAmountsHeadline, toSendAmount, toReceiveAmount, price;
    @Nullable
    private ComboBox<FiatPaymentMethod> takersFiatPaymentMethods;
    private Subscription showCreateOfferSuccessPin, showTakeOfferSuccessPin;

    TradeWizardReviewView(TradeWizardReviewModel model, TradeWizardReviewController controller) {
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
        String detailsStyle = "trade-wizard-review-details";

        int rowIndex = 0;
        headline = new Label();
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(0, 20, 10, 0));
        GridPane.setColumnSpan(headline, 4);
        content.add(headline, 0, rowIndex);
        content.setMouseTransparent(true);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setColumnSpan(line1, 4);
        content.add(line1, 0, rowIndex);

        rowIndex++;
        directionHeadline = new MultiStyleLabelPane();
        directionHeadline.getStyleClass().add("trade-wizard-review-direction");
        GridPane.setMargin(directionHeadline, new Insets(16, 0, 10, 0));
        GridPane.setColumnSpan(directionHeadline, 4);
        content.add(directionHeadline, 0, rowIndex);

        rowIndex++;
        minAmountsHeadline = new MultiStyleLabelPane();
        GridPane.setMargin(minAmountsHeadline, new Insets(-15, 0, -18, 0));
        GridPane.setColumnSpan(minAmountsHeadline, 4);
        content.add(minAmountsHeadline, 0, rowIndex);

        rowIndex++;
        maxAmountsHeadline = new MultiStyleLabelPane();
        GridPane.setColumnSpan(maxAmountsHeadline, 4);
        content.add(maxAmountsHeadline, 0, rowIndex);

        rowIndex++;
        fixAmountsHeadline = new MultiStyleLabelPane();
        GridPane.setMargin(fixAmountsHeadline, new Insets(-27, 0, 17, 0));
        GridPane.setColumnSpan(fixAmountsHeadline, 4);
        content.add(fixAmountsHeadline, 0, rowIndex);

        rowIndex++;
        detailsHeadline = new Label();
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setMargin(detailsHeadline, new Insets(0, 0, -2, 0));
        GridPane.setColumnSpan(detailsHeadline, 4);
        content.add(detailsHeadline, 0, rowIndex);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(0, 0, 3, 0));
        GridPane.setColumnSpan(line2, 4);
        content.add(line2, 0, rowIndex);

        rowIndex++;
        toSendAmountDescription = new Label();
        toSendAmountDescription.getStyleClass().add(descriptionStyle);
        content.add(toSendAmountDescription, 0, rowIndex);

        toSendAmount = new MultiStyleLabelPane();
        toSendAmount.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(toSendAmount, 2);
        content.add(toSendAmount, 1, rowIndex);

        rowIndex++;
        toReceiveAmountDescription = new Label();
        toReceiveAmountDescription.getStyleClass().add(descriptionStyle);
        content.add(toReceiveAmountDescription, 0, rowIndex);

        toReceiveAmount = new MultiStyleLabelPane();
        toReceiveAmount.getStyleClass().add(valueStyle);
        GridPane.setColumnSpan(toReceiveAmount, 2);
        content.add(toReceiveAmount, 1, rowIndex);

        rowIndex++;
        priceDescription = new Label();
        priceDescription.getStyleClass().add(descriptionStyle);
        content.add(priceDescription, 0, rowIndex);

        price = new MultiStyleLabelPane();
        price.getStyleClass().add(valueStyle);
        content.add(price, 1, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(detailsStyle);
        GridPane.setColumnSpan(priceDetails, 2);
        content.add(priceDetails, 2, rowIndex);

        rowIndex++;
        paymentMethodDescription = new Label();
        paymentMethodDescription.getStyleClass().add(descriptionStyle);
        content.add(paymentMethodDescription, 0, rowIndex);

        paymentMethod = new Label();
        paymentMethod.getStyleClass().add(valueStyle);
        paymentMethodValuePane = new StackPane(paymentMethod);
        paymentMethodValuePane.setAlignment(Pos.TOP_LEFT);
        GridPane.setColumnSpan(paymentMethodValuePane, 3);
        content.add(paymentMethodValuePane, 1, rowIndex);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.tradeWizard.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(descriptionStyle);
        content.add(feeInfoDescription, 0, rowIndex);

        fee = new Label();
        fee.getStyleClass().add(valueStyle);
        content.add(fee, 1, rowIndex);

        feeDetails = new Label();
        feeDetails.getStyleClass().add(detailsStyle);
        GridPane.setColumnSpan(feeDetails, 2);
        content.add(feeDetails, 2, rowIndex);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setMargin(line3, new Insets(2, 0, 0, 0));
        GridPane.setColumnSpan(line3, 4);
        content.add(line3, 0, rowIndex);


        // Feedback overlays
        createOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.createOfferSuccessButton"));
        createOfferSuccess = new VBox(20);
        configCreateOfferSuccess();

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.takeOfferSuccessButton"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(content, new Insets(40));
        StackPane.setMargin(createOfferSuccess, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, createOfferSuccess, takeOfferSuccess);
    }

    @Override
    protected void onViewAttached() {
        headline.setText(model.getHeadline());
        directionHeadline.setText(model.getDirectionHeadline());
        minAmountsHeadline.setText(model.getMinAmountsHeadline());
        minAmountsHeadline.setManaged(model.getMinAmountsHeadline() != null);
        minAmountsHeadline.setVisible(model.getMinAmountsHeadline() != null);
        maxAmountsHeadline.setText(model.getMaxAmountsHeadline());
        maxAmountsHeadline.setManaged(model.getMaxAmountsHeadline() != null);
        maxAmountsHeadline.setVisible(model.getMaxAmountsHeadline() != null);
        fixAmountsHeadline.setText(model.getFixAmountsHeadline());
        fixAmountsHeadline.setManaged(model.getFixAmountsHeadline() != null);
        fixAmountsHeadline.setVisible(model.getFixAmountsHeadline() != null);
        if (model.getFixAmountsHeadline() == null) {
            GridPane.setMargin(directionHeadline, new Insets(9, 0, 10, 0));
        } else {
            GridPane.setMargin(directionHeadline, new Insets(16, 0, 10, 0));
        }
        detailsHeadline.setText(model.getDetailsHeadline());

        toSendAmountDescription.setText(model.getToSendAmountDescription());
        toSendAmount.setText(model.getToSendAmount());
        toReceiveAmountDescription.setText(model.getToReceiveAmountDescription());
        toReceiveAmount.setText(model.getToReceiveAmount());

        priceDescription.setText(model.getPriceDescription());
        price.setText(model.getPrice());
        priceDetails.setText(model.getPriceDetails());

        paymentMethodDescription.setText(model.getPaymentMethodDescription());
        paymentMethod.setText(model.getPaymentMethod());
        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        createOfferSuccessButton.setOnAction(e -> controller.onShowOfferbook());
        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(),
                show -> {
                    createOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(createOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
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

        if (model.getTakersPaymentMethods().size() > 1) {
            takersFiatPaymentMethods = new ComboBox<>(model.getTakersPaymentMethods());
            takersFiatPaymentMethods.getStyleClass().add("trade-wizard-review-value");
            StackPane.setMargin(takersFiatPaymentMethods, new Insets(0, 0, 0, -9));
            paymentMethodValuePane.getChildren().setAll(takersFiatPaymentMethods);
            takersFiatPaymentMethods.setConverter(new StringConverter<>() {
                @Override
                public String toString(FiatPaymentMethod method) {
                    return method != null ? method.getShortDisplayString() : "";
                }

                @Override
                public FiatPaymentMethod fromString(String string) {
                    return null;
                }
            });

            takersFiatPaymentMethods.getSelectionModel().select(model.getTakersSelectedPaymentMethod());
            takersFiatPaymentMethods.setOnAction(e -> {
                if (takersFiatPaymentMethods.getSelectionModel().getSelectedItem() == null) {
                    takersFiatPaymentMethods.getSelectionModel().select(model.getTakersSelectedPaymentMethod());
                    return;
                }
                controller.onSelectFiatPaymentMethod(takersFiatPaymentMethods.getSelectionModel().getSelectedItem());
            });
        } else {
            paymentMethodValuePane.getChildren().setAll(paymentMethod);
        }
    }

    @Override
    protected void onViewDetached() {
        createOfferSuccessButton.setOnAction(null);
        takeOfferSuccessButton.setOnAction(null);

        showCreateOfferSuccessPin.unsubscribe();
        showTakeOfferSuccessPin.unsubscribe();

        if (takersFiatPaymentMethods != null) {
            takersFiatPaymentMethods.setOnAction(null);
        }
    }

    private void configCreateOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        createOfferSuccess.setVisible(false);
        createOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.tradeWizard.review.createOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.review.createOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        createOfferSuccessButton.setDefaultButton(true);
        VBox.setMargin(createOfferSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, createOfferSuccessButton);
        createOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
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
        subtitleLabel.getStyleClass().add("bisq-text-21");
        subtitleLabel.setWrapText(true);
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
