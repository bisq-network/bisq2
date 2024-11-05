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
    public final static int AMOUNT_BOX_HEIGHT = 130;

    private final Slider slider = new Slider();
    private final Label minRangeValue, maxRangeValue, description;
    private final Region selectionLine;
    private final SmallAmountInput baseAmount;
    private final BigAmountInput quoteAmount;
    private Subscription baseAmountFocusPin, quoteAmountFocusPin, sliderTrackStylePin, useCompactFormatPin;

    AmountSelectionView(AmountSelectionModel model,
                        AmountSelectionController controller,
                        SmallAmountInput baseAmount,
                        BigAmountInput quoteAmount) {
        super(new VBox(10), model, controller);

        Pane baseAmountRoot = baseAmount.getRoot();
        this.baseAmount = baseAmount;
        Pane quoteAmountRoot = quoteAmount.getRoot();
        this.quoteAmount = quoteAmount;

        description = new Label();
        description.getStyleClass().add("description");
        description.setMouseTransparent(true);

        VBox amountVBox = new VBox(0, description, Spacer.fillVBox(), quoteAmountRoot,
                Spacer.fillVBox(), baseAmountRoot);
        amountVBox.getStyleClass().add("bisq-dual-amount-bg");
        amountVBox.setMinWidth(AMOUNT_BOX_WIDTH);
        amountVBox.setMaxWidth(AMOUNT_BOX_WIDTH);
        amountVBox.setMinHeight(AMOUNT_BOX_HEIGHT);
        amountVBox.setMaxHeight(AMOUNT_BOX_HEIGHT);
        amountVBox.setPadding(new Insets(5, 20, 10, 20));

        Region line = new Region();
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

        Pane amountPane = new Pane(amountVBox, line, selectionLine);
        amountPane.setMaxWidth(AMOUNT_BOX_WIDTH);

        slider.setMin(model.getSliderMin());
        slider.setMax(model.getSliderMax());

        minRangeValue = new Label();
        minRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        maxRangeValue = new Label();
        maxRangeValue.getStyleClass().add("bisq-small-light-label-dimmed");

        VBox sliderBox = new VBox(2, slider, new HBox(minRangeValue, Spacer.fillHBox(), maxRangeValue));
        sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH);

//            VBox.setMargin(amountPane, new Insets(0, 0, 20, 0));
        root.getChildren().addAll(amountPane, sliderBox);
        root.getStyleClass().add("amount-component");
        root.setAlignment(Pos.TOP_CENTER);
    }

    @Override
    protected void onViewAttached() {
        UIScheduler.run(() -> {
            quoteAmount.requestFocus();
            baseAmountFocusPin = EasyBind.subscribe(baseAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(quoteAmount.focusedProperty(), focus));
            quoteAmountFocusPin = EasyBind.subscribe(quoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(baseAmount.focusedProperty(), focus));
        }).after(700);

        sliderTrackStylePin = EasyBind.subscribe(model.getSliderTrackStyle(), slider::setStyle);
        slider.valueProperty().bindBidirectional(model.getSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());
        description.textProperty().bind(model.getDescription());
        minRangeValue.textProperty().bind(model.getMinRangeValueAsString());
        maxRangeValue.textProperty().bind(model.getMaxRangeValueAsString());

        useCompactFormatPin = EasyBind.subscribe(model.getUseCompactFormat(), useCompactFormat -> {

        });

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
        if (baseAmountFocusPin != null) {
            baseAmountFocusPin.unsubscribe();
        }
        if (quoteAmountFocusPin != null) {
            quoteAmountFocusPin.unsubscribe();
        }
        sliderTrackStylePin.unsubscribe();
        slider.valueProperty().unbindBidirectional(model.getSliderValue());
        model.getSliderFocus().unbind();
        description.textProperty().unbind();
        minRangeValue.textProperty().unbind();
        maxRangeValue.textProperty().unbind();
        baseAmount.isAmountValidProperty().set(true);
        quoteAmount.isAmountValidProperty().set(true);
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
