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
import bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input.BigAmountInput;
import bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input.SmallAmountInput;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class AmountSelectionView extends View<VBox, AmountSelectionModel, AmountSelectionController> {
    public final static int AMOUNT_BOX_WIDTH = 300;
    public final static int AMOUNT_BOX_HEIGHT = 120;
    private final static String INPUT_TEXT_9_STYLE_CLASS = "input-text-9";
    private final static String INPUT_TEXT_10_STYLE_CLASS = "input-text-10";
    private final static String INPUT_TEXT_11_STYLE_CLASS = "input-text-11";
    private final static String INPUT_TEXT_12_STYLE_CLASS = "input-text-12";
    private final static String INPUT_TEXT_13_STYLE_CLASS = "input-text-13";
    private final static String INPUT_TEXT_14_STYLE_CLASS = "input-text-14";
    private final static String INPUT_TEXT_15_STYLE_CLASS = "input-text-15";
    private final static String INPUT_TEXT_16_STYLE_CLASS = "input-text-16";
    private final static String INPUT_TEXT_17_STYLE_CLASS = "input-text-17";
    private final static String INPUT_TEXT_18_STYLE_CLASS = "input-text-18";
    private final static String INPUT_TEXT_19_STYLE_CLASS = "input-text-19";
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static final String EN_DASH_SYMBOL = "\u2013"; // Unicode for "–"

    private final Slider slider = new Slider();
    private final Label minRangeValue, maxRangeValue, description, quoteAmountSeparator,
            baseAmountSeparator;
    private final Region selectionLine;
    private final SmallAmountInput maxOrFixedBaseAmount, minBaseAmount;
    private final BigAmountInput maxOrFixedQuoteAmount, minQuoteAmount;
    private final Pane minQuoteAmountRoot;
    private final Pane minBaseAmountRoot;
    private final VBox sliderBox;
    private final HBox quoteAmountSelectionHBox;
    private Subscription maxOrFixedBaseAmountFocusPin, maxOrFixedQuoteAmountFocusPin,
            minBaseAmountFocusPin, minQuoteAmountFocusPin, sliderTrackStylePin, isRangeAmountEnabledPin,
            maxOrFixedQuoteAmountLengthPin, minQuoteAmountLengthPin;

    AmountSelectionView(AmountSelectionModel model,
                        AmountSelectionController controller,
                        SmallAmountInput maxOrFixedBaseAmount,
                        BigAmountInput maxOrFixedQuoteAmount,
                        SmallAmountInput minBaseAmount,
                        BigAmountInput minQuoteAmount) {
        super(new VBox(10), model, controller);

        // max or fixed component
        Pane maxOrFixedBaseAmountRoot = maxOrFixedBaseAmount.getRoot();
        this.maxOrFixedBaseAmount = maxOrFixedBaseAmount;
        Pane maxOrFixedQuoteAmountRoot = maxOrFixedQuoteAmount.getRoot();
        this.maxOrFixedQuoteAmount = maxOrFixedQuoteAmount;

        // min component (only shown when using a range)
        minBaseAmountRoot = minBaseAmount.getRoot();
        this.minBaseAmount = minBaseAmount;
        minQuoteAmountRoot = minQuoteAmount.getRoot();
        this.minQuoteAmount = minQuoteAmount;

        // quote amount selection
        quoteAmountSeparator = new Label(EN_DASH_SYMBOL);
        quoteAmountSeparator.getStyleClass().add("quote-separator");
        minQuoteAmountRoot.getStyleClass().add("min-quote-amount");
        maxOrFixedQuoteAmountRoot.getStyleClass().add("max-or-fixed-quote-amount");
        quoteAmountSelectionHBox = new HBox(5, minQuoteAmountRoot, quoteAmountSeparator, maxOrFixedQuoteAmountRoot);
        quoteAmountSelectionHBox.getStyleClass().add("quote-amount");
        quoteAmountSelectionHBox.setMaxWidth(AMOUNT_BOX_WIDTH);
        quoteAmountSelectionHBox.setMinWidth(AMOUNT_BOX_WIDTH);

        // base amount selection
        baseAmountSeparator = new Label(EN_DASH_SYMBOL);
        baseAmountSeparator.getStyleClass().add("base-separator");
        minBaseAmountRoot.getStyleClass().add("min-base-amount");
        maxOrFixedBaseAmountRoot.getStyleClass().add("max-or-fixed-base-amount");
        HBox baseAmountSelectionHBox = new HBox(minBaseAmountRoot, baseAmountSeparator, maxOrFixedBaseAmountRoot);
        baseAmountSelectionHBox.getStyleClass().add("base-amount");
        baseAmountSelectionHBox.setMaxWidth(AMOUNT_BOX_WIDTH);
        baseAmountSelectionHBox.setMinWidth(AMOUNT_BOX_WIDTH);
        HBox.setHgrow(maxOrFixedBaseAmountRoot, Priority.ALWAYS);

        // rest of the component
        description = new Label();
        description.getStyleClass().add("description");
        description.setMouseTransparent(true);

        quoteAmountSelectionHBox.setLayoutY(0);
        quoteAmountSelectionHBox.setMinHeight(70);
        quoteAmountSelectionHBox.setMaxHeight(70);
        baseAmountSelectionHBox.setLayoutY(70);
        Pane amountInputVBox = new Pane(quoteAmountSelectionHBox, baseAmountSelectionHBox);
        amountInputVBox.setMinHeight(AMOUNT_BOX_HEIGHT - 30);
        amountInputVBox.setMaxHeight(AMOUNT_BOX_HEIGHT - 30);
        amountInputVBox.getStyleClass().add("amount-input");

        VBox descriptionAndAmountVBox = new VBox(0, description, Spacer.fillVBox(), amountInputVBox);
        descriptionAndAmountVBox.getStyleClass().addAll("bisq-dual-amount-bg", "description-and-amount-box");

        Region line = new Region();
        line.setPrefHeight(1);
        line.setPrefWidth(AMOUNT_BOX_WIDTH + 40); // plus 40 for the paddings
        line.setLayoutY(AMOUNT_BOX_HEIGHT + 6);
        line.setStyle("-fx-background-color: -bisq-mid-grey-20");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.getStyleClass().add("material-text-field-selection-line");
        selectionLine.setPrefHeight(3);
        selectionLine.setPrefWidth(0);
        selectionLine.setLayoutY(AMOUNT_BOX_HEIGHT + 4);
        selectionLine.setMouseTransparent(true);

        Pane amountPane = new Pane(descriptionAndAmountVBox, line, selectionLine);
        amountPane.setMaxWidth(AMOUNT_BOX_WIDTH + 40);
        amountPane.setMinHeight(AMOUNT_BOX_HEIGHT);
        amountPane.setMaxHeight(AMOUNT_BOX_HEIGHT);

        slider.setMin(model.getSliderMin());
        slider.setMax(model.getSliderMax());

        minRangeValue = new Label();
        minRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        maxRangeValue = new Label();
        maxRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        sliderBox = new VBox(2, slider, new HBox(minRangeValue, Spacer.fillHBox(), maxRangeValue));
        sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH + 40);

        VBox.setMargin(sliderBox, new Insets(30, 0, 0, 0));
        root.getChildren().addAll(amountPane, sliderBox);
        root.setAlignment(Pos.TOP_CENTER);
    }

    @Override
    protected void onViewAttached() {
        UIScheduler.run(() -> {
            maxOrFixedQuoteAmount.requestFocus();
            maxOrFixedBaseAmountFocusPin = EasyBind.subscribe(maxOrFixedBaseAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(maxOrFixedQuoteAmount.focusedProperty(), focus));
            maxOrFixedQuoteAmountFocusPin = EasyBind.subscribe(maxOrFixedQuoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(maxOrFixedBaseAmount.focusedProperty(), focus));
            minBaseAmountFocusPin = EasyBind.subscribe(minBaseAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(minQuoteAmount.focusedProperty(), focus));
            minQuoteAmountFocusPin = EasyBind.subscribe(minQuoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(minBaseAmount.focusedProperty(), focus));
            maxOrFixedQuoteAmountLengthPin = EasyBind.subscribe(maxOrFixedQuoteAmount.lengthProperty(), length -> {
                applyFontStyle();
                applyPrefWidth();
            });
            minQuoteAmountLengthPin = EasyBind.subscribe(minQuoteAmount.lengthProperty(), length -> {
                applyFontStyle();
                applyPrefWidth();
            });
        }).after(700);

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            root.getStyleClass().clear();
            root.getStyleClass().add("amount-selection");
            root.getStyleClass().add(isRangeAmountEnabled ? "range-amount" : "fixed-amount");
            applyFontStyle();
            applyPrefWidth();
        });
        sliderTrackStylePin = EasyBind.subscribe(model.getSliderTrackStyle(), slider::setStyle);

        slider.valueProperty().bindBidirectional(model.getMaxOrFixedSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());
        description.textProperty().bind(model.getDescription());
        minRangeValue.textProperty().bind(model.getMinRangeValueAsString());
        maxRangeValue.textProperty().bind(model.getMaxRangeValueAsString());
        quoteAmountSeparator.visibleProperty().bind(model.getIsRangeAmountEnabled());
        quoteAmountSeparator.managedProperty().bind(model.getIsRangeAmountEnabled());
        baseAmountSeparator.visibleProperty().bind(model.getIsRangeAmountEnabled());
        baseAmountSeparator.managedProperty().bind(model.getIsRangeAmountEnabled());
        minQuoteAmountRoot.visibleProperty().bind(model.getIsRangeAmountEnabled());
        minQuoteAmountRoot.managedProperty().bind(model.getIsRangeAmountEnabled());
        minBaseAmountRoot.visibleProperty().bind(model.getIsRangeAmountEnabled());
        minBaseAmountRoot.managedProperty().bind(model.getIsRangeAmountEnabled());

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
        if (maxOrFixedBaseAmountFocusPin != null) {
            maxOrFixedBaseAmountFocusPin.unsubscribe();
        }
        if (maxOrFixedQuoteAmountFocusPin != null) {
            maxOrFixedQuoteAmountFocusPin.unsubscribe();
        }
        if (minBaseAmountFocusPin != null) {
            minBaseAmountFocusPin.unsubscribe();
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
        slider.valueProperty().unbindBidirectional(model.getMaxOrFixedSliderValue());
        model.getSliderFocus().unbind();
        description.textProperty().unbind();
        minRangeValue.textProperty().unbind();
        maxRangeValue.textProperty().unbind();
        quoteAmountSeparator.visibleProperty().unbind();
        quoteAmountSeparator.managedProperty().unbind();
        baseAmountSeparator.visibleProperty().unbind();
        baseAmountSeparator.managedProperty().unbind();
        minQuoteAmountRoot.visibleProperty().unbind();
        minQuoteAmountRoot.managedProperty().unbind();
        minBaseAmountRoot.visibleProperty().unbind();
        minBaseAmountRoot.managedProperty().unbind();

        maxOrFixedBaseAmount.isAmountValidProperty().set(true);
        maxOrFixedQuoteAmount.isAmountValidProperty().set(true);
        minBaseAmount.isAmountValidProperty().set(true);
        minQuoteAmount.isAmountValidProperty().set(true);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }

    private void onInputTextFieldFocus(ReadOnlyBooleanProperty other, boolean focus) {
        if (focus) {
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animateWidth(selectionLine, AMOUNT_BOX_WIDTH + 40);
        } else if (!other.get()) {
            // If switching between the 2 fields we want to avoid to get the fadeout called that's why
            // we do the check with !other.get()
            Transitions.fadeOut(selectionLine, 200);
        }
    }

    private void applyPrefWidth() {
        int charCount = model.getIsRangeAmountEnabled().get()
                ? minQuoteAmount.getTextInputLength() + maxOrFixedQuoteAmount.getTextInputLength() + 1 // for the dash
                : maxOrFixedQuoteAmount.getTextInputLength();

        int length = minQuoteAmount.getTextInputLength();
        minQuoteAmount.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));

        length = maxOrFixedQuoteAmount.getTextInputLength();
        maxOrFixedQuoteAmount.setTextInputPrefWidth(length == 0 ? 1 : length * getFontCharWidth(charCount));
    }

    private void applyFontStyle() {
        quoteAmountSelectionHBox.getStyleClass().clear();
        quoteAmountSelectionHBox.getStyleClass().add("quote-amount");

        int charCount = model.getIsRangeAmountEnabled().get()
                ? minQuoteAmount.getTextInputLength() + maxOrFixedQuoteAmount.getTextInputLength() + 1 // for the dash
                : maxOrFixedQuoteAmount.getTextInputLength();
        quoteAmountSelectionHBox.getStyleClass().add(getFontStyle(charCount));
    }

    private int getFontCharWidth(int charCount) {
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

    private String getFontStyle(int charCount) {
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
        return INPUT_TEXT_19_STYLE_CLASS;
    }
}
