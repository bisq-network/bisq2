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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.Getter;

public class BtcSatsText extends VBox {

    public enum Style {
        DEFAULT,
        TEXT_FIELD
    }

    private final StringProperty btcAmount = new SimpleStringProperty("");
    private final StringProperty labelText = new SimpleStringProperty("");
    private final BooleanProperty showBtcCode = new SimpleBooleanProperty(true);

    private final Label label = new Label();
    private final HBox contentBox = new HBox(0);
    private final TextFlow valueTextFlow = new TextFlow();

    private final Text integerPart = new Text();
    private final Text leadingZeros = new Text();

    @Getter
    private final Text significantDigits = new Text();

    @Getter
    private final Text btcCode = new Text();

    public BtcSatsText() {
        this("", null, Style.DEFAULT);
    }

    public BtcSatsText(String amount) {
        this(amount, null, Style.DEFAULT);
    }

    public BtcSatsText(String amount, String label, Style style) {
        setSpacing(0);
        setPadding(new Insets(0));

        this.label.getStyleClass().add("bisq-text-3");
        contentBox.setAlignment(Pos.BASELINE_LEFT);
        valueTextFlow.setLineSpacing(0);
        valueTextFlow.getChildren().addAll(integerPart, leadingZeros, significantDigits, btcCode);
        contentBox.getChildren().add(valueTextFlow);

        if (style == Style.TEXT_FIELD) {
            setupTextFieldStyle();
        }

        if (label != null) {
            labelText.set(label);
            this.label.setText(label);
            this.label.setVisible(true);
            this.label.setManaged(true);
            getChildren().addAll(this.label, contentBox);
        } else {
            this.label.setVisible(false);
            this.label.setManaged(false);
            getChildren().add(contentBox);
        }

        btcAmount.set(amount);

        btcAmount.addListener((obs, old, newVal) -> updateDisplay());
        labelText.addListener((obs, old, newVal) -> {
            this.label.setText(newVal);
            boolean hasText = newVal != null && !newVal.isEmpty();
            this.label.setVisible(hasText);
            this.label.setManaged(hasText);
        });
        showBtcCode.addListener((obs, old, newVal) -> updateDisplay());

        updateDisplay();

        getStyleClass().add("btc-sats-text");

        setAlignment(Pos.BASELINE_LEFT);
        contentBox.setAlignment(Pos.BASELINE_LEFT);
    }

    private void updateDisplay() {
        String amount = btcAmount.get();
        if (amount == null || amount.isEmpty()) {
            valueTextFlow.setVisible(false);
            return;
        }

        valueTextFlow.setVisible(true);
        formatBtcAmount(amount, showBtcCode.get());
    }

    private void formatBtcAmount(String amount, boolean showCode) {
        if (!amount.contains(".")) {
            amount = amount + ".0";
        }

        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String fractionalPart = parts.length > 1 ? parts[1] : "";

        StringBuilder reversedFractional = new StringBuilder(fractionalPart).reverse();

        StringBuilder chunkedReversed = new StringBuilder();
        for (int i = 0; i < reversedFractional.length(); i++) {
            chunkedReversed.append(reversedFractional.charAt(i));
            if ((i + 1) % 3 == 0 && i < reversedFractional.length() - 1) {
                chunkedReversed.append(' ');
            }
        }

        String formattedFractional = chunkedReversed.reverse().toString();

        StringBuilder leadingZeros = new StringBuilder();
        int i = 0;
        while (i < formattedFractional.length() &&
                (formattedFractional.charAt(i) == '0' || formattedFractional.charAt(i) == ' ')) {
            leadingZeros.append(formattedFractional.charAt(i));
            i++;
        }

        String significantDigits = formattedFractional.substring(i);

        this.integerPart.setText(integerPart + ".");
        this.integerPart.setFill(Integer.parseInt(integerPart) > 0 ? Color.WHITE : Color.GRAY);
        this.integerPart.getStyleClass().add("btc-integer-part");

        this.leadingZeros.setText(leadingZeros.toString());
        this.leadingZeros.setFill(Color.GRAY);
        this.leadingZeros.getStyleClass().add("btc-leading-zeros");

        this.significantDigits.setText(significantDigits);
        this.significantDigits.setFill(Color.WHITE);
        this.significantDigits.getStyleClass().add("btc-significant-digits");

        if (showCode) {
            this.btcCode.setText(" BTC");
            this.btcCode.setFill(Color.GRAY);
            this.btcCode.setVisible(true);
            this.btcCode.getStyleClass().add("btc-code");
        } else {
            this.btcCode.setVisible(false);
        }
    }

    public void applyStyleToTextNodes() {
        for (String styleClass : getStyleClass()) {
            if (!styleClass.equals("btc-sats-text") && !styleClass.equals("vbox")) {
                for (Text textNode : new Text[]{integerPart, leadingZeros, significantDigits, btcCode}) {
                    if (!textNode.getStyleClass().contains(styleClass)) {
                        textNode.getStyleClass().add(styleClass);
                    }
                }
            }
        }

        for (String styleClass : getStyleClass()) {
            if (!styleClass.equals("btc-sats-text") && !styleClass.equals("vbox")) {
                if (!contentBox.getStyleClass().contains(styleClass)) {
                    contentBox.getStyleClass().add(styleClass);
                }
                if (!valueTextFlow.getStyleClass().contains(styleClass)) {
                    valueTextFlow.getStyleClass().add(styleClass);
                }
            }
        }
    }

    private void setupTextFieldStyle() {
        contentBox.getStyleClass().add("btc-text-field");
        contentBox.setPadding(new Insets(8, 12, 8, 12));
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
    }

    public void setBaselineAlignment() {
        setAlignment(Pos.BASELINE_LEFT);
        setSpacing(0);
        contentBox.setAlignment(Pos.BASELINE_LEFT);
        valueTextFlow.setLineSpacing(0);
    }

    public void setTextAlignment(TextAlignment alignment) {
        valueTextFlow.setTextAlignment(alignment);
    }

    public void setHeightConstraints(double minHeight, double maxHeight) {
        setMinHeight(minHeight);
        setMaxHeight(maxHeight);
    }

    public void setPaddings(Insets padding) {
        setPadding(padding);
        contentBox.setPadding(new Insets(0));
        valueTextFlow.setPadding(new Insets(0));
    }

    public void setBtcAmount(String amount) {
        btcAmount.set(amount);
    }

    public StringProperty btcAmountProperty() {
        return btcAmount;
    }

    public void setShowBtcCode(boolean show) {
        showBtcCode.set(show);
    }
}