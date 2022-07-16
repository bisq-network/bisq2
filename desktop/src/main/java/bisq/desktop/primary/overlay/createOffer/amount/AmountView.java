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

package bisq.desktop.primary.overlay.createOffer.amount;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class AmountView extends View<VBox, AmountModel, AmountController> {
    private final static int AMOUNT_BOX_WIDTH = 330;
    private final Label minAmountLabel, maxAmountLabel;
    private final Slider slider;
    private final Label headLineLabel;
    private final Label subtitleLabel;
    private final Region line, selectionLine;
    private final Pane baseAmountRoot;
    private final SmallAmountInput baseAmount;
    private final Pane quoteAmountRoot;
    private final BigAmountInput quoteAmount;
    private Subscription baseAmountFocusPin, quoteAmountFocusPin;

    public AmountView(AmountModel model, AmountController controller, SmallAmountInput baseAmount, BigAmountInput quoteAmount) {
        super(new VBox(), model, controller);

        baseAmountRoot = baseAmount.getRoot();
        this.baseAmount = baseAmount;
        quoteAmountRoot = quoteAmount.getRoot();
        this.quoteAmount = quoteAmount;

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        headLineLabel = new Label();
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        VBox.setMargin(quoteAmountRoot, new Insets(-15, 0, 0, 0));
        VBox.setMargin(baseAmountRoot, new Insets(-17, 0, 0, 0));
        VBox vbox = new VBox(0, quoteAmountRoot, baseAmountRoot);
        vbox.getStyleClass().add("bisq-dual-amount-bg");
        vbox.setAlignment(Pos.CENTER);
        vbox.setMinWidth(AMOUNT_BOX_WIDTH);
        vbox.setMaxWidth(AMOUNT_BOX_WIDTH);
        vbox.setPadding(new Insets(25, 20, 10, 20));

        line = new Region();
        line.setLayoutY(121);
        line.setPrefHeight(1);
        line.setPrefWidth(AMOUNT_BOX_WIDTH);
        line.setStyle("-fx-background-color: -bisq-grey-dimmed");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.getStyleClass().add("bisq-green-line");
        selectionLine.setPrefHeight(3);
        selectionLine.setPrefWidth(0);
        selectionLine.setLayoutY(119);
        selectionLine.setMouseTransparent(true);
        Pane pane = new Pane(vbox, line, selectionLine);
        pane.setMaxWidth(AMOUNT_BOX_WIDTH);

        slider = new Slider();
        minAmountLabel = new Label();
        minAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        maxAmountLabel = new Label();
        maxAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");

        VBox sliderBox = new VBox(2, slider, new HBox(minAmountLabel, Spacer.fillHBox(), maxAmountLabel));
        sliderBox.setMaxWidth(AMOUNT_BOX_WIDTH);
        VBox.setMargin(sliderBox, new Insets(28, 0, 70, 0));

        VBox.setMargin(headLineLabel, new Insets(44, 0, 2, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 60, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel, pane, sliderBox);
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

    @Override
    protected void onViewAttached() {
        UIScheduler.run(() -> {
            quoteAmount.requestFocus();
            baseAmountFocusPin = EasyBind.subscribe(baseAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(quoteAmount.focusedProperty(), focus));
            quoteAmountFocusPin = EasyBind.subscribe(quoteAmount.focusedProperty(),
                    focus -> onInputTextFieldFocus(baseAmount.focusedProperty(), focus));
        }).after(700);

        slider.minProperty().bind(model.getSliderMin());
        slider.maxProperty().bind(model.getSliderMax());
        slider.valueProperty().bindBidirectional(model.getSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());

        // Needed to trigger focusOut event on amount components
        root.setOnMousePressed(e -> root.requestFocus());

        headLineLabel.setText(Res.get("onboarding.amount.headline"));
        subtitleLabel.setText(Res.get("onboarding.amount.subtitle",
                model.getQuoteSideAmount().get().getCode(),
                model.getSpendOrReceiveString().get()));
        minAmountLabel.setText(Res.get("onboarding.amount.minLabel",
                AmountFormatter.formatAmountWithCode(model.getMinAmount().get(), true)));
        maxAmountLabel.setText(Res.get("onboarding.amount.maxLabel",
                AmountFormatter.formatAmountWithCode(model.getMaxAmount().get(), true)));
    }

    @Override
    protected void onViewDetached() {
        if (baseAmountFocusPin != null) {
            baseAmountFocusPin.unsubscribe();
        }
        if (quoteAmountFocusPin != null) {
            quoteAmountFocusPin.unsubscribe();
        }
        slider.minProperty().unbind();
        slider.maxProperty().unbind();
        slider.valueProperty().unbindBidirectional(model.getSliderValue());
        model.getSliderFocus().unbind();
        root.setOnMousePressed(null);
    }
}
