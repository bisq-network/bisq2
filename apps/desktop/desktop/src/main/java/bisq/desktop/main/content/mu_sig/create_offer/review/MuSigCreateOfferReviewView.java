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

package bisq.desktop.main.content.mu_sig.create_offer.review;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferView;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
class MuSigCreateOfferReviewView extends View<StackPane, MuSigCreateOfferReviewModel, MuSigCreateOfferReviewController> {
    private final static int FEEDBACK_WIDTH = 700;
    public static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    public static final String VALUE_STYLE = "trade-wizard-review-value";
    public static final String DETAILS_STYLE = "trade-wizard-review-details";

    private final Label headline, detailsHeadline, priceDetails, priceDescription,
            paymentMethodDescription, paymentMethod, paymentMethodDetails, fee, feeDetails;
    private final VBox createOfferSuccess;
    private final Button createOfferSuccessButton;
    private final GridPane gridPane;
    private final TextFlow price;
    private Subscription showCreateOfferSuccessPin;

    MuSigCreateOfferReviewView(MuSigCreateOfferReviewModel model,
                               MuSigCreateOfferReviewController controller,
                               HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setMouseTransparent(true);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 4);

        int rowIndex = 0;
        headline = new Label();
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
        detailsHeadline = new Label();
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        gridPane.add(detailsHeadline, 0, rowIndex, 4, 1);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        gridPane.add(line2, 0, rowIndex, 4, 1);

        rowIndex++;
        priceDescription = new Label();
        priceDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(VALUE_STYLE);
        gridPane.add(price, 1, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(priceDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        paymentMethodDescription = new Label();
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

        // Feedback overlays
        createOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.createOfferSuccessButton"));
        createOfferSuccess = new VBox(20);
        configCreateOfferSuccess();

        StackPane.setMargin(gridPane, new Insets(40));
        StackPane.setMargin(createOfferSuccess, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(gridPane, createOfferSuccess);
    }

    @Override
    protected void onViewAttached() {
        headline.setText(model.getHeadline());
        detailsHeadline.setText(model.getDetailsHeadline());

        priceDescription.setText(model.getPriceDescription());
        TextFlowUtils.updateTextFlow(price, model.getPriceWithCode());
        priceDetails.setText(model.getPriceDetails());

        paymentMethodDescription.setText(model.getPaymentMethodDescription());
        String paymentMethodsDisplayString = model.getPaymentMethodsDisplayString();
        paymentMethod.setText(paymentMethodsDisplayString);
        if (paymentMethodsDisplayString.length() > 25) {
            paymentMethod.setTooltip(new BisqTooltip(paymentMethodsDisplayString));
        }
        String paymentMethodDetailsValue = model.getPaymentMethodDetails();
        paymentMethodDetails.setText(paymentMethodDetailsValue);
        if (paymentMethodDetailsValue.length() > 50) {
            paymentMethodDetails.setTooltip(new BisqTooltip(paymentMethodDetailsValue));
        }

        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        createOfferSuccessButton.setOnAction(e -> controller.onShowOfferbook());

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(),
            show -> {
                createOfferSuccess.setVisible(show);
                if (show) {
                    Transitions.blurStrong(gridPane, 0);
                    Transitions.slideInTop(createOfferSuccess, 450);
                } else {
                    Transitions.removeEffect(gridPane);
                }
            });
    }

    @Override
    protected void onViewDetached() {
        createOfferSuccessButton.setOnAction(null);
        showCreateOfferSuccessPin.unsubscribe();
        paymentMethod.setTooltip(null);
        paymentMethodDetails.setTooltip(null);
    }

    private void configCreateOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        createOfferSuccess.setVisible(false);
        createOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.review.createOfferSuccess.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.review.createOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        createOfferSuccessButton.setDefaultButton(true);
        VBox.setMargin(createOfferSuccessButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headlineLabel, subtitleLabel, createOfferSuccessButton);
        createOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
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
