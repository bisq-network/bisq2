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

package bisq.desktop.main.content.bisq_easy.components;

import bisq.desktop.components.controls.MaterialTextField;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PriceInputBox extends MaterialTextField {
    public static final int AMOUNT_BOX_HEIGHT = 127;
    private static final int INPUT_TEXT_MAX_LENGTH = 14;
    private static final String INPUT_TEXT_9_STYLE_CLASS = "input-text-9";
    private static final String INPUT_TEXT_10_STYLE_CLASS = "input-text-10";
    private static final String INPUT_TEXT_11_STYLE_CLASS = "input-text-11";
    private static final String INPUT_TEXT_12_STYLE_CLASS = "input-text-12";
    private static final String INPUT_TEXT_13_STYLE_CLASS = "input-text-13";
    private static final String INPUT_TEXT_14_STYLE_CLASS = "input-text-14";
    private static final String INPUT_TEXT_15_STYLE_CLASS = "input-text-15";
    private static final String INPUT_TEXT_16_STYLE_CLASS = "input-text-16";

    private final Label textInputSymbolLabel, conversionPriceLabel, conversionPriceLabelSymbol;
    private final HBox textInputAndSymbolHBox;
    private final ChangeListener<String> textInputTextListener;

    public PriceInputBox(String description, String prompt, String numericRegex) {
        super(description, prompt);

        bg.getStyleClass().setAll("bisq-dual-amount-bg");

        descriptionLabel.setLayoutX(20);
        descriptionLabel.setPadding(new Insets(2, 0, 0, 0));
        textInputSymbolLabel = new Label();
        textInputSymbolLabel.getStyleClass().add("text-input-symbol");
        textInputAndSymbolHBox = new HBox(10, textInputControl, textInputSymbolLabel);
        textInputAndSymbolHBox.setLayoutY(27);

        conversionPriceLabel = new Label();
        conversionPriceLabel.getStyleClass().add("conversion-price");
        conversionPriceLabelSymbol = new Label();
        conversionPriceLabelSymbol.getStyleClass().add("conversion-price-symbol");
        HBox conversionPriceBox = new HBox(7, conversionPriceLabel, conversionPriceLabelSymbol);
        conversionPriceBox.getStyleClass().add("conversion-price-box");
        conversionPriceBox.setLayoutY(97);

        getChildren().setAll(bg, conversionPriceBox, line, selectionLine, descriptionLabel, textInputAndSymbolHBox, iconButton, helpLabel, errorLabel);
        getStyleClass().add("price-input-box");

        textInputTextListener = (observable, oldValue, newValue) -> {
            if (newValue.length() > INPUT_TEXT_MAX_LENGTH || !newValue.matches(numericRegex)) {
                textInputControl.setText(oldValue);
            }
            // If using an integer we need to count one more char since a dot occupies much less space.
            int calculatedLength = !textInputControl.getText().contains(".")
                    ? textInputControl.getLength() + 1
                    : textInputControl.getLength();
            applyFontStyle(calculatedLength);
        };
        initialize();
    }

    public PriceInputBox(String description, String numericRegex) {
        this(description, null, numericRegex);
    }

    public void initialize() {
        textInputControl.textProperty().addListener(textInputTextListener);
    }

    public void dispose() {
        textInputControl.textProperty().removeListener(textInputTextListener);
    }

    public final StringProperty textInputSymbolTextProperty() {
        return textInputSymbolLabel.textProperty();
    }

    public final StringProperty conversionPriceTextProperty() {
        return conversionPriceLabel.textProperty();
    }

    public final StringProperty conversionPriceSymbolTextProperty() {
        return conversionPriceLabelSymbol.textProperty();
    }

    private void applyFontStyle(int length) {
        textInputAndSymbolHBox.getStyleClass().clear();
        textInputAndSymbolHBox.getStyleClass().addAll("text-input-and-units-box",
                getFontStyleBasedOnTextLength(length));
    }

    @Override
    protected double getBgHeight() {
        return AMOUNT_BOX_HEIGHT;
    }

    private static String getFontStyleBasedOnTextLength(int charCount) {
        if (charCount < 9) {
            return INPUT_TEXT_9_STYLE_CLASS;
        }
        if (charCount == 9) {
            return INPUT_TEXT_10_STYLE_CLASS;
        }
        if (charCount == 10) {
            return INPUT_TEXT_11_STYLE_CLASS;
        }
        if (charCount == 11) {
            return INPUT_TEXT_12_STYLE_CLASS;
        }
        if (charCount == 12) {
            return INPUT_TEXT_13_STYLE_CLASS;
        }
        if (charCount == 13) {
            return INPUT_TEXT_14_STYLE_CLASS;
        }
        if (charCount == 14) {
            return INPUT_TEXT_15_STYLE_CLASS;
        }
        return INPUT_TEXT_16_STYLE_CLASS;
    }
}
