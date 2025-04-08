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

package bisq.desktop.components.controls;

import bisq.common.util.StringUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import lombok.Getter;

import javax.annotation.Nullable;

public class MaterialBitcoinAmountDisplay extends MaterialTextField {
    @Getter
    private final BitcoinAmountDisplay bitcoinAmountDisplay;
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<Boolean> focusListener = this::handleFocusChange;
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<String> textChangeListener = this::handleTextChange;

    public MaterialBitcoinAmountDisplay() {
        this(null, null, null);
    }

    public MaterialBitcoinAmountDisplay(String description) {
        this(description, null, null);
    }

    public MaterialBitcoinAmountDisplay(String description, String prompt) {
        this(description, prompt, null);
    }

    public MaterialBitcoinAmountDisplay(@Nullable String description,
                                        @Nullable String prompt,
                                        @Nullable String help) {
        super(description, prompt, help);

        bitcoinAmountDisplay = new BitcoinAmountDisplay("0");
        configureBitcoinAmountDisplay();
        addBitcoinAmountDisplayToComponent();

        textInputControl.focusedProperty().addListener(new WeakChangeListener<>(focusListener));
        textInputControl.textProperty().addListener(new WeakChangeListener<>(textChangeListener));
        updateInitialText();
        doLayout();
    }

    @Override
    protected void doLayout() {
        super.doLayout();

        if (bitcoinAmountDisplay != null && textInputControl != null) {
            bitcoinAmountDisplay.setLayoutX(textInputControl.getLayoutX());
            bitcoinAmountDisplay.setLayoutY(getFieldLayoutY());
            bitcoinAmountDisplay.setPrefWidth(textInputControl.getPrefWidth());
        }
    }

    private void configureBitcoinAmountDisplay() {
        bitcoinAmountDisplay.setBaselineAlignment();
        bitcoinAmountDisplay.setTranslateY(9);
        bitcoinAmountDisplay.setTranslateX(9);
        bitcoinAmountDisplay.setVisible(false);
        bitcoinAmountDisplay.setManaged(false);
    }

    private void addBitcoinAmountDisplayToComponent() {
        int textFieldIndex = getChildren().indexOf(textInputControl);
        if (textFieldIndex >= 0) {
            getChildren().add(textFieldIndex + 1, bitcoinAmountDisplay);
        } else {
            getChildren().add(bitcoinAmountDisplay);
        }
    }

    private void handleFocusChange(javafx.beans.value.ObservableValue<? extends Boolean> observable,
                                   Boolean oldValue,
                                   Boolean newValue) {
        if (newValue) {
            hideFormattedDisplay();
        } else {
            updateDisplayOnBlur();
        }
    }

    private void handleTextChange(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (!textInputControl.isFocused()) {
            updateDisplayOnBlur();
        }
    }

    private void updateDisplay(boolean hideWhenEmpty) {
        String text = textInputControl.getText();
        if (StringUtils.isNotEmpty(text)) {
            bitcoinAmountDisplay.setBtcAmount(text);
            showFormattedDisplay();
        } else if (hideWhenEmpty) {
            hideFormattedDisplay();
        }
    }

    private void updateDisplayOnBlur() {
        updateDisplay(true); // Hide when empty
    }

    private void updateInitialText() {
        updateDisplay(false); // Do nothing when empty
    }

    private void showFormattedDisplay() {
        bitcoinAmountDisplay.setVisible(true);
        bitcoinAmountDisplay.setManaged(true);
        textInputControl.setOpacity(0);
    }

    private void hideFormattedDisplay() {
        bitcoinAmountDisplay.setVisible(false);
        bitcoinAmountDisplay.setManaged(false);
        textInputControl.setOpacity(1);
    }
}