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
            priceDetails, priceDescription;
    private final VBox createOfferSuccess, takeOfferSuccess;
    private final Button createOfferSuccessButton, takeOfferSuccessButton;
    private final GridPane content;
    private final StackPane paymentMethodValuePane;
    private final MultiStyleLabelPane price;
    private final HBox reviewDataDisplay;
    @Nullable
    private ComboBox<FiatPaymentMethod> paymentMethodsComboBox;
    private Subscription showCreateOfferSuccessPin, showTakeOfferSuccessPin;

    TradeWizardReviewView(TradeWizardReviewModel model, TradeWizardReviewController controller, HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        this.reviewDataDisplay = reviewDataDisplay;

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

        String descriptionStyle = "trade-wizard-review-description";
        String valueStyle = "trade-wizard-review-value";
        String detailsStyle = "trade-wizard-review-details";

        int rowIndex = 0;
        headline = new Label();
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
        content.add(reviewDataDisplay, 0, rowIndex);

        rowIndex++;
        detailsHeadline = new Label();
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setColumnSpan(detailsHeadline, 4);
        content.add(detailsHeadline, 0, rowIndex);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        GridPane.setColumnSpan(line2, 4);
        content.add(line2, 0, rowIndex);

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
        detailsHeadline.setText(model.getDetailsHeadline());

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
            paymentMethodsComboBox = new ComboBox<>(model.getTakersPaymentMethods());
            paymentMethodsComboBox.getStyleClass().add("trade-wizard-review-payment-combo-box");
            GridPane.setMargin(paymentMethodValuePane, new Insets(-8, 0, -8, 0));
            paymentMethodValuePane.getChildren().setAll(paymentMethodsComboBox);
            paymentMethodsComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(FiatPaymentMethod method) {
                    return method != null ? method.getDisplayString() : "";
                }

                @Override
                public FiatPaymentMethod fromString(String string) {
                    return null;
                }
            });

            paymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedPaymentMethod());
            paymentMethodsComboBox.setOnAction(e -> {
                if (paymentMethodsComboBox.getSelectionModel().getSelectedItem() == null) {
                    paymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedPaymentMethod());
                    return;
                }
                controller.onSelectFiatPaymentMethod(paymentMethodsComboBox.getSelectionModel().getSelectedItem());
            });
        } else {
            GridPane.setMargin(paymentMethodValuePane, new Insets(0, 0, 0, 0));
            paymentMethodValuePane.getChildren().setAll(paymentMethod);
        }

        if (model.isRangeAmount()) {
            GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 45, 0));
        } else {
            GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 10, 0));
        }
    }

    @Override
    protected void onViewDetached() {
        createOfferSuccessButton.setOnAction(null);
        takeOfferSuccessButton.setOnAction(null);

        showCreateOfferSuccessPin.unsubscribe();
        showTakeOfferSuccessPin.unsubscribe();

        if (paymentMethodsComboBox != null) {
            paymentMethodsComboBox.setOnAction(null);
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
