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

import bisq.common.locale.LocaleRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import lombok.Getter;

import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

public class BitcoinAmountDisplay extends HBox {
    @Getter
    private final StringProperty btcAmount = new SimpleStringProperty("");
    private final TextFlow valueTextFlow = new TextFlow();
    @Getter
    private final Text integerPart = new Text();
    @Getter
    private final Text leadingZeros = new Text();
    @Getter
    private final Text significantDigits = new Text();
    @Getter
    private final Text btcCode = new Text();
    @SuppressWarnings("FieldCanBeLocal")
    private final ChangeListener<String> amountChangeListener =
            (obs, old, newVal) -> updateDisplay();

    public BitcoinAmountDisplay(String amount, boolean showBtcCode) {
        this(amount);
        btcCode.setVisible(showBtcCode);
    }

    public BitcoinAmountDisplay(String amount) {
        setAlignment(Pos.CENTER);

        valueTextFlow.setTextAlignment(TextAlignment.CENTER);
        getChildren().add(valueTextFlow);

        integerPart.getStyleClass().add("btc-integer-part");
        leadingZeros.getStyleClass().add("btc-leading-zeros-empty");
        significantDigits.getStyleClass().add("btc-significant-digits");
        btcCode.getStyleClass().add("btc-code");

        valueTextFlow.getChildren().addAll(integerPart, leadingZeros, significantDigits, btcCode);

        btcAmount.set(amount);

        btcAmount.addListener(new WeakChangeListener<>(amountChangeListener));

        btcCode.setText(" BTC");
        getStyleClass().add("btc-sats-text");

        updateDisplay();

    }

    public void setBaselineAlignment() {
        setAlignment(Pos.CENTER);
        setSpacing(0);
        valueTextFlow.setLineSpacing(0);
    }

    public void setTextAlignment(TextAlignment alignment) {
        valueTextFlow.setTextAlignment(alignment);
    }

    public void setFixedHeight(double maxHeight) {
        setMinHeight(maxHeight);
        setMaxHeight(maxHeight);
    }

    public void setPaddings(Insets padding) {
        setPadding(padding);
        valueTextFlow.setPadding(new Insets(0));
    }

    public void setBtcAmount(String amount) {
        btcAmount.set(amount);
    }

    public void setFontSize(double fontSize) {
        integerPart.setFont(new Font(integerPart.getFont().getName(), fontSize));
        leadingZeros.setFont(new Font(leadingZeros.getFont().getName(), fontSize));
        significantDigits.setFont(new Font(significantDigits.getFont().getName(), fontSize));
    }

    public void setBtcCodeFontSize(double fontSize) {
        Font btcCodeFont = btcCode.getFont();
        btcCode.setFont(new Font(btcCodeFont.getName(), fontSize));
    }

    public void applyCompactConfig(double mainFontSize, double btcCodeFontSize, double height) {
        setFontSize(mainFontSize);
        setBtcCodeFontSize(btcCodeFontSize);
        setBaselineAlignment();
        setFixedHeight(height);
        setPaddings(new Insets(0));
        setSpacing(0);
    }

    public void applySmallCompactConfig() {
        applyCompactConfig(18, 13, 28);
    }

    public void applyMediumCompactConfig() {
        applyCompactConfig(21, 18, 28);
    }

    public void applyMicroCompactConfig() {
        applyCompactConfig(12, 12, 24);
    }

    private void updateDisplay() {
        String amount = btcAmount.get();
        if (amount == null || amount.isEmpty()) {
            valueTextFlow.setVisible(false);
            return;
        }

        valueTextFlow.setVisible(true);
        formatBtcAmount(amount);
    }

    private void setExclusiveStyle(Text textNode, String styleToAdd, String styleToRemove) {
        textNode.getStyleClass().remove(styleToRemove);
        if (!textNode.getStyleClass().contains(styleToAdd)) {
            textNode.getStyleClass().add(styleToAdd);
        }
    }

    private void formatBtcAmount(String amount) {
        char decimalSeparator =
                DecimalFormatSymbols.getInstance(LocaleRepository.getDefaultLocale()).getDecimalSeparator();

        if (!amount.contains(String.valueOf(decimalSeparator))) {
            amount = amount + decimalSeparator + "0";
        }

        String[] parts = amount.split(Pattern.quote(String.valueOf(decimalSeparator)));

        String integerPartValue = parts.length > 0 ? parts[0] : "";
        if (integerPartValue.isEmpty()) {
            integerPartValue = "0";
        }
        String fractionalPart = parts.length > 1 ? parts[1] : "";
        if (fractionalPart.isEmpty() && amount.contains(String.valueOf(decimalSeparator))) {
            fractionalPart = "0";
        }
        StringBuilder reversedFractional = new StringBuilder(fractionalPart).reverse();

        StringBuilder chunkedReversed = new StringBuilder();
        for (int i = 0; i < reversedFractional.length(); i++) {
            chunkedReversed.append(reversedFractional.charAt(i));
            if ((i + 1) % 3 == 0 && i < reversedFractional.length() - 1) {
                chunkedReversed.append(' ');
            }
        }

        String formattedFractional = chunkedReversed.reverse().toString();

        StringBuilder leadingZerosValue = new StringBuilder();
        int integerValue = Integer.parseInt(integerPartValue);
        int i = 0;
        if (integerValue == 0) {
            while (i < formattedFractional.length() &&
                    (formattedFractional.charAt(i) == '0' || formattedFractional.charAt(i) == ' ')) {
                leadingZerosValue.append(formattedFractional.charAt(i));
                i++;
            }
        }

        String significantDigitsValue = formattedFractional.substring(i);

        if (integerValue > 0) {
            setExclusiveStyle(integerPart, "btc-integer-part", "btc-integer-part-dimmed");
        } else {
            setExclusiveStyle(integerPart, "btc-integer-part-dimmed", "btc-integer-part");
        }
        integerPart.setText(integerPartValue + decimalSeparator);

        leadingZeros.setText(leadingZerosValue.toString());

        if (leadingZerosValue.isEmpty()) {
            setExclusiveStyle(leadingZeros,
                    "btc-leading-zeros-empty", "btc-leading-zeros-dimmed");
        } else {
            setExclusiveStyle(leadingZeros, "btc-leading-zeros-dimmed",
                    "btc-leading-zeros-empty");
        }

        significantDigits.setText(significantDigitsValue);
    }
}