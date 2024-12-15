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
    public final static int AMOUNT_BOX_WIDTH = 340;
    public final static int AMOUNT_BOX_HEIGHT = 127;
    private final static String INPUT_TEXT_9_STYLE_CLASS = "input-text-9";
    private final static String INPUT_TEXT_10_STYLE_CLASS = "input-text-10";
    private final static String INPUT_TEXT_11_STYLE_CLASS = "input-text-11";
    private final static String INPUT_TEXT_12_STYLE_CLASS = "input-text-12";
    private final static String INPUT_TEXT_13_STYLE_CLASS = "input-text-13";
    private final static String INPUT_TEXT_14_STYLE_CLASS = "input-text-14";

    private final Label textInputUnitsLabel, conversionPriceLabel, conversionPriceUnitsLabel;
    private final HBox textInputAndUnitsHBox;
    private final ChangeListener<Number> textInputLengthListener;

    public PriceInputBox(String description, String prompt) {
        super(description, prompt);

        bg.getStyleClass().setAll("bisq-dual-amount-bg");

        descriptionLabel.setLayoutX(20);
        descriptionLabel.setPadding(new Insets(2, 0, 0, 0));
        textInputUnitsLabel = new Label();
        textInputUnitsLabel.getStyleClass().add("text-input-units");
        textInputAndUnitsHBox = new HBox(10, textInputControl, textInputUnitsLabel);
        textInputAndUnitsHBox.setLayoutY(27);

        conversionPriceLabel = new Label("test");
        conversionPriceUnitsLabel = new Label("123");
        HBox conversionPriceBox = new HBox(10, conversionPriceLabel, conversionPriceUnitsLabel);
        conversionPriceBox.setLayoutY(100);

        getChildren().setAll(bg, conversionPriceBox, line, selectionLine, descriptionLabel, textInputAndUnitsHBox, iconButton, helpLabel, errorLabel);
        getStyleClass().add("price-input-box");

        textInputLengthListener = (observable, oldValue, newValue) -> applyFontStyle(newValue.intValue());

        initialize();
    }

    void initialize() {
        textInputControl.lengthProperty().addListener(textInputLengthListener);
    }

    void dispose() {
        textInputControl.lengthProperty().removeListener(textInputLengthListener);
    }

    final StringProperty textInputUnitsLabelTextProperty() {
        return textInputUnitsLabel.textProperty();
    }

    void setConversionPriceLabel(String conversionPriceLabel) {
        this.conversionPriceLabel.setText(conversionPriceLabel);
    }

    void setConversionPriceUnitsLabel(String conversionPriceUnitsLabel) {
        this.conversionPriceUnitsLabel.setText(conversionPriceUnitsLabel.toUpperCase());
    }

    private void applyFontStyle(int length) {
        textInputAndUnitsHBox.getStyleClass().clear();
        textInputAndUnitsHBox.getStyleClass().addAll("text-input-and-units-box",
                getFontStyleBasedOnTextLength(length));
    }

    @Override
    protected double getBgHeight() {
        return AMOUNT_BOX_HEIGHT;
    }

    @Override
    protected void doLayout() {
        super.doLayout();

        setMinWidth(AMOUNT_BOX_WIDTH);
        setMaxWidth(AMOUNT_BOX_WIDTH);
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
        return INPUT_TEXT_14_STYLE_CLASS;
    }
}
