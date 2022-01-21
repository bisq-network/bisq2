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

package bisq.desktop.primary.main.content.swap.create;

import bisq.common.monetary.Quote;
import bisq.desktop.common.utils.validation.PriceValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.presentation.parser.PriceParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class PriceInput {
    public static class PriceController implements Controller {
        private final PriceModel model;
        @Getter
        private final PriceView view;
        private final PriceValidator validator = new PriceValidator();
        private final ChangeListener<String> baseCurrencyCodeListener, quoteCurrencyCodeListener;
        private final ChangeListener<Quote> fixedPriceQuoteListener;

        public PriceController(MarketPriceService marketPriceService,
                               ObjectProperty<Quote> fixPriceQuote,
                               StringProperty baseCurrencyCode,
                               StringProperty quoteCurrencyCode,
                               String description) {
            this.model = new PriceModel(marketPriceService, fixPriceQuote, baseCurrencyCode, quoteCurrencyCode);

            view = new PriceView(model, this, validator, description);

            baseCurrencyCodeListener = (o, oldValue, newValue) -> onCodeChange();
            quoteCurrencyCodeListener = (o, oldValue, newValue) -> onCodeChange();
            fixedPriceQuoteListener = (o, oldValue, newValue) -> onFixedPriceQuoteChange();
        }

        // domain data change events
        private void onCodeChange() {
            if (!model.hasFocus &&
                    model.baseCurrencyCode.get() != null &&
                    model.quoteCurrencyCode.get() != null &&
                    model.quoteCodes.get() != null) {
                //model.fixPriceQuote.set(PriceParser.parse(model.fixPriceQuote.get(), model.baseCurrencyCode.get(), model.quoteCurrencyCode.get()));
                setQuoteCodes();
            }
        }

        private void onFixedPriceQuoteChange() {
            if (model.fixPriceQuote.get() != null) {
                model.quoteCodes.set(QuoteFormatter.format(model.fixPriceQuote.get()));
                setQuoteCodes();
            }
        }

        // View events
        public void onFixPriceInput(String value) {
            if (!model.hasFocus &&
                    model.baseCurrencyCode.get() != null &&
                    model.quoteCurrencyCode.get() != null &&
                    validator.validate(value).isValid) {
                Quote quote = PriceParser.parse(value, model.baseCurrencyCode.get(), model.quoteCurrencyCode.get());
                model.fixPriceQuote.set(quote);
                setQuoteCodes();
            }
        }

        public void onFocusChange(boolean hasFocus) {
            model.hasFocus = hasFocus;
        }

        public void onViewAttached() {
            model.baseCurrencyCode.addListener(baseCurrencyCodeListener);
            model.quoteCurrencyCode.addListener(quoteCurrencyCodeListener);
            model.fixPriceQuote.addListener(fixedPriceQuoteListener);
        }

        public void onViewDetached() {
            model.baseCurrencyCode.removeListener(baseCurrencyCodeListener);
            model.quoteCurrencyCode.removeListener(quoteCurrencyCodeListener);
            model.fixPriceQuote.removeListener(fixedPriceQuoteListener);
        }

        private void setQuoteCodes() {
            if (model.fixPriceQuote.get() != null) {
                model.quoteCodes.set(model.fixPriceQuote.get().getQuoteCodePair().toString());
            }
        }
    }

    @Getter
    public static class PriceModel implements Model {
        private final MarketPriceService marketPriceService;
        private final ObjectProperty<Quote> fixPriceQuote;
        private final StringProperty baseCurrencyCode;
        private final StringProperty quoteCurrencyCode;
        public boolean hasFocus;
        public final StringProperty quoteCodes = new SimpleStringProperty();

        public PriceModel(MarketPriceService marketPriceService,
                          ObjectProperty<Quote> fixPriceQuote,
                          StringProperty baseCurrencyCode,
                          StringProperty quoteCurrencyCode) {
            this.marketPriceService = marketPriceService;
            this.fixPriceQuote = fixPriceQuote;
            this.baseCurrencyCode = baseCurrencyCode;
            this.quoteCurrencyCode = quoteCurrencyCode;
        }
    }

    public static class PriceView extends View<VBox, PriceModel, PriceController> {
        private final BisqInputTextField fixedPrice;
        private final ChangeListener<String> fixedPriceTextListener;
        private final ChangeListener<Boolean> fixedPriceFocusListener;
        private final ChangeListener<Quote> fixedPriceQuoteListener;
        private final BisqLabel quoteCodes;

        public PriceView(PriceModel model,
                         PriceController controller,
                         PriceValidator validator,
                         String description) {
            super(new VBox(), model, controller);

            fixedPrice = new BisqInputTextField(60);
            fixedPrice.setPromptText(Res.offerbook.get("createOffer.price.fix.prompt"));
            fixedPrice.setMaxWidth(Double.MAX_VALUE);
            fixedPrice.setValidator(validator);

            quoteCodes = new BisqLabel();
            quoteCodes.setMinHeight(42);
            quoteCodes.setFixWidth(100);
            quoteCodes.setAlignment(Pos.CENTER);

            HBox hBox = new HBox();
            hBox.getStyleClass().add("input-with-border");
            HBox.setHgrow(fixedPrice, Priority.ALWAYS);
            hBox.getChildren().addAll(fixedPrice, quoteCodes);

            BisqLabel descriptionLabel = new BisqLabel(description);
            descriptionLabel.setId("input-description-label");
            descriptionLabel.setPrefWidth(190);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(descriptionLabel, hBox);

            //  Listeners on view component events
            fixedPriceFocusListener = (o, old, newValue) -> {
                controller.onFocusChange(newValue);
                controller.onFixPriceInput(fixedPrice.getText());
            };
            fixedPriceTextListener = (o, old, newValue) -> controller.onFixPriceInput(fixedPrice.getText());

            // Listeners on model change
            fixedPriceQuoteListener = (o, old, newValue) -> fixedPrice.setText(QuoteFormatter.format(newValue));
        }

        public void onViewAttached() {
            quoteCodes.textProperty().bind(model.quoteCodes);
            fixedPrice.textProperty().addListener(fixedPriceTextListener);
            fixedPrice.focusedProperty().addListener(fixedPriceFocusListener);
            model.fixPriceQuote.addListener(fixedPriceQuoteListener);
        }

        public void onViewDetached() {
            quoteCodes.textProperty().unbind();
            fixedPrice.textProperty().removeListener(fixedPriceTextListener);
            fixedPrice.focusedProperty().removeListener(fixedPriceFocusListener);
            model.fixPriceQuote.removeListener(fixedPriceQuoteListener);
        }
    }
}