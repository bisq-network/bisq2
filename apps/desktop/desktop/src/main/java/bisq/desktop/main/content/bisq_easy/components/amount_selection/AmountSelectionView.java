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

package bisq.desktop.main.content.bisq_easy.components.amount_selection;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class AmountSelectionView extends View<VBox, AmountSelectionModel, AmountSelectionController> {
    private static final int RANGE_INPUT_TEXT_MAX_LENGTH = 11;
    private static final int FIXED_INPUT_TEXT_MAX_LENGTH = 18;
    private static final Insets SLIDER_INDICATORS_RANGE_MARGIN = new Insets(-15, 0, 0, 0);
    private static final Insets SLIDER_INDICATORS_FIXED_MARGIN = new Insets(2.5, 0, 0, 0);
    private static final String INPUT_TEXT_9_STYLE_CLASS = "input-text-9";
    private static final String INPUT_TEXT_10_STYLE_CLASS = "input-text-10";
    private static final String INPUT_TEXT_11_STYLE_CLASS = "input-text-11";
    private static final String INPUT_TEXT_12_STYLE_CLASS = "input-text-12";
    private static final String INPUT_TEXT_13_STYLE_CLASS = "input-text-13";
    private static final String INPUT_TEXT_14_STYLE_CLASS = "input-text-14";
    private static final String INPUT_TEXT_15_STYLE_CLASS = "input-text-15";
    private static final String INPUT_TEXT_16_STYLE_CLASS = "input-text-16";
    private static final String INPUT_TEXT_17_STYLE_CLASS = "input-text-17";
    private static final String INPUT_TEXT_18_STYLE_CLASS = "input-text-18";
    private static final String INPUT_TEXT_19_STYLE_CLASS = "input-text-19";
    private static final String INPUT_TEXT_20_STYLE_CLASS = "input-text-20";
    private static final String INPUT_TEXT_21_STYLE_CLASS = "input-text-21";
    private static final String INPUT_TEXT_22_STYLE_CLASS = "input-text-22";
    private static final String INPUT_TEXT_23_STYLE_CLASS = "input-text-23";
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String EN_DASH_SYMBOL = "\u2013"; // Unicode for "–"

    private final Slider maxOrFixedAmountSlider, minAmountSlider;
    private final Label minRangeValue, maxRangeValue, minRangeCode, maxRangeCode, description, quoteAmountSeparator,
            baseAmountSeparator;
    private final Region selectionLine;
    private final BigNumberAmountInputBox maxOrFixedQuoteAmount, minQuoteAmount, invertedMaxOrFixedBaseAmount, invertedMinBaseAmount;
    private final Pane minQuoteAmountRoot, minBaseAmountRoot, invertedMinQuoteAmountRoot, invertedMinBaseAmountRoot,
            maxOrFixedBaseAmountRoot, maxOrFixedQuoteAmountRoot, invertedMaxOrFixedQuoteAmountRoot, invertedMaxOrFixedBaseAmountRoot;
    private final HBox quoteAmountSelectionHBox, baseAmountSelectionHBox, sliderIndicators;
    private final ChangeListener<Number> maxOrFixedAmountSliderValueListener, minAmountSliderValueListener;
    private final BisqMenuItem flipCurrenciesButton;
    private Subscription maxOrFixedQuoteAmountFocusPin, minQuoteAmountFocusPin, sliderTrackStylePin, isRangeAmountEnabledPin,
            maxOrFixedQuoteAmountLengthPin, minQuoteAmountLengthPin;

    AmountSelectionView(AmountSelectionModel model,
                        AmountSelectionController controller,
                        SmallNumberDisplayBox maxOrFixedBaseAmount,
                        BigNumberAmountInputBox maxOrFixedQuoteAmount,
                        SmallNumberDisplayBox invertedMaxOrFixedQuoteAmount,
                        BigNumberAmountInputBox invertedMaxOrFixedBaseAmount,
                        SmallNumberDisplayBox minBaseAmount,
                        BigNumberAmountInputBox minQuoteAmount,
                        SmallNumberDisplayBox invertedMinQuoteAmount,
                        BigNumberAmountInputBox invertedMinBaseAmount) {
        super(new VBox(10), model, controller);

        // max or fixed component
        maxOrFixedBaseAmountRoot = maxOrFixedBaseAmount.getRoot();
        maxOrFixedQuoteAmountRoot = maxOrFixedQuoteAmount.getRoot();
        this.maxOrFixedQuoteAmount = maxOrFixedQuoteAmount;
        // inverted ones
        invertedMaxOrFixedQuoteAmountRoot = invertedMaxOrFixedQuoteAmount.getRoot();
        invertedMaxOrFixedBaseAmountRoot = invertedMaxOrFixedBaseAmount.getRoot();
        this.invertedMaxOrFixedBaseAmount = invertedMaxOrFixedBaseAmount;

        // min component (only shown when using a range)
        minBaseAmountRoot = minBaseAmount.getRoot();
        minQuoteAmountRoot = minQuoteAmount.getRoot();
        this.minQuoteAmount = minQuoteAmount;
        // inverted ones
        invertedMinQuoteAmountRoot = invertedMinQuoteAmount.getRoot();
        invertedMinBaseAmountRoot = invertedMinBaseAmount.getRoot();
        this.invertedMinBaseAmount = invertedMinBaseAmount;

        // quote amount selection
        quoteAmountSeparator = new Label(EN_DASH_SYMBOL);
        quoteAmountSeparator.getStyleClass().add("quote-separator");
        minQuoteAmountRoot.getStyleClass().add("min-quote-amount");
        maxOrFixedQuoteAmountRoot.getStyleClass().add("max-or-fixed-quote-amount");
        quoteAmountSelectionHBox = new HBox(5, minQuoteAmountRoot, invertedMinBaseAmountRoot, quoteAmountSeparator,
                maxOrFixedQuoteAmountRoot, invertedMaxOrFixedBaseAmountRoot);
        quoteAmountSelectionHBox.getStyleClass().add("quote-amount");
        quoteAmountSelectionHBox.setMaxWidth(model.getAmountBoxWidth() + 40);
        quoteAmountSelectionHBox.setMinWidth(model.getAmountBoxWidth() + 40);
        quoteAmountSelectionHBox.setLayoutY(0);
        quoteAmountSelectionHBox.setMinHeight(70);
        quoteAmountSelectionHBox.setMaxHeight(70);
        quoteAmountSelectionHBox.setPadding(new Insets(0, 20, 0, 20));

        // base amount selection
        baseAmountSeparator = new Label(EN_DASH_SYMBOL);
        baseAmountSeparator.getStyleClass().add("base-separator");
        minBaseAmountRoot.getStyleClass().add("min-base-amount");
        maxOrFixedBaseAmountRoot.getStyleClass().add("max-or-fixed-base-amount");
        baseAmountSelectionHBox = new HBox(minBaseAmountRoot, invertedMinQuoteAmountRoot, baseAmountSeparator,
                maxOrFixedBaseAmountRoot, invertedMaxOrFixedQuoteAmountRoot);
        baseAmountSelectionHBox.getStyleClass().add("base-amount");
        HBox.setHgrow(maxOrFixedBaseAmountRoot, Priority.ALWAYS);

        flipCurrenciesButton = new BisqMenuItem("flip-fields-arrows-green", "flip-fields-arrows-white");
        HBox baseAmountAndFlippingButtonHBox = new HBox(baseAmountSelectionHBox, Spacer.fillHBox(), flipCurrenciesButton);
        baseAmountAndFlippingButtonHBox.setLayoutY(70);
        baseAmountAndFlippingButtonHBox.setPadding(new Insets(0, 10, 0, 20));

        // rest of the component
        description = new Label();
        description.setMouseTransparent(true);
        description.setPadding(new Insets(0, 0, 0, 20));

        Pane amountInputVBox = new Pane(quoteAmountSelectionHBox, baseAmountAndFlippingButtonHBox);
        amountInputVBox.setMinHeight(model.getAmountBoxHeight() - 30);
        amountInputVBox.setMaxHeight(model.getAmountBoxHeight() - 30);
        amountInputVBox.getStyleClass().add("amount-input");

        VBox descriptionAndAmountVBox = new VBox(0, description, Spacer.fillVBox(), amountInputVBox);
        descriptionAndAmountVBox.getStyleClass().add("bisq-dual-amount-bg");
        descriptionAndAmountVBox.setPadding(new Insets(8, 0, 8, 0));

        Region line = new Region();
        line.setPrefHeight(1);
        line.setPrefWidth(model.getAmountBoxWidth() + 40); // plus 40 for the paddings
        line.setLayoutY(model.getAmountBoxHeight() + 6);
        line.setStyle("-fx-background-color: -bisq-mid-grey-20");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.getStyleClass().add("material-text-field-selection-line");
        selectionLine.setPrefHeight(2);
        selectionLine.setPrefWidth(0);
        selectionLine.setLayoutY(model.getAmountBoxHeight() + 5);
        selectionLine.setMouseTransparent(true);

        Pane amountPane = new Pane(descriptionAndAmountVBox, line, selectionLine);
        amountPane.setMaxWidth(model.getAmountBoxWidth() + 40);
        amountPane.setMinHeight(model.getAmountBoxHeight());
        amountPane.setMaxHeight(model.getAmountBoxHeight());

        // slider
        maxOrFixedAmountSlider = new Slider();
        maxOrFixedAmountSlider.setMin(model.getSliderMin());
        maxOrFixedAmountSlider.setMax(model.getSliderMax());
        maxOrFixedAmountSlider.getStyleClass().add("max-or-fixed-amount-slider");

        minAmountSlider = new Slider();
        minAmountSlider.setMin(model.getSliderMin());
        minAmountSlider.setMax(model.getSliderMax());
        minAmountSlider.getStyleClass().add("min-amount-slider");

        minRangeValue = new Label();
        minRangeValue.getStyleClass().add("range-value");
        minRangeCode = new Label();
        minRangeCode.getStyleClass().add("range-code");
        HBox minRangeValueAndCodeHBox = new HBox(2, minRangeValue, minRangeCode);
        minRangeValueAndCodeHBox.setAlignment(Pos.BASELINE_LEFT);

        maxRangeValue = new Label();
        maxRangeValue.getStyleClass().add("range-value");
        maxRangeCode = new Label();
        maxRangeCode.getStyleClass().add("range-code");
        HBox maxRangeValueAndCodeHBox = new HBox(2, maxRangeValue, maxRangeCode);
        maxRangeValueAndCodeHBox.setAlignment(Pos.BASELINE_RIGHT);

        sliderIndicators = new HBox(minRangeValueAndCodeHBox, Spacer.fillHBox(), maxRangeValueAndCodeHBox);
        VBox sliderBox = new VBox(2, maxOrFixedAmountSlider, minAmountSlider, sliderIndicators);
        sliderBox.setMaxWidth(model.getAmountBoxWidth() + 40);

        VBox.setMargin(sliderBox, new Insets(30, 0, 0, 0));
        root.getChildren().addAll(amountPane, sliderBox);
        root.setAlignment(Pos.TOP_CENTER);

        maxOrFixedAmountSliderValueListener = (observable, oldValue, newValue) -> {
            double maxAllowedSliderValue = controller.getMaxAllowedSliderValue();
            maxOrFixedAmountSlider.setValue(Math.min(newValue.doubleValue(), maxAllowedSliderValue));
        };
        minAmountSliderValueListener = (observable, oldValue, newValue) -> {
            double maxAllowedSliderValue = controller.getMaxAllowedSliderValue();
            minAmountSlider.setValue(Math.min(newValue.doubleValue(), maxAllowedSliderValue));
        };
    }

    @Override
    protected void onViewAttached() {
        UIScheduler.run(() -> {
            maxOrFixedQuoteAmount.requestFocus();
            maxOrFixedQuoteAmountFocusPin = EasyBind.subscribe(maxOrFixedQuoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(minQuoteAmount.focusedProperty().get(), focus));
            minQuoteAmountFocusPin = EasyBind.subscribe(minQuoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(maxOrFixedQuoteAmount.focusedProperty().get(), focus));
            maxOrFixedQuoteAmountLengthPin = EasyBind.subscribe(maxOrFixedQuoteAmount.lengthProperty(), length -> {
                applyTextInputFontStyle();
                applyTextInputPrefWidth();
            });
            minQuoteAmountLengthPin = EasyBind.subscribe(minQuoteAmount.lengthProperty(), length -> {
                applyTextInputFontStyle();
                applyTextInputPrefWidth();
            });
        }).after(700);

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            root.getStyleClass().clear();
            root.getStyleClass().add("amount-selection");
            root.getStyleClass().add(isRangeAmountEnabled ? "range-amount" : "fixed-amount");
            VBox.setMargin(sliderIndicators, isRangeAmountEnabled ? SLIDER_INDICATORS_RANGE_MARGIN : SLIDER_INDICATORS_FIXED_MARGIN);
            maxOrFixedQuoteAmount.setTextInputMaxCharCount(isRangeAmountEnabled ? RANGE_INPUT_TEXT_MAX_LENGTH : FIXED_INPUT_TEXT_MAX_LENGTH);
            minQuoteAmount.setTextInputMaxCharCount(RANGE_INPUT_TEXT_MAX_LENGTH);
            applyTextInputFontStyle();
            applyTextInputPrefWidth();
            deselectAll();
            UIScheduler.run(maxOrFixedQuoteAmount::requestFocus).after(100);
        });
        sliderTrackStylePin = EasyBind.subscribe(model.getSliderTrackStyle(), maxOrFixedAmountSlider::setStyle);

        maxOrFixedAmountSlider.valueProperty().bindBidirectional(model.getMaxOrFixedAmountSliderValue());
        maxOrFixedAmountSlider.valueProperty().addListener(maxOrFixedAmountSliderValueListener);
        minAmountSlider.valueProperty().bindBidirectional(model.getMinAmountSliderValue());
        minAmountSlider.valueProperty().addListener(minAmountSliderValueListener);
        model.getMaxOrFixedAmountSliderFocus().bind(maxOrFixedAmountSlider.focusedProperty());
        model.getMinAmountSliderFocus().bind(minAmountSlider.focusedProperty());
        description.textProperty().bind(model.getDescription());
        minRangeValue.textProperty().bind(model.getMinRangeValueAsString());
        minRangeCode.textProperty().bind(model.getMinRangeCodeAsString());
        maxRangeValue.textProperty().bind(model.getMaxRangeValueLimitationAsString());
        maxRangeCode.textProperty().bind(model.getMaxRangeCodeAsString());
        quoteAmountSeparator.visibleProperty().bind(model.getIsRangeAmountEnabled());
        quoteAmountSeparator.managedProperty().bind(model.getIsRangeAmountEnabled());
        baseAmountSeparator.visibleProperty().bind(model.getIsRangeAmountEnabled());
        baseAmountSeparator.managedProperty().bind(model.getIsRangeAmountEnabled());
        minQuoteAmountRoot.visibleProperty().bind(model.getShouldShowMinAmounts());
        minQuoteAmountRoot.managedProperty().bind(model.getShouldShowMinAmounts());
        minBaseAmountRoot.visibleProperty().bind(model.getShouldShowMinAmounts());
        minBaseAmountRoot.managedProperty().bind(model.getShouldShowMinAmounts());
        invertedMinQuoteAmountRoot.visibleProperty().bind(model.getShouldShowInvertedMinAmounts());
        invertedMinQuoteAmountRoot.managedProperty().bind(model.getShouldShowInvertedMinAmounts());
        invertedMinBaseAmountRoot.visibleProperty().bind(model.getShouldShowInvertedMinAmounts());
        invertedMinBaseAmountRoot.managedProperty().bind(model.getShouldShowInvertedMinAmounts());
        minAmountSlider.visibleProperty().bind(model.getIsRangeAmountEnabled());
        minAmountSlider.managedProperty().bind(model.getIsRangeAmountEnabled());
        flipCurrenciesButton.visibleProperty().bind(model.getAllowInvertingBaseAndQuoteCurrencies());
        flipCurrenciesButton.managedProperty().bind(model.getAllowInvertingBaseAndQuoteCurrencies());
        baseAmountSelectionHBox.minWidthProperty().bind(model.getBaseAmountSelectionHBoxWidth());
        baseAmountSelectionHBox.maxWidthProperty().bind(model.getBaseAmountSelectionHBoxWidth());
        maxOrFixedBaseAmountRoot.visibleProperty().bind(model.getShouldShowMaxOrFixedAmounts());
        maxOrFixedBaseAmountRoot.managedProperty().bind(model.getShouldShowMaxOrFixedAmounts());
        maxOrFixedQuoteAmountRoot.visibleProperty().bind(model.getShouldShowMaxOrFixedAmounts());
        maxOrFixedQuoteAmountRoot.managedProperty().bind(model.getShouldShowMaxOrFixedAmounts());
        invertedMaxOrFixedQuoteAmountRoot.visibleProperty().bind(model.getShouldShowInvertedMaxOrFixedAmounts());
        invertedMaxOrFixedQuoteAmountRoot.managedProperty().bind(model.getShouldShowInvertedMaxOrFixedAmounts());
        invertedMaxOrFixedBaseAmountRoot.visibleProperty().bind(model.getShouldShowInvertedMaxOrFixedAmounts());
        invertedMaxOrFixedBaseAmountRoot.managedProperty().bind(model.getShouldShowInvertedMaxOrFixedAmounts());

        flipCurrenciesButton.setOnAction(e -> controller.onClickFlipCurrenciesButton());

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
        if (maxOrFixedQuoteAmountFocusPin != null) {
            maxOrFixedQuoteAmountFocusPin.unsubscribe();
        }
        if (minQuoteAmountFocusPin != null) {
            minQuoteAmountFocusPin.unsubscribe();
        }
        if (maxOrFixedQuoteAmountLengthPin != null) {
            maxOrFixedQuoteAmountLengthPin.unsubscribe();
        }
        if (minQuoteAmountLengthPin != null) {
            minQuoteAmountLengthPin.unsubscribe();
        }
        isRangeAmountEnabledPin.unsubscribe();
        sliderTrackStylePin.unsubscribe();
        maxOrFixedAmountSlider.valueProperty().unbindBidirectional(model.getMaxOrFixedAmountSliderValue());
        maxOrFixedAmountSlider.valueProperty().removeListener(maxOrFixedAmountSliderValueListener);
        minAmountSlider.valueProperty().unbindBidirectional(model.getMinAmountSliderValue());
        minAmountSlider.valueProperty().removeListener(minAmountSliderValueListener);
        model.getMaxOrFixedAmountSliderFocus().unbind();
        model.getMinAmountSliderFocus().unbind();
        description.textProperty().unbind();
        minRangeValue.textProperty().unbind();
        minRangeCode.textProperty().unbind();
        maxRangeValue.textProperty().unbind();
        maxRangeCode.textProperty().unbind();
        quoteAmountSeparator.visibleProperty().unbind();
        quoteAmountSeparator.managedProperty().unbind();
        baseAmountSeparator.visibleProperty().unbind();
        baseAmountSeparator.managedProperty().unbind();
        minQuoteAmountRoot.visibleProperty().unbind();
        minQuoteAmountRoot.managedProperty().unbind();
        minBaseAmountRoot.visibleProperty().unbind();
        minBaseAmountRoot.managedProperty().unbind();
        minAmountSlider.visibleProperty().unbind();
        minAmountSlider.managedProperty().unbind();
        flipCurrenciesButton.visibleProperty().unbind();
        flipCurrenciesButton.managedProperty().unbind();
        baseAmountSelectionHBox.minWidthProperty().unbind();
        baseAmountSelectionHBox.maxWidthProperty().unbind();

        maxOrFixedQuoteAmount.isAmountValidProperty().set(true);
        minQuoteAmount.isAmountValidProperty().set(true);

        flipCurrenciesButton.setOnAction(null);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }

    private void onInputTextFieldFocus(boolean isOtherFocused, boolean focus) {
        description.getStyleClass().clear();
        if (focus || isOtherFocused) {
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animatePrefWidth(selectionLine, model.getAmountBoxWidth() + 40);
            description.getStyleClass().add("description-focused");
        } else {
            Transitions.fadeOut(selectionLine, 200);
            description.getStyleClass().add("description");
        }
    }

    private void applyTextInputPrefWidth() {
        int charCount = getCalculatedTotalCharCount();

        int length = getCalculatedTextInputLength(minQuoteAmount);
        minQuoteAmount.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));

        length = getCalculatedTextInputLength(maxOrFixedQuoteAmount);
        maxOrFixedQuoteAmount.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));
    }

    private void applyTextInputFontStyle() {
        quoteAmountSelectionHBox.getStyleClass().clear();
        quoteAmountSelectionHBox.getStyleClass().add("quote-amount");

        int charCount = getCalculatedTotalCharCount();
        quoteAmountSelectionHBox.getStyleClass().add(getFontStyleBasedOnTextLength(charCount));
    }

    private int getCalculatedTotalCharCount() {
        int count = model.getIsRangeAmountEnabled().get()
                ? minQuoteAmount.getTextInputLength() + maxOrFixedQuoteAmount.getTextInputLength() + 1 // 1 for the dash
                : maxOrFixedQuoteAmount.getTextInputLength();

        if (!minQuoteAmount.getTextInput().contains(".") || !maxOrFixedQuoteAmount.getTextInput().contains(".")) {
            // If using an integer we need to count one more char since a dot occupies much less space.
            ++count;
        }
        return count;
    }

    private int getCalculatedTextInputLength(BigNumberAmountInputBox quoteAmountInputBox) {
        // If using an integer we need to count one more char since a dot occupies much less space.
        return !quoteAmountInputBox.getTextInput().contains(".")
                ? quoteAmountInputBox.getTextInputLength() + 1
                : quoteAmountInputBox.getTextInputLength();
    }

    private void deselectAll() {
        minQuoteAmount.deselect();
        maxOrFixedQuoteAmount.deselect();
    }

    private static int getFontCharWidth(int charCount) {
        if (charCount < 10) {
            return 31;
        }
        if (charCount == 10) {
            return 28;
        }
        if (charCount == 11) {
            return 25;
        }
        if (charCount == 12) {
            return 23;
        }
        if (charCount == 13) {
            return 21;
        }
        if (charCount == 14) {
            return 19;
        }
        if (charCount == 15) {
            return 18;
        }
        if (charCount == 16) {
            return 17;
        }
        if (charCount == 17) {
            return 16;
        }
        return 15;
    }

    private static String getFontStyleBasedOnTextLength(int charCount) {
        if (charCount < 10) {
            return INPUT_TEXT_9_STYLE_CLASS;
        }
        if (charCount == 10) {
            return INPUT_TEXT_10_STYLE_CLASS;
        }
        if (charCount == 11) {
            return INPUT_TEXT_11_STYLE_CLASS;
        }
        if (charCount == 12) {
            return INPUT_TEXT_12_STYLE_CLASS;
        }
        if (charCount == 13) {
            return INPUT_TEXT_13_STYLE_CLASS;
        }
        if (charCount == 14) {
            return INPUT_TEXT_14_STYLE_CLASS;
        }
        if (charCount == 15) {
            return INPUT_TEXT_15_STYLE_CLASS;
        }
        if (charCount == 16) {
            return INPUT_TEXT_16_STYLE_CLASS;
        }
        if (charCount == 17) {
            return INPUT_TEXT_17_STYLE_CLASS;
        }
        if (charCount == 18) {
            return INPUT_TEXT_18_STYLE_CLASS;
        }
        if (charCount == 19) {
            return INPUT_TEXT_19_STYLE_CLASS;
        }
        if (charCount == 20) {
            return INPUT_TEXT_20_STYLE_CLASS;
        }
        if (charCount == 21) {
            return INPUT_TEXT_21_STYLE_CLASS;
        }
        if (charCount == 22) {
            return INPUT_TEXT_22_STYLE_CLASS;
        }
        return INPUT_TEXT_23_STYLE_CLASS;
    }
}
