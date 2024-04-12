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

package bisq.desktop.main.content.bisq_easy.trade_wizard.price;

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardPriceView extends View<VBox, TradeWizardPriceModel, TradeWizardPriceController> {
    private static final String SELECTED_PRICE_MODEL_STYLE_CLASS = "selected-price-model";

    private final MaterialTextField percentage;
    private final VBox fieldsBox;
    private final PriceInput priceInput;
    private final Button percentagePrice, fixedPrice, learnWhy;
    private final Label feedbackSentence;
    private Subscription percentageFocussedPin, useFixPricePin;

    public TradeWizardPriceView(TradeWizardPriceModel model, TradeWizardPriceController controller, PriceInput priceInput) {
        super(new VBox(10), model, controller);

        this.priceInput = priceInput;

        root.setAlignment(Pos.TOP_CENTER);

        Label headline = new Label(Res.get("bisqEasy.price.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headline, new Insets(40, 0, 0, 0));

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.price.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(500);

        percentagePrice = new Button(Res.get("bisqEasy.price.percentage.title"));
        percentagePrice.getStyleClass().add("price-item");
        fixedPrice = new Button(Res.get("bisqEasy.price.tradePrice.title"));
        fixedPrice.getStyleClass().add("price-item");
        Label separator = new Label("|");

        HBox percentagePriceBox = new HBox(percentagePrice);
        percentagePriceBox.getStyleClass().add("price-item-box");
        percentagePriceBox.setAlignment(Pos.CENTER_RIGHT);
        HBox fixedPriceBox = new HBox(fixedPrice);
        fixedPriceBox.getStyleClass().add("price-item-box");
        fixedPriceBox.setAlignment(Pos.CENTER_LEFT);

        HBox pricingModels = new HBox(30, percentagePriceBox, separator, fixedPriceBox);
        pricingModels.getStyleClass().addAll("pricing-models", "bisq-text-3");

        percentage = new MaterialTextField(Res.get("bisqEasy.price.percentage.inputBoxText"));

        fieldsBox = new VBox(20);
        fieldsBox.setAlignment(Pos.TOP_CENTER);
        fieldsBox.setMinWidth(350);
        fieldsBox.setPrefWidth(350);
        fieldsBox.setMaxWidth(350);

        feedbackSentence = new Label();
        feedbackSentence.getStyleClass().add("bisq-text-3");
        learnWhy = new Button(Res.get("bisqEasy.price.feedback.learnWhy"));
        learnWhy.getStyleClass().add("learn-why");
        HBox feedbackBox = new HBox(5, feedbackSentence, learnWhy);
        feedbackBox.getStyleClass().add("feedback-box");

        root.getStyleClass().add("bisq-easy-trade-wizard-price-step");
        root.getChildren().addAll(headline, subtitleLabel, pricingModels, fieldsBox, feedbackBox);
    }

    @Override
    protected void onViewAttached() {
        percentage.textProperty().bindBidirectional(model.getPercentageAsString());
        feedbackSentence.textProperty().bind(model.getFeedbackSentence());

        percentageFocussedPin = EasyBind.subscribe(percentage.textInputFocusedProperty(), controller::onPercentageFocussed);
        // FIXME: The very first time this component is used when starting the app requestFocus() is not being applied.
        useFixPricePin = EasyBind.subscribe(model.getUseFixPrice(), useFixPrice ->
                UIScheduler.run(this::updateFieldsBox).after(100));

        percentagePrice.setOnAction(e -> controller.usePercentagePrice());
        fixedPrice.setOnAction(e -> controller.useFixedPrice());

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
        percentage.textProperty().unbindBidirectional(model.getPercentageAsString());
        feedbackSentence.textProperty().unbind();

        percentageFocussedPin.unsubscribe();
        useFixPricePin.unsubscribe();

        percentagePrice.setOnAction(null);
        fixedPrice.setOnAction(null);

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
            fieldsBox.getChildren().setAll(priceInput.getRoot(), percentage);
            percentage.deselect();
            percentage.setEditable(false);
            priceInput.setEditable(true);
            priceInput.requestFocus();
        } else {
            percentagePrice.getStyleClass().add(SELECTED_PRICE_MODEL_STYLE_CLASS);
            fieldsBox.getChildren().setAll(percentage, priceInput.getRoot());
            priceInput.deselect();
            priceInput.setEditable(false);
            percentage.setEditable(true);
            percentage.requestFocus();
        }
    }
}
