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

@Slf4j
public class AmountSelectionView extends View<VBox, AmountSelectionModel, AmountSelectionController> {
    public final static int AMOUNT_BOX_WIDTH = 330;
    public final static int AMOUNT_BOX_HEIGHT = 120;

    private final Slider slider = new Slider();
    private final Label minRangeValue, maxRangeValue, description;
    private final Region line, selectionLine;
    private final SmallAmountInput maxOrFixedBaseAmount, minBaseAmount;
    private final BigAmountInput maxOrFixedQuoteAmount, minQuoteAmount;
    private final VBox minAmountVBox, amountSeparatorVBox, sliderBox;
    private Subscription maxOrFixedBaseAmountFocusPin, maxOrFixedQuoteAmountFocusPin,
            minBaseAmountFocusPin, minQuoteAmountFocusPin, sliderTrackStylePin, isRangeAmountEnabledPin;

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

        VBox maxOrFixedAmountVBox = new VBox(0, maxOrFixedQuoteAmountRoot, maxOrFixedBaseAmountRoot);
        maxOrFixedAmountVBox.getStyleClass().add("max-or-fixed-amount");

        // min component (only shown when using a range)
        Pane minBaseAmountRoot = minBaseAmount.getRoot();
        this.minBaseAmount = minBaseAmount;
        Pane minQuoteAmountRoot = minQuoteAmount.getRoot();
        this.minQuoteAmount = minQuoteAmount;

        minAmountVBox = new VBox(0, minQuoteAmountRoot, minBaseAmountRoot);
        minAmountVBox.getStyleClass().add("min-amount");

        // rest of the component
        description = new Label();
        description.getStyleClass().add("description");
        description.setMouseTransparent(true);

        Label quoteAmountSeparator = new Label("-");
        quoteAmountSeparator.getStyleClass().add("quote-separator");
        Label baseAmountSeparator = new Label("-");
        baseAmountSeparator.getStyleClass().add("base-separator");
        amountSeparatorVBox = new VBox(quoteAmountSeparator, baseAmountSeparator);
        amountSeparatorVBox.getStyleClass().add("amount-separator");
        HBox amountInputHBox = new HBox(minAmountVBox, amountSeparatorVBox, maxOrFixedAmountVBox);
        amountInputHBox.setMaxWidth(AMOUNT_BOX_WIDTH);
        amountInputHBox.setMinWidth(AMOUNT_BOX_WIDTH);
        amountInputHBox.setMinHeight(AMOUNT_BOX_HEIGHT - 23);
        amountInputHBox.setMaxHeight(AMOUNT_BOX_HEIGHT - 23);
        amountInputHBox.getStyleClass().add("amount-input");

        VBox descriptionAndAmountVBox = new VBox(0, description, Spacer.fillVBox(), amountInputHBox);
        descriptionAndAmountVBox.getStyleClass().add("bisq-dual-amount-bg");

        line = new Region();
        line.setPrefHeight(1);
        line.setPrefWidth(AMOUNT_BOX_WIDTH);
        line.setLayoutY(AMOUNT_BOX_HEIGHT);
        line.setStyle("-fx-background-color: -bisq-mid-grey-20");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.getStyleClass().add("material-text-field-selection-line");
        selectionLine.setPrefHeight(3);
        selectionLine.setPrefWidth(0);
        selectionLine.setLayoutY(AMOUNT_BOX_HEIGHT - 2);
        selectionLine.setMouseTransparent(true);

        Pane amountPane = new Pane(descriptionAndAmountVBox, line, selectionLine);
        amountPane.setMaxWidth(AMOUNT_BOX_WIDTH);
        amountPane.setMinHeight(AMOUNT_BOX_HEIGHT);
        amountPane.setMaxHeight(AMOUNT_BOX_HEIGHT);

        slider.setMin(model.getSliderMin());
        slider.setMax(model.getSliderMax());

        minRangeValue = new Label();
        minRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        maxRangeValue = new Label();
        maxRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        sliderBox = new VBox(2, slider, new HBox(minRangeValue, Spacer.fillHBox(), maxRangeValue));
        sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH);

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
        }).after(700);

        isRangeAmountEnabledPin = EasyBind.subscribe(model.getIsRangeAmountEnabled(), isRangeAmountEnabled -> {
            root.getStyleClass().clear();
            root.getStyleClass().add("amount-selection");
            root.getStyleClass().add(isRangeAmountEnabled ? "range-amount" : "fixed-amount");
            maxOrFixedQuoteAmount.setUseVerySmallText(isRangeAmountEnabled);
            minQuoteAmount.setUseVerySmallText(isRangeAmountEnabled);
        });
        sliderTrackStylePin = EasyBind.subscribe(model.getSliderTrackStyle(), slider::setStyle);
        slider.valueProperty().bindBidirectional(model.getMaxOrFixedSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());
        description.textProperty().bind(model.getDescription());
        minRangeValue.textProperty().bind(model.getMinRangeValueAsString());
        maxRangeValue.textProperty().bind(model.getMaxRangeValueAsString());
        minAmountVBox.visibleProperty().bind(model.getIsRangeAmountEnabled());
        minAmountVBox.managedProperty().bind(model.getIsRangeAmountEnabled());
        amountSeparatorVBox.visibleProperty().bind(model.getIsRangeAmountEnabled());
        amountSeparatorVBox.managedProperty().bind(model.getIsRangeAmountEnabled());

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
        isRangeAmountEnabledPin.unsubscribe();
        sliderTrackStylePin.unsubscribe();
        slider.valueProperty().unbindBidirectional(model.getMaxOrFixedSliderValue());
        model.getSliderFocus().unbind();
        description.textProperty().unbind();
        minRangeValue.textProperty().unbind();
        maxRangeValue.textProperty().unbind();
        minAmountVBox.visibleProperty().unbind();
        minAmountVBox.managedProperty().unbind();
        amountSeparatorVBox.visibleProperty().unbind();
        amountSeparatorVBox.managedProperty().unbind();

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
            Transitions.animateWidth(selectionLine, AMOUNT_BOX_WIDTH);
        } else if (!other.get()) {
            // If switching between the 2 fields we want to avoid to get the fadeout called that's why
            // we do the check with !other.get()
            Transitions.fadeOut(selectionLine, 200);
        }
    }
}
