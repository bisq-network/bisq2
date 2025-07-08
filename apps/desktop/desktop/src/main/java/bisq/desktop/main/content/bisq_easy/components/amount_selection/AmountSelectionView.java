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
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.RangeSlider;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AmountSelectionView extends View<VBox, AmountSelectionModel, AmountSelectionController> {
    private static final Insets SLIDER_INDICATORS_RANGE_MARGIN = new Insets(-15, 0, 0, 0);
    private static final Insets SLIDER_INDICATORS_FIXED_MARGIN = new Insets(2.5, 0, 0, 0);
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String EN_DASH_SYMBOL = "\u2013"; // Unicode for "–"
    private static final Map<Integer, String> CHAR_COUNT_FONT_STYLE_MAP = new HashMap<>();
    static {
        CHAR_COUNT_FONT_STYLE_MAP.put(9, "input-text-9");
        CHAR_COUNT_FONT_STYLE_MAP.put(10, "input-text-10");
        CHAR_COUNT_FONT_STYLE_MAP.put(11, "input-text-11");
        CHAR_COUNT_FONT_STYLE_MAP.put(12, "input-text-12");
        CHAR_COUNT_FONT_STYLE_MAP.put(13, "input-text-13");
        CHAR_COUNT_FONT_STYLE_MAP.put(14, "input-text-14");
        CHAR_COUNT_FONT_STYLE_MAP.put(15, "input-text-15");
        CHAR_COUNT_FONT_STYLE_MAP.put(16, "input-text-16");
        CHAR_COUNT_FONT_STYLE_MAP.put(17, "input-text-17");
        CHAR_COUNT_FONT_STYLE_MAP.put(18, "input-text-18");
        CHAR_COUNT_FONT_STYLE_MAP.put(19, "input-text-19");
        CHAR_COUNT_FONT_STYLE_MAP.put(20, "input-text-20");
        CHAR_COUNT_FONT_STYLE_MAP.put(21, "input-text-21");
        CHAR_COUNT_FONT_STYLE_MAP.put(22, "input-text-22");
        CHAR_COUNT_FONT_STYLE_MAP.put(23, "input-text-23");
    }

    private final Slider maxOrFixedAmountSlider;
    private final Label minRangeValue, maxRangeValue, minRangeCode, maxRangeCode, description, quoteAmountSeparator,
            baseAmountSeparator;
    private final Region selectionLine;
    private final Pane minQuoteAmountRoot, minBaseAmountRoot, invertedMinQuoteAmountRoot, invertedMinBaseAmountRoot,
            maxOrFixedBaseAmountRoot, maxOrFixedQuoteAmountRoot, invertedMaxOrFixedQuoteAmountRoot, invertedMaxOrFixedBaseAmountRoot;
    private final HBox quoteAmountSelectionHBox, baseAmountSelectionHBox, sliderIndicators;
    private final ChangeListener<Number> maxOrFixedAmountSliderValueListener, minAmountSliderValueListener;
    private final BisqMenuItem flipCurrenciesButton;
    private final RangeSlider rangeSlider;
    private Subscription shouldFocusInputTextFieldPin, sliderTrackStylePin, isRangeAmountEnabledPin,
            shouldApplyNewInputTextFontStylePin;

    AmountSelectionView(AmountSelectionModel model,
                        AmountSelectionController controller,
                        Pane maxOrFixedBaseAmount,
                        Pane maxOrFixedQuoteAmount,
                        Pane invertedMaxOrFixedQuoteAmount,
                        Pane invertedMaxOrFixedBaseAmount,
                        Pane minBaseAmount,
                        Pane minQuoteAmount,
                        Pane invertedMinQuoteAmount,
                        Pane invertedMinBaseAmount) {
        super(new VBox(10), model, controller);

        // max or fixed component
        maxOrFixedBaseAmountRoot = maxOrFixedBaseAmount;
        maxOrFixedQuoteAmountRoot = maxOrFixedQuoteAmount;
        // inverted ones
        invertedMaxOrFixedQuoteAmountRoot = invertedMaxOrFixedQuoteAmount;
        invertedMaxOrFixedBaseAmountRoot = invertedMaxOrFixedBaseAmount;

        // min component (only shown when using a range)
        minBaseAmountRoot = minBaseAmount;
        minQuoteAmountRoot = minQuoteAmount;
        // inverted ones
        invertedMinQuoteAmountRoot = invertedMinQuoteAmount;
        invertedMinBaseAmountRoot = invertedMinBaseAmount;

        // quote amount selection
        quoteAmountSeparator = new Label(EN_DASH_SYMBOL);
        quoteAmountSeparator.getStyleClass().add("quote-separator");
        minQuoteAmountRoot.getStyleClass().add("min-quote-amount");
        invertedMinBaseAmountRoot.getStyleClass().add("min-quote-amount");
        maxOrFixedQuoteAmountRoot.getStyleClass().add("max-or-fixed-quote-amount");
        invertedMaxOrFixedBaseAmountRoot.getStyleClass().add("max-or-fixed-quote-amount");
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
        invertedMinQuoteAmountRoot.getStyleClass().add("min-base-amount");
        maxOrFixedBaseAmountRoot.getStyleClass().add("max-or-fixed-base-amount");
        invertedMaxOrFixedQuoteAmountRoot.getStyleClass().add("max-or-fixed-base-amount");
        baseAmountSelectionHBox = new HBox(minBaseAmountRoot, invertedMinQuoteAmountRoot, baseAmountSeparator,
                maxOrFixedBaseAmountRoot, invertedMaxOrFixedQuoteAmountRoot);
        baseAmountSelectionHBox.getStyleClass().add("base-amount");

        flipCurrenciesButton = new BisqMenuItem("flip-fields-arrows-green", "flip-fields-arrows-white");
        flipCurrenciesButton.setTooltip(Res.get("bisqEasy.tradeWizard.amount.selection.flipCurrenciesButton.tooltip"));
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

        // fixed slider
        maxOrFixedAmountSlider = new Slider();
        maxOrFixedAmountSlider.setMin(model.getSliderMin());
        maxOrFixedAmountSlider.setMax(model.getSliderMax());
        maxOrFixedAmountSlider.getStyleClass().add("max-or-fixed-amount-slider");

        // range slider
        rangeSlider = new RangeSlider();
        rangeSlider.setMin(model.getSliderMin());
        rangeSlider.setMax(model.getSliderMax());
        rangeSlider.getStyleClass().add("amount-range-slider");

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
        VBox sliderBox = new VBox(2, maxOrFixedAmountSlider, rangeSlider, sliderIndicators);
        sliderBox.setMaxWidth(model.getAmountBoxWidth() + 40);

        VBox.setMargin(sliderBox, new Insets(30, 0, 0, 0));
        root.getChildren().addAll(amountPane, sliderBox);
        root.setAlignment(Pos.TOP_CENTER);

        maxOrFixedAmountSliderValueListener = (observable, oldValue, newValue) -> {
            double maxAllowedSliderValue = controller.onGetMaxAllowedSliderValue();
            maxOrFixedAmountSlider.setValue(Math.min(newValue.doubleValue(), maxAllowedSliderValue));
            rangeSlider.setHighValue(Math.min(newValue.doubleValue(), maxAllowedSliderValue));
        };
        minAmountSliderValueListener = (observable, oldValue, newValue) -> {
            double maxAllowedSliderValue = controller.onGetMaxAllowedSliderValue();
            rangeSlider.setLowValue(Math.min(newValue.doubleValue(), maxAllowedSliderValue));
        };
    }

    @Override
    protected void onViewAttached() {
        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            root.getStyleClass().clear();
            root.getStyleClass().add("amount-selection");
            root.getStyleClass().add(isRangeAmountEnabled ? "range-amount" : "fixed-amount");
            VBox.setMargin(sliderIndicators, isRangeAmountEnabled ? SLIDER_INDICATORS_RANGE_MARGIN : SLIDER_INDICATORS_FIXED_MARGIN);
            applyTextInputFontStyle(true);
        });
        sliderTrackStylePin = EasyBind.subscribe(model.getSliderTrackStyle(), trackStyle -> {
            rangeSlider.setStyle(trackStyle);
            maxOrFixedAmountSlider.setStyle(trackStyle);
        });
        shouldFocusInputTextFieldPin = EasyBind.subscribe(model.getShouldFocusInputTextField(), this::maybeFocusInputTextField);
        shouldApplyNewInputTextFontStylePin = EasyBind.subscribe(model.getShouldApplyNewInputTextFontStyle(), this::applyTextInputFontStyle);

        rangeSlider.lowValueProperty().bindBidirectional(model.getMinAmountSliderValue());
        rangeSlider.highValueProperty().bindBidirectional(model.getMaxOrFixedAmountSliderValue());
        rangeSlider.lowValueProperty().addListener(minAmountSliderValueListener);
        rangeSlider.highValueProperty().addListener(maxOrFixedAmountSliderValueListener);
        model.getRangeSliderFocus().bind(rangeSlider.focusedProperty());
        maxOrFixedAmountSlider.valueProperty().bindBidirectional(model.getMaxOrFixedAmountSliderValue());
        maxOrFixedAmountSlider.valueProperty().addListener(maxOrFixedAmountSliderValueListener);
        model.getMaxOrFixedAmountSliderFocus().bind(maxOrFixedAmountSlider.focusedProperty());
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
        rangeSlider.visibleProperty().bind(model.getIsRangeAmountEnabled());
        rangeSlider.managedProperty().bind(model.getIsRangeAmountEnabled());
        maxOrFixedAmountSlider.visibleProperty().bind(model.getIsRangeAmountEnabled().not());
        maxOrFixedAmountSlider.managedProperty().bind(model.getIsRangeAmountEnabled().not());
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
        isRangeAmountEnabledPin.unsubscribe();
        sliderTrackStylePin.unsubscribe();
        shouldFocusInputTextFieldPin.unsubscribe();
        shouldApplyNewInputTextFontStylePin.unsubscribe();

        rangeSlider.highValueProperty().unbindBidirectional(model.getMaxOrFixedAmountSliderValue());
        rangeSlider.highValueProperty().removeListener(maxOrFixedAmountSliderValueListener);
        rangeSlider.lowValueProperty().unbindBidirectional(model.getMinAmountSliderValue());
        rangeSlider.lowValueProperty().removeListener(minAmountSliderValueListener);
        model.getRangeSliderFocus().unbind();
        maxOrFixedAmountSlider.valueProperty().unbindBidirectional(model.getMaxOrFixedAmountSliderValue());
        maxOrFixedAmountSlider.valueProperty().removeListener(maxOrFixedAmountSliderValueListener);
        model.getMaxOrFixedAmountSliderFocus().unbind();
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
        invertedMinQuoteAmountRoot.visibleProperty().unbind();
        invertedMinQuoteAmountRoot.managedProperty().unbind();
        invertedMinBaseAmountRoot.visibleProperty().unbind();
        invertedMinBaseAmountRoot.managedProperty().unbind();
        rangeSlider.visibleProperty().unbind();
        rangeSlider.managedProperty().unbind();
        maxOrFixedAmountSlider.visibleProperty().unbind();
        maxOrFixedAmountSlider.managedProperty().unbind();
        flipCurrenciesButton.visibleProperty().unbind();
        flipCurrenciesButton.managedProperty().unbind();
        baseAmountSelectionHBox.minWidthProperty().unbind();
        baseAmountSelectionHBox.maxWidthProperty().unbind();
        maxOrFixedBaseAmountRoot.visibleProperty().unbind();
        maxOrFixedBaseAmountRoot.managedProperty().unbind();
        maxOrFixedQuoteAmountRoot.visibleProperty().unbind();
        maxOrFixedQuoteAmountRoot.managedProperty().unbind();
        invertedMaxOrFixedQuoteAmountRoot.visibleProperty().unbind();
        invertedMaxOrFixedQuoteAmountRoot.managedProperty().unbind();
        invertedMaxOrFixedBaseAmountRoot.visibleProperty().unbind();
        invertedMaxOrFixedBaseAmountRoot.managedProperty().unbind();

        flipCurrenciesButton.setOnAction(null);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }

    private void maybeFocusInputTextField(boolean shouldFocus) {
        description.getStyleClass().clear();
        if (shouldFocus) {
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animatePrefWidth(selectionLine, model.getAmountBoxWidth() + 40);
            description.getStyleClass().add("description-focused");
        } else {
            Transitions.fadeOut(selectionLine, 200);
            description.getStyleClass().add("description");
        }
    }

    private void applyTextInputFontStyle(boolean shouldApplyNewTextStyle) {
        if (shouldApplyNewTextStyle) {
            quoteAmountSelectionHBox.getStyleClass().clear();
            quoteAmountSelectionHBox.getStyleClass().add("quote-amount");
            int charCount = controller.onGetCalculatedTotalCharCount();
            quoteAmountSelectionHBox.getStyleClass().add(getFontStyleBasedOnTextLength(charCount));
            model.getShouldApplyNewInputTextFontStyle().set(false);
        }
    }

    private static String getFontStyleBasedOnTextLength(int charCount) {
        if (charCount < 10) {
            return "input-text-9";
        }
        return CHAR_COUNT_FONT_STYLE_MAP.getOrDefault(charCount, "input-text-23");
    }
}
