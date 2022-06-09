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

package bisq.desktop.primary.overlay.onboarding.offer.amount;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountView extends View<VBox, AmountModel, AmountController> {
    private final Label minAmountLabel, maxAmountLabel;
    //   private final Label minAmountLabel, maxAmountLabel, valueLabel, currencyLabel, marketValueLabel;
    private final Slider slider;
    private final Button nextButton, backButton;
    private final Label headLineLabel;
    private final Label subtitleLabel;

    public AmountView(AmountModel model, AmountController controller, Pane baseAmount, Pane quoteAmount, Pane price) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        headLineLabel = new Label();
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");

        VBox.setMargin(baseAmount, new Insets(-15, 0, 0, 0));
        VBox.setMargin(quoteAmount, new Insets(-17, 0, 0, 0));
        VBox vbox = new VBox(0, baseAmount, quoteAmount);
        vbox.getStyleClass().add("bisq-box-3");
        vbox.setAlignment(Pos.CENTER);
        vbox.setMaxWidth(330);
        vbox.setPadding(new Insets(25, 20, 10, 20));

        slider = new Slider();
        minAmountLabel = new Label();
        minAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        maxAmountLabel = new Label();
        maxAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");

        VBox sliderBox = new VBox(2, slider, new HBox(minAmountLabel, Spacer.fillHBox(), maxAmountLabel));
        sliderBox.setMaxWidth(330);
        VBox.setMargin(sliderBox, new Insets(28, 0, 70, 0));

        backButton = new Button(Res.get("back"));
        nextButton = new Button(Res.get("next"));
        nextButton.setDefaultButton(true);

        HBox buttons = new HBox(7, backButton, nextButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(38, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 60, 0));
        VBox.setMargin(buttons, new Insets(0, 0, 90, 0));

        root.getChildren().addAll(headLineLabel, subtitleLabel, vbox, sliderBox, Spacer.fillVBox(), buttons);
    }

    @Override
    protected void onViewAttached() {
        slider.minProperty().bind(model.getSliderMin());
        slider.maxProperty().bind(model.getSliderMax());
        slider.valueProperty().bindBidirectional(model.getSliderValue());
        model.getSliderFocus().bind(slider.focusedProperty());

        backButton.setOnAction(evt -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());

        // Needed to trigger focusOut event on amount components
        root.setOnMousePressed(e -> root.requestFocus());

        headLineLabel.setText(Res.get("onboarding.amount.headline", model.getDirection().get()));
        subtitleLabel.setText(Res.get("onboarding.amount.subtitle", model.getDirection().get()));
        minAmountLabel.setText(Res.get("onboarding.amount.minLabel",
                AmountFormatter.formatAmountWithCode(model.getMinAmount().get(), true)));
        maxAmountLabel.setText(Res.get("onboarding.amount.maxLabel",
                AmountFormatter.formatAmountWithCode(model.getMaxAmount().get(), true)));
    }

    @Override
    protected void onViewDetached() {
        slider.minProperty().unbind();
        slider.maxProperty().unbind();
        slider.valueProperty().unbindBidirectional(model.getSliderValue());
        model.getSliderFocus().unbind();

        backButton.setOnAction(null);
        nextButton.setOnAction(null);
        root.setOnMousePressed(null);
    }
}
