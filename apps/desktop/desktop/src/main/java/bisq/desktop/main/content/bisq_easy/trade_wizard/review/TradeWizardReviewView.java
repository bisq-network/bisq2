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

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.application.DevMode;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.TextFlowUtils;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
class TradeWizardReviewView extends View<StackPane, TradeWizardReviewModel, TradeWizardReviewController> {
    private final Label headline, detailsHeadline, bitcoinPaymentMethod, bitcoinPaymentMethodDescription,
            fiatPaymentMethod, fiatPaymentMethodDescription, fee, feeDetails, priceDetails, priceDescription;
    private final WizardOverlay createOfferSuccessOverlay, sendTakeOfferMessageOverlay, takeOfferSuccessOverlay;
    private final Button createOfferSuccessButton, takeOfferSuccessButton;
    private final GridPane gridPane;
    private final StackPane bitcoinPaymentMethodValuePane, fiatPaymentMethodValuePane;
    private final TextFlow price;
    private final WaitingAnimation takeOfferSendMessageWaitingAnimation;
    @Nullable
    private ComboBox<BitcoinPaymentMethod> bitcoinPaymentMethodsComboBox;
    @Nullable
    private ComboBox<FiatPaymentMethod> fiatPaymentMethodsComboBox;
    private Subscription showCreateOfferSuccessPin, takeOfferStatusPin;
    private boolean minWaitingTimePassed = false;

    TradeWizardReviewView(TradeWizardReviewModel model,
                          TradeWizardReviewController controller,
                          HBox reviewDataDisplay) {
        super(new StackPane(), model, controller);

        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setMouseTransparent(true);
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, 4);

        String descriptionStyle = "trade-wizard-review-description";
        String valueStyle = "trade-wizard-review-value";
        String detailsStyle = "trade-wizard-review-details";

        int rowIndex = 0;
        headline = new Label();
        headline.getStyleClass().add("trade-wizard-review-headline");
        GridPane.setHalignment(headline, HPos.CENTER);
        GridPane.setMargin(headline, new Insets(10, 0, 30, 0));
        GridPane.setColumnSpan(headline, 4);
        gridPane.add(headline, 0, rowIndex);

        rowIndex++;
        Region line1 = getLine();
        GridPane.setColumnSpan(line1, 4);
        gridPane.add(line1, 0, rowIndex);

        rowIndex++;
        GridPane.setColumnSpan(reviewDataDisplay, 4);
        GridPane.setMargin(reviewDataDisplay, new Insets(0, 0, 10, 0));
        gridPane.add(reviewDataDisplay, 0, rowIndex);

        rowIndex++;
        detailsHeadline = new Label();
        detailsHeadline.getStyleClass().add("trade-wizard-review-details-headline");
        GridPane.setColumnSpan(detailsHeadline, 4);
        gridPane.add(detailsHeadline, 0, rowIndex);

        rowIndex++;
        Region line2 = getLine();
        GridPane.setMargin(line2, new Insets(-10, 0, -5, 0));
        GridPane.setColumnSpan(line2, 4);
        gridPane.add(line2, 0, rowIndex);

        rowIndex++;
        priceDescription = new Label();
        priceDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(priceDescription, 0, rowIndex);

        price = new TextFlow();
        price.getStyleClass().add(valueStyle);
        gridPane.add(price, 1, rowIndex);

        priceDetails = new Label();
        priceDetails.getStyleClass().add(detailsStyle);
        GridPane.setColumnSpan(priceDetails, 2);
        gridPane.add(priceDetails, 2, rowIndex);

        rowIndex++;
        bitcoinPaymentMethodDescription = new Label();
        bitcoinPaymentMethodDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(bitcoinPaymentMethodDescription, 0, rowIndex);

        bitcoinPaymentMethod = new Label();
        bitcoinPaymentMethod.getStyleClass().add(valueStyle);
        bitcoinPaymentMethodValuePane = new StackPane(bitcoinPaymentMethod);
        bitcoinPaymentMethodValuePane.setAlignment(Pos.TOP_LEFT);
        GridPane.setColumnSpan(bitcoinPaymentMethodValuePane, 3);
        gridPane.add(bitcoinPaymentMethodValuePane, 1, rowIndex);

        rowIndex++;
        fiatPaymentMethodDescription = new Label();
        fiatPaymentMethodDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(fiatPaymentMethodDescription, 0, rowIndex);

        fiatPaymentMethod = new Label();
        fiatPaymentMethod.getStyleClass().add(valueStyle);
        fiatPaymentMethodValuePane = new StackPane(fiatPaymentMethod);
        fiatPaymentMethodValuePane.setAlignment(Pos.TOP_LEFT);
        GridPane.setColumnSpan(fiatPaymentMethodValuePane, 3);
        gridPane.add(fiatPaymentMethodValuePane, 1, rowIndex);

        rowIndex++;
        Label feeInfoDescription = new Label(Res.get("bisqEasy.tradeWizard.review.feeDescription"));
        feeInfoDescription.getStyleClass().add(descriptionStyle);
        gridPane.add(feeInfoDescription, 0, rowIndex);

        fee = new Label();
        fee.getStyleClass().add(valueStyle);
        gridPane.add(fee, 1, rowIndex);

        feeDetails = new Label();
        feeDetails.getStyleClass().add(detailsStyle);
        GridPane.setColumnSpan(feeDetails, 2);
        gridPane.add(feeDetails, 2, rowIndex);

        rowIndex++;
        Region line3 = getLine();
        GridPane.setColumnSpan(line3, 4);
        gridPane.add(line3, 0, rowIndex);

        // Overlays
        createOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.createOfferSuccessButton"));
        createOfferSuccessButton.setDefaultButton(true);
        createOfferSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("bisqEasy.tradeWizard.review.createOfferSuccess.headline")
                .description("bisqEasy.tradeWizard.review.createOfferSuccess.subTitle")
                .buttons(createOfferSuccessButton)
                .build();

        takeOfferSendMessageWaitingAnimation = new WaitingAnimation(WaitingState.TAKE_BISQ_EASY_OFFER);
        sendTakeOfferMessageOverlay = new WizardOverlay(root)
                .headlineIcon(takeOfferSendMessageWaitingAnimation)
                .headline("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.headline")
                .description("bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.subTitle",
                        "bisqEasy.takeOffer.review.sendTakeOfferMessageFeedback.info")
                .build();

        takeOfferSuccessButton = new Button(Res.get("bisqEasy.tradeWizard.review.takeOfferSuccessButton"));
        takeOfferSuccessButton.setDefaultButton(true);
        takeOfferSuccessOverlay = new WizardOverlay(root)
                .info()
                .headline("bisqEasy.tradeWizard.review.takeOfferSuccess.headline")
                .description("bisqEasy.tradeWizard.review.takeOfferSuccess.subTitle")
                .buttons(takeOfferSuccessButton)
                .build();

        StackPane.setMargin(gridPane, new Insets(40));
        root.getChildren().addAll(gridPane, createOfferSuccessOverlay, sendTakeOfferMessageOverlay, takeOfferSuccessOverlay);
    }

    @Override
    protected void onViewAttached() {
        headline.setText(model.getHeadline());
        detailsHeadline.setText(model.getDetailsHeadline());

        priceDescription.setText(model.getPriceDescription());
        TextFlowUtils.updateTextFlow(price, model.getPriceWithCode());
        priceDetails.setText(model.getPriceDetails());

        bitcoinPaymentMethodDescription.setText(model.getBitcoinPaymentMethodDescription());
        bitcoinPaymentMethod.setText(model.getBitcoinPaymentMethod());

        fiatPaymentMethodDescription.setText(model.getFiatPaymentMethodDescription());
        fiatPaymentMethod.setText(model.getFiatPaymentMethod());

        feeDetails.setVisible(model.isFeeDetailsVisible());
        feeDetails.setManaged(model.isFeeDetailsVisible());

        fee.setText(model.getFee());
        feeDetails.setText(model.getFeeDetails());

        createOfferSuccessButton.setOnAction(e -> controller.onShowOfferbook());
        takeOfferSuccessButton.setOnAction(e -> controller.onShowOpenTrades());

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(), shouldShow ->
                createOfferSuccessOverlay.updateOverlayVisibility(gridPane,
                        shouldShow,
                        controller::onKeyPressedWhileShowingOverlay));

        takeOfferStatusPin = EasyBind.subscribe(model.getTakeOfferStatus(), this::showTakeOfferStatusFeedback);

        if (model.getTakersBitcoinPaymentMethods().size() > 1) {
            bitcoinPaymentMethodsComboBox = new ComboBox<>(model.getTakersBitcoinPaymentMethods());
            bitcoinPaymentMethodsComboBox.getStyleClass().add("trade-wizard-review-payment-combo-box");
            GridPane.setMargin(bitcoinPaymentMethodValuePane, new Insets(-8, 0, -8, 0));
            bitcoinPaymentMethodValuePane.getChildren().setAll(bitcoinPaymentMethodsComboBox);
            bitcoinPaymentMethodsComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(BitcoinPaymentMethod method) {
                    return method != null ? method.getShortDisplayString() : "";
                }

                @Override
                public BitcoinPaymentMethod fromString(String string) {
                    return null;
                }
            });

            bitcoinPaymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedBitcoinPaymentMethod());
            bitcoinPaymentMethodsComboBox.setOnAction(e -> {
                if (bitcoinPaymentMethodsComboBox.getSelectionModel().getSelectedItem() == null) {
                    bitcoinPaymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedBitcoinPaymentMethod());
                    return;
                }
                controller.onSelectBitcoinPaymentMethod(bitcoinPaymentMethodsComboBox.getSelectionModel().getSelectedItem());
            });
        } else {
            GridPane.setMargin(bitcoinPaymentMethodValuePane, new Insets(0, 0, 0, 0));
            bitcoinPaymentMethodValuePane.getChildren().setAll(bitcoinPaymentMethod);
        }

        if (model.getTakersFiatPaymentMethods().size() > 1) {
            fiatPaymentMethodsComboBox = new ComboBox<>(model.getTakersFiatPaymentMethods());
            fiatPaymentMethodsComboBox.getStyleClass().add("trade-wizard-review-payment-combo-box");
            GridPane.setMargin(fiatPaymentMethodValuePane, new Insets(-8, 0, -8, 0));
            fiatPaymentMethodValuePane.getChildren().setAll(fiatPaymentMethodsComboBox);
            fiatPaymentMethodsComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(FiatPaymentMethod method) {
                    return method != null ? method.getDisplayString() : "";
                }

                @Override
                public FiatPaymentMethod fromString(String string) {
                    return null;
                }
            });

            fiatPaymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedFiatPaymentMethod());
            fiatPaymentMethodsComboBox.setOnAction(e -> {
                if (fiatPaymentMethodsComboBox.getSelectionModel().getSelectedItem() == null) {
                    fiatPaymentMethodsComboBox.getSelectionModel().select(model.getTakersSelectedFiatPaymentMethod());
                    return;
                }
                controller.onSelectFiatPaymentMethod(fiatPaymentMethodsComboBox.getSelectionModel().getSelectedItem());
            });
        } else {
            GridPane.setMargin(fiatPaymentMethodValuePane, new Insets(0, 0, 0, 0));
            fiatPaymentMethodValuePane.getChildren().setAll(fiatPaymentMethod);
        }
    }

    @Override
    protected void onViewDetached() {
        createOfferSuccessButton.setOnAction(null);
        takeOfferSuccessButton.setOnAction(null);

        showCreateOfferSuccessPin.unsubscribe();
        takeOfferStatusPin.unsubscribe();

        takeOfferSendMessageWaitingAnimation.stop();

        if (bitcoinPaymentMethodsComboBox != null) {
            bitcoinPaymentMethodsComboBox.setOnAction(null);
        }
        if (fiatPaymentMethodsComboBox != null) {
            fiatPaymentMethodsComboBox.setOnAction(null);
        }
    }

    private void showTakeOfferStatusFeedback(TradeWizardReviewModel.TakeOfferStatus status) {
        if (status == TradeWizardReviewModel.TakeOfferStatus.SENT) {
            sendTakeOfferMessageOverlay.setVisible(true);

            Transitions.blurStrong(gridPane, 0);
            Transitions.slideInTop(sendTakeOfferMessageOverlay, 450);
            takeOfferSendMessageWaitingAnimation.playIndefinitely();

            UIScheduler.run(() -> {
                minWaitingTimePassed = true;
                if (model.getTakeOfferStatus().get() == TradeWizardReviewModel.TakeOfferStatus.SUCCESS) {
                    sendTakeOfferMessageOverlay.setVisible(false);
                    takeOfferSuccessOverlay.setVisible(true);
                    takeOfferSendMessageWaitingAnimation.stop();
                }
            }).after(DevMode.isDevMode() ? 500 : 4000);
        } else if (status == TradeWizardReviewModel.TakeOfferStatus.SUCCESS && minWaitingTimePassed) {
            sendTakeOfferMessageOverlay.setVisible(false);
            takeOfferSuccessOverlay.setVisible(true);
            takeOfferSendMessageWaitingAnimation.stop();
        } else if (status == TradeWizardReviewModel.TakeOfferStatus.NOT_STARTED) {
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
