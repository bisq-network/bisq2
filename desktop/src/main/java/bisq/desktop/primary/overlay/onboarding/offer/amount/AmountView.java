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
import bisq.desktop.components.controls.BisqIconButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmountView extends View<VBox, AmountModel, AmountController> {
    private final Label minAmountLabel, maxAmountLabel, valueLabel, currencyLabel, marketValueLabel;
    private final Slider slider;
    private final Button nextButton, backButton;

    public AmountView(AmountModel model, AmountController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.amount.headline", model.getDirectionAsString()));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.amount.subtitle", model.getDirectionAsString()));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");

        valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-11");
        currencyLabel = new Label("BTC");
        currencyLabel.getStyleClass().add("bisq-text-9");
        HBox valueBox = new HBox(10, valueLabel, currencyLabel);
        valueBox.setAlignment(Pos.BASELINE_CENTER);
        
        marketValueLabel = new Label();
        marketValueLabel.getStyleClass().add("bisq-text-3");
        Button marketValueInfo = BisqIconButton.createIconButton("info-circle-solid");
        Tooltip tooltip = new Tooltip(Res.get("onboarding.amount.marketValueInfo"));
        tooltip.getStyleClass().add("dark-tooltip");
        marketValueInfo.setTooltip(tooltip);
        HBox marketValueBox = new HBox(3, marketValueLabel, marketValueInfo);
        marketValueBox.setAlignment(Pos.CENTER);
        
        VBox vbox = new VBox(15, valueBox, marketValueBox);
        vbox.getStyleClass().add("bisq-box-3");
        vbox.setAlignment(Pos.CENTER);
        vbox.setMaxWidth(400);
        vbox.setPadding(new Insets(20, 20, 20, 20));
        VBox.setMargin(vbox, new Insets(0, 0, 28, 0));

        slider = new Slider();
        minAmountLabel = new Label();
        minAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        maxAmountLabel = new Label();
        maxAmountLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        
        VBox sliderBox = new VBox(2, slider, new HBox(minAmountLabel, Spacer.fillHBox(), maxAmountLabel));
        sliderBox.setMaxWidth(400);
        VBox.setMargin(sliderBox, new Insets(10, 0, 60, 0));

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
        slider.minProperty().bind(model.minAmountAsDouble);
        slider.maxProperty().bind(model.maxAmountAsDoubleProperty);
        slider.valueProperty().bindBidirectional(model.amountAsDoubleProperty);
        
        minAmountLabel.textProperty().bind(model.formattedMinAmountProperty);
        maxAmountLabel.textProperty().bind(model.formattedMaxAmountProperty);
        
        valueLabel.textProperty().bind(model.formattedAmountProperty);
        currencyLabel.textProperty().bind(model.currencyCode);
        marketValueLabel.textProperty().bind(model.formattedQuoteAmountProperty);

        backButton.setOnAction(evt -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        slider.minProperty().unbind();
        slider.maxProperty().unbind();
        slider.valueProperty().unbindBidirectional(model.amountAsDoubleProperty);
        
        minAmountLabel.textProperty().unbind();
        maxAmountLabel.textProperty().unbind();

        valueLabel.textProperty().unbind();
        currencyLabel.textProperty().unbind();
        marketValueLabel.textProperty().unbind();

        backButton.setOnAction(null);
        nextButton.setOnAction(null);
    }
}
