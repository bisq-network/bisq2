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

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class MuSigCreateOfferReviewView extends View<StackPane, MuSigCreateOfferReviewModel, MuSigCreateOfferReviewController> {
    public static final String DESCRIPTION_STYLE = "trade-wizard-review-description";
    public static final String VALUE_STYLE = "trade-wizard-review-value";
    public static final String DETAILS_STYLE = "trade-wizard-review-details";

    private final Label headline, detailsHeadline, priceDetails, priceDescription,
            paymentMethodDescription, paymentMethod, paymentMethodDetails,
            collateral, collateralDetails, fee, feeDetails;
    private final WizardOverlay createOfferSuccessOverlay;
    private final Button createOfferSuccessButton;
    private final GridPane gridPane;
    private final TextFlow price;
    private final HBox reviewDataDisplay;
    private Subscription showCreateOfferSuccessPin;

    MuSigCreateOfferReviewView(MuSigCreateOfferReviewModel model,
                               MuSigCreateOfferReviewController controller,
                               HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        this.reviewDataDisplay = reviewDataDisplay;

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
        Label collateralDescription = new Label(Res.get("muSig.tradeWizard.review.collateral.description"));
        collateralDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(collateralDescription, 0, rowIndex);

        collateral = new Label();
        collateral.getStyleClass().add(VALUE_STYLE);
        gridPane.add(collateral, 1, rowIndex);

        collateralDetails = new Label(Res.get("muSig.tradeWizard.review.collateral.maker.details"));
        collateralDetails.getStyleClass().add(DETAILS_STYLE);
        gridPane.add(collateralDetails, 2, rowIndex, 2, 1);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.tradeWizard.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(DESCRIPTION_STYLE);
        gridPane.add(feeInfoDescription, 0, rowIndex);

        fee = new Label(Res.get("bisqEasy.tradeWizard.review.collateral.details"));
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
        createOfferSuccessButton.setDefaultButton(true);
        createOfferSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("bisqEasy.tradeWizard.review.createOfferSuccess.headline")
                .descriptionFromI18nKey("bisqEasy.tradeWizard.review.createOfferSuccess.subTitle")
                .buttons(createOfferSuccessButton)
                .build();

        StackPane.setMargin(gridPane, new Insets(40));
        root.getChildren().addAll(gridPane, createOfferSuccessOverlay);
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

        collateral.setText(model.getCollateralAsPercent());
        collateralDetails.setText(model.getCollateralAsBtc());

        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        createOfferSuccessButton.setOnAction(e -> controller.onShowOfferbook());

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(), shouldShow ->
                createOfferSuccessOverlay.updateOverlayVisibility(gridPane,
                        shouldShow,
                        controller::onKeyPressedWhileShowingOverlay));

        if (model.isRangeAmount() && model.getMarket().isCrypto()) {
            GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 45, 0));
        } else {
            GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 10, 0));
        }
    }

    @Override
    protected void onViewDetached() {
        createOfferSuccessButton.setOnAction(null);
        showCreateOfferSuccessPin.unsubscribe();
        paymentMethod.setTooltip(null);
        paymentMethodDetails.setTooltip(null);
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
