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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount_and_price.price;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.UnorderedList;
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.main.content.bisq_easy.components.PriceInputBox;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.text.DecimalFormat;

@Slf4j
public class TradeWizardPriceView extends View<VBox, TradeWizardPriceModel, TradeWizardPriceController> {
    private static final String SELECTED_PRICE_MODEL_STYLE_CLASS = "selected-model";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");

    private final PriceInputBox percentageInput;
    private final VBox learnWhyOverlay, content;
    private final PriceInput priceInput;
    private final Button percentagePrice, fixedPrice, showLearnWhyButton, closeLearnWhyButton;
    private final Label feedbackSentence, minSliderValue, maxSliderValue;
    private final HBox feedbackBox;
    private final Slider slider;
    private Subscription percentageFocussedPin, useFixPricePin, shouldShowLearnWhyOverlayPin;

    public TradeWizardPriceView(TradeWizardPriceModel model, TradeWizardPriceController controller, PriceInput priceInput) {
        super(new VBox(), model, controller);

        this.priceInput = priceInput;

        root.setAlignment(Pos.TOP_CENTER);

        // Pricing model selection
        percentagePrice = new Button(Res.get("bisqEasy.price.percentage.title"));
        percentagePrice.getStyleClass().add("model-selection-item");
        fixedPrice = new Button(Res.get("bisqEasy.price.tradePrice.title"));
        fixedPrice.getStyleClass().add("model-selection-item");
        Label separator = new Label("|");

        HBox percentagePriceBox = new HBox(percentagePrice);
        percentagePriceBox.getStyleClass().add("model-selection-item-box");
        percentagePriceBox.setAlignment(Pos.CENTER_RIGHT);
        HBox fixedPriceBox = new HBox(fixedPrice);
        fixedPriceBox.getStyleClass().add("model-selection-item-box");
        fixedPriceBox.setAlignment(Pos.CENTER_LEFT);

        HBox pricingModels = new HBox(30, percentagePriceBox, separator, fixedPriceBox);
        pricingModels.getStyleClass().addAll("selection-models", "bisq-text-3");

        // Input box
        percentageInput = new PriceInputBox(Res.get("bisqEasy.price.percentage.inputBoxText"),
                BisqEasyViewUtils.NUMERIC_WITH_DECIMAL_REGEX);
        percentageInput.setValidator(new PercentageValidator());
        percentageInput.textInputSymbolTextProperty().set("%");
        VBox fieldsBox = new VBox(20, priceInput.getRoot(), percentageInput);
        fieldsBox.setAlignment(Pos.TOP_CENTER);
        fieldsBox.setMinWidth(340);
        fieldsBox.setPrefWidth(340);
        fieldsBox.setMaxWidth(340);

        // Slider
        slider = new Slider();
        slider.setMin(model.getSliderMin());
        slider.setMax(model.getSliderMax());
        slider.getStyleClass().add("price-slider");

        minSliderValue = new Label();
        minSliderValue.getStyleClass().add("range-value");
        minSliderValue.setAlignment(Pos.BASELINE_LEFT);

        maxSliderValue = new Label();
        maxSliderValue.getStyleClass().add("range-value");
        maxSliderValue.setAlignment(Pos.BASELINE_RIGHT);
        HBox sliderIndicators = new HBox(minSliderValue, Spacer.fillHBox(), maxSliderValue);
        VBox.setMargin(sliderIndicators, new Insets(2.5, 0, 0, 0));
        VBox sliderBox = new VBox(2, slider, sliderIndicators);

        // Feedback sentence
        feedbackSentence = new Label();
        feedbackSentence.getStyleClass().add("bisq-text-3");
        showLearnWhyButton = new Button(Res.get("bisqEasy.price.feedback.learnWhySection.openButton"));
        showLearnWhyButton.getStyleClass().add("learn-why-button");
        feedbackBox = new HBox(5, feedbackSentence, showLearnWhyButton);
        feedbackBox.getStyleClass().add("feedback-box");

        // Overlay
        closeLearnWhyButton = new Button(Res.get("bisqEasy.price.feedback.learnWhySection.closeButton"));
        learnWhyOverlay = createAndGetLearnWhyOverlay();

        VBox.setMargin(sliderBox, new Insets(22.5, 0, 0, 0));
        content = new VBox(10, pricingModels, fieldsBox, sliderBox/*, feedbackBox*/);
        content.getStyleClass().add("price-content");
        StackPane layeredContent = new StackPane(content, learnWhyOverlay);
        layeredContent.getStyleClass().add("bisq-easy-trade-wizard-price-step");
        root.getChildren().addAll(layeredContent);
        root.getStyleClass().add("bisq-easy-trade-wizard-price-step-pane");
    }

    @Override
    protected void onViewAttached() {
        minSliderValue.setText(DECIMAL_FORMAT.format(model.getMinPercentage() * 100) + "%");
        maxSliderValue.setText(DECIMAL_FORMAT.format(model.getMaxPercentage() * 100) + "%");
        percentageInput.textProperty().bindBidirectional(model.getPercentageInput());
        percentageInput.conversionPriceTextProperty().bind(model.getPriceAsString());
        percentageInput.conversionPriceSymbolTextProperty().set(model.getMarket().getMarketCodes());
        percentageInput.initialize();
        feedbackSentence.textProperty().bind(model.getFeedbackSentence());
        feedbackBox.visibleProperty().bind(model.getShouldShowFeedback());
        feedbackBox.managedProperty().bind(model.getShouldShowFeedback());
        slider.valueProperty().bindBidirectional(model.getPriceSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());

        percentageFocussedPin = EasyBind.subscribe(percentageInput.textInputFocusedProperty(), controller::onPercentageFocussed);

        useFixPricePin = EasyBind.subscribe(model.getUseFixPrice(), useFixPrice ->
                UIScheduler.run(this::updateFieldsBox).after(100));

        shouldShowLearnWhyOverlayPin = EasyBind.subscribe(model.getShouldShowLearnWhyOverlay(), showOverlay -> UIScheduler.run(() -> {
            if (showOverlay) {
                learnWhyOverlay.setVisible(true);
                learnWhyOverlay.setManaged(true);
                Transitions.blurStrong(content, 0);
                Transitions.slideInTop(learnWhyOverlay, 450);
            } else {
                learnWhyOverlay.setVisible(false);
                learnWhyOverlay.setManaged(false);
                Transitions.removeEffect(content);
            }
        }).after(100));

        percentagePrice.setOnAction(e -> controller.usePercentagePrice());
        fixedPrice.setOnAction(e -> controller.useFixedPrice());
        showLearnWhyButton.setOnAction(e -> controller.showLearnWhySection());
        closeLearnWhyButton.setOnAction(e -> controller.closeLearnWhySection());

        // Needed to trigger focusOut event on amount components
        // We handle all parents mouse events.
        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(e -> root.requestFocus());
            node = node.getParent();
        }
    }

    @Override
    protected void onViewDetached() {
        percentageInput.textProperty().unbindBidirectional(model.getPercentageInput());
        percentageInput.conversionPriceTextProperty().unbind();
        percentageInput.dispose();
        feedbackSentence.textProperty().unbind();
        feedbackBox.visibleProperty().unbind();
        feedbackBox.managedProperty().unbind();
        slider.valueProperty().unbindBidirectional(model.getPriceSliderValue());
        model.getSliderFocus().unbind();

        percentageFocussedPin.unsubscribe();
        useFixPricePin.unsubscribe();
        shouldShowLearnWhyOverlayPin.unsubscribe();

        percentagePrice.setOnAction(null);
        fixedPrice.setOnAction(null);
        showLearnWhyButton.setOnAction(null);
        closeLearnWhyButton.setOnAction(null);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }

    private void updateFieldsBox() {
        fixedPrice.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
        percentagePrice.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
        if (model.getUseFixPrice().get()) {
            fixedPrice.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            priceInput.getRoot().visibleProperty().set(true);
            priceInput.getRoot().managedProperty().set(true);
            percentageInput.visibleProperty().set(false);
            percentageInput.managedProperty().set(false);
            percentageInput.deselect();
            percentageInput.setEditable(false);
            percentageInput.resetValidation();
            priceInput.setEditable(true);
            priceInput.requestFocusWithCursor();
        } else {
            percentagePrice.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            priceInput.getRoot().visibleProperty().set(false);
            priceInput.getRoot().managedProperty().set(false);
            percentageInput.visibleProperty().set(true);
            percentageInput.managedProperty().set(true);
            priceInput.deselect();
            priceInput.setEditable(false);
            priceInput.resetValidation();
            percentageInput.setEditable(true);
            percentageInput.requestFocusWithCursor();
        }
    }

    private VBox createAndGetLearnWhyOverlay() {
        Label learnWhyTitle = new Label(Res.get("bisqEasy.price.feedback.learnWhySection.title"));
        learnWhyTitle.getStyleClass().addAll("learn-why-title-label", "large-text");
        Label learnWhyIntroLabel = new Label(Res.get("bisqEasy.price.feedback.learnWhySection.description.intro"));
        learnWhyIntroLabel.getStyleClass().addAll("learn-why-text", "learn-why-intro-label");
        UnorderedList learnWhyExpositionList = new UnorderedList(Res.get("bisqEasy.price.feedback.learnWhySection.description.exposition"),
                "learn-why-text", 7, 10, "- ", "- ");

        VBox overlay = new VBox(10, learnWhyTitle, learnWhyIntroLabel, learnWhyExpositionList, closeLearnWhyButton);
        overlay.getStyleClass().addAll("trade-wizard-feedback-bg", "learn-why-overlay");
        StackPane.setAlignment(overlay, Pos.TOP_CENTER);
        StackPane.setMargin(overlay, new Insets(-63, 0, 0, 0));
        return overlay;
    }
}
