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

import bisq.desktop.common.Icons;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.UnorderedList;
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.bisq_easy.components.PriceInputBox;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.text.DecimalFormat;

@Slf4j
public class TradeWizardPriceView extends View<VBox, TradeWizardPriceModel, TradeWizardPriceController> {
    private static final String SELECTED_PRICE_MODEL_STYLE_CLASS = "selected-model";
    private static final String PRICE_SLIDER_BUYER_STYLE_CLASS = "price-slider-buyer";
    private static final String PRICE_SLIDER_SELLER_STYLE_CLASS = "price-slider-seller";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("00");

    private final PriceInputBox percentageInputBox;
    @Getter
    private final WizardOverlay overlay;
    private final Pane priceInputBox;
    private final Button percentagePriceButton, fixedPriceButton, closeOverlayButton;
    private final Label warningIcon, feedbackSentence, minSliderValue, maxSliderValue;
    private final Slider slider;
    private final Hyperlink showLearnWhyButton;
    private final ImageView percentagePriceIconGreen, percentagePriceIconGrey, fixedPriceIconGreen, fixedPriceIconGrey;
    private final Circle marketPriceMarker;
    private final BisqTooltip marketPriceMarkerTooltip = new BisqTooltip();
    private Subscription percentageFocusedPin, useFixPricePin, isOverlayVisible;

    public TradeWizardPriceView(TradeWizardPriceModel model,
                                TradeWizardPriceController controller,
                                Pane priceInput) {
        super(new VBox(10), model, controller);

        this.priceInputBox = priceInput;

        // Pricing model selection
        percentagePriceButton = new Button(Res.get("bisqEasy.price.percentage.title"));
        percentagePriceButton.getStyleClass().add("model-selection-item");
        percentagePriceButton.setGraphicTextGap(8);
        percentagePriceIconGreen = ImageUtil.getImageViewById("chart-icon-green");
        percentagePriceIconGrey = ImageUtil.getImageViewById("chart-icon-grey");
        fixedPriceButton = new Button(Res.get("bisqEasy.price.tradePrice.title"));
        fixedPriceButton.getStyleClass().add("model-selection-item");
        fixedPriceButton.setGraphicTextGap(8);
        fixedPriceIconGreen = ImageUtil.getImageViewById("lock-icon-green");
        fixedPriceIconGrey = ImageUtil.getImageViewById("lock-icon-grey");
        Label separator = new Label("|");

        HBox percentagePriceBox = new HBox(percentagePriceButton);
        percentagePriceBox.getStyleClass().add("model-selection-item-box");
        percentagePriceBox.setAlignment(Pos.CENTER_RIGHT);
        HBox fixedPriceBox = new HBox(fixedPriceButton);
        fixedPriceBox.getStyleClass().add("model-selection-item-box");
        fixedPriceBox.setAlignment(Pos.CENTER_LEFT);

        HBox pricingModels = new HBox(30, percentagePriceBox, separator, fixedPriceBox);
        pricingModels.getStyleClass().addAll("selection-models", "bisq-text-3");

        // Input box
        percentageInputBox = new PriceInputBox(Res.get("bisqEasy.price.percentage.inputBoxText"),
                BisqEasyViewUtils.NUMERIC_WITH_DECIMAL_REGEX);
        percentageInputBox.setValidator(new PercentageValidator());
        percentageInputBox.textInputSymbolTextProperty().set("%");
        VBox fieldsBox = new VBox(20, priceInput, percentageInputBox);
        fieldsBox.setAlignment(Pos.TOP_CENTER);
        fieldsBox.setMinWidth(model.getPriceComponentWidth());
        fieldsBox.setPrefWidth(model.getPriceComponentWidth());
        fieldsBox.setMaxWidth(model.getPriceComponentWidth());

        // Slider
        slider = new Slider();
        slider.setMin(model.getSliderMin());
        slider.setMax(model.getSliderMax());

        minSliderValue = new Label();
        minSliderValue.getStyleClass().add("range-value");
        minSliderValue.setAlignment(Pos.BASELINE_LEFT);

        maxSliderValue = new Label();
        maxSliderValue.getStyleClass().add("range-value");
        maxSliderValue.setAlignment(Pos.BASELINE_RIGHT);

        marketPriceMarker = new Circle(2.5);
        marketPriceMarker.getStyleClass().add("market-price-marker");
        marketPriceMarker.setLayoutY(7.5);

        HBox sliderIndicators = new HBox(minSliderValue, Spacer.fillHBox(), maxSliderValue);
        VBox.setMargin(sliderIndicators, new Insets(2.5, 0, 0, 0));
        VBox sliderBox = new VBox(2, slider, sliderIndicators);
        sliderBox.setMinWidth(model.getPriceComponentWidth());
        sliderBox.setPrefWidth(model.getPriceComponentWidth());
        sliderBox.setMaxWidth(model.getPriceComponentWidth());

        Pane sliderBoxAndMarketPriceMarker = new Pane(sliderBox, marketPriceMarker);

        // Feedback sentence
        warningIcon = new Label();
        warningIcon.getStyleClass().add("text-fill-grey-dimmed");
        warningIcon.setPadding(new Insets(0, 2.5, 0, 0));
        warningIcon.setMinWidth(Label.USE_PREF_SIZE);
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1em");

        feedbackSentence = new Label();
        feedbackSentence.getStyleClass().add("trade-wizard-amount-limit-info");

        showLearnWhyButton = new Hyperlink(Res.get("bisqEasy.price.feedback.learnWhySection.openButton"));
        showLearnWhyButton.getStyleClass().add("trade-wizard-amount-limit-info-overlay-link");
        showLearnWhyButton.setMinWidth(Hyperlink.USE_PREF_SIZE);

        HBox feedbackBox = new HBox(2.5, warningIcon, feedbackSentence, showLearnWhyButton);
        feedbackBox.setAlignment(Pos.CENTER);

        // Overlay
        Label learnWhyIntroLabel = new Label(Res.get("bisqEasy.price.feedback.learnWhySection.description.intro"));
        learnWhyIntroLabel.getStyleClass().addAll("learn-why-text", "wrap-text");
        UnorderedList learnWhyExpositionList = new UnorderedList(Res.get("bisqEasy.price.feedback.learnWhySection.description.exposition"),
                "learn-why-text", 7, 10, "- ", "- ");
        closeOverlayButton = new Button(Res.get("bisqEasy.price.feedback.learnWhySection.closeButton"));
        overlay = new WizardOverlay(root,
                "bisqEasy.price.feedback.learnWhySection.title",
                new VBox(40, learnWhyIntroLabel, learnWhyExpositionList),
                closeOverlayButton);

        VBox.setMargin(sliderBoxAndMarketPriceMarker, new Insets(22.5, 0, 0, 0));
        root.setAlignment(Pos.TOP_CENTER);
        root.getChildren().addAll(pricingModels, fieldsBox, sliderBoxAndMarketPriceMarker, feedbackBox);
        root.getStyleClass().add("bisq-easy-trade-wizard-price-step");
    }

    @Override
    protected void onViewAttached() {
        minSliderValue.setText(DECIMAL_FORMAT.format(model.getMinPercentage() * 100) + "%");
        maxSliderValue.setText(DECIMAL_FORMAT.format(model.getMaxPercentage() * 100) + "%");
        marketPriceMarker.setLayoutX(model.getMarketPriceMarkerLayoutX());
        marketPriceMarkerTooltip.setText(Res.get("bisqEasy.price.slider.marketPriceMarker"));
        Tooltip.install(marketPriceMarker, marketPriceMarkerTooltip);

        priceInputBox.visibleProperty().bind(model.getUseFixPrice());
        priceInputBox.managedProperty().bind(model.getUseFixPrice());
        percentageInputBox.visibleProperty().bind(model.getUseFixPrice().not());
        percentageInputBox.managedProperty().bind(model.getUseFixPrice().not());
        percentageInputBox.textProperty().bindBidirectional(model.getPercentageInput());
        percentageInputBox.conversionPriceTextProperty().bind(model.getPriceAsString());
        percentageInputBox.conversionPriceSymbolTextProperty().set(model.getMarket().getMarketCodes());
        percentageInputBox.initialize();
        feedbackSentence.textProperty().bind(model.getFeedbackSentence());
        warningIcon.visibleProperty().bind(model.getShouldShowWarningIcon());
        warningIcon.managedProperty().bind(model.getShouldShowWarningIcon());
        showLearnWhyButton.visibleProperty().bind(model.getShouldShowLearnWhyButton());
        showLearnWhyButton.managedProperty().bind(model.getShouldShowLearnWhyButton());
        slider.valueProperty().bindBidirectional(model.getPriceSliderValue());
        slider.getStyleClass().add(model.getDirection().isSell() ? PRICE_SLIDER_SELLER_STYLE_CLASS : PRICE_SLIDER_BUYER_STYLE_CLASS);
        model.getSliderFocus().bind(slider.focusedProperty());

        percentageFocusedPin = EasyBind.subscribe(percentageInputBox.textInputFocusedProperty(), controller::onPercentageFocused);

        useFixPricePin = EasyBind.subscribe(model.getUseFixPrice(), useFixPrice ->
                UIScheduler.run(this::updatePriceSpec).after(100));

        isOverlayVisible = EasyBind.subscribe(model.getIsOverlayVisible(), isOverlayVisible -> {
            if (isOverlayVisible) {
                root.setOnKeyPressed(controller::onKeyPressedWhileShowingOverlay);
            } else {
                root.setOnKeyPressed(null);
            }
        });

        percentagePriceButton.setOnAction(e -> controller.usePercentagePrice());
        fixedPriceButton.setOnAction(e -> controller.useFixedPrice());
        showLearnWhyButton.setOnAction(e -> controller.onShowOverlay());
        closeOverlayButton.setOnAction(e -> controller.onCloseOverlay());
        marketPriceMarker.setOnMouseClicked(e -> controller.onMarketPriceMarkerClicked());

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
        Tooltip.uninstall(marketPriceMarker, marketPriceMarkerTooltip);

        percentageInputBox.textProperty().unbindBidirectional(model.getPercentageInput());
        percentageInputBox.conversionPriceTextProperty().unbind();
        percentageInputBox.dispose();
        feedbackSentence.textProperty().unbind();
        showLearnWhyButton.visibleProperty().unbind();
        showLearnWhyButton.managedProperty().unbind();
        warningIcon.visibleProperty().unbind();
        warningIcon.managedProperty().unbind();
        slider.valueProperty().unbindBidirectional(model.getPriceSliderValue());
        slider.getStyleClass().removeAll(PRICE_SLIDER_BUYER_STYLE_CLASS, PRICE_SLIDER_SELLER_STYLE_CLASS);
        model.getSliderFocus().unbind();

        percentageFocusedPin.unsubscribe();
        useFixPricePin.unsubscribe();
        isOverlayVisible.unsubscribe();

        percentagePriceButton.setOnAction(null);
        fixedPriceButton.setOnAction(null);
        showLearnWhyButton.setOnAction(null);
        closeOverlayButton.setOnAction(null);
        marketPriceMarker.setOnMouseClicked(null);

        root.setOnKeyPressed(null);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }

    private void updatePriceSpec() {
        updatePriceSpecButtonsStyle();
        updatePercentagePrice();
        controller.onUpdatePriceSpec();
        controller.onPriceComponentUpdated();
    }

    private void updatePercentagePrice() {
        if (model.getUseFixPrice().get()) {
            percentageInputBox.deactivate();
        } else {
            boolean shouldRequestFocus = model.isShouldFocusPriceComponent();
            percentageInputBox.activate(shouldRequestFocus);
        }
    }

    private void updatePriceSpecButtonsStyle() {
        fixedPriceButton.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
        percentagePriceButton.getStyleClass().remove(SELECTED_PRICE_MODEL_STYLE_CLASS);
        if (model.getUseFixPrice().get()) {
            fixedPriceButton.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            fixedPriceButton.setGraphic(fixedPriceIconGreen);
            percentagePriceButton.setGraphic(percentagePriceIconGrey);
        } else {
            percentagePriceButton.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            percentagePriceButton.setGraphic(percentagePriceIconGreen);
            fixedPriceButton.setGraphic(fixedPriceIconGrey);
        }
    }
}
