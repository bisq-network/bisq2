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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * A simple component for displaying Bitcoin amounts with formatted sats.
 */
public class BtcSatsText extends VBox {

    // Style options
    public enum Style {
        DEFAULT,
        TEXT_FIELD
    }

    // Properties
    private final StringProperty btcAmount = new SimpleStringProperty("");
    private final StringProperty labelText = new SimpleStringProperty("");
    private final BooleanProperty showBtcCode = new SimpleBooleanProperty(true);

    // UI components
    private final Label label = new Label();
    private final HBox contentBox = new HBox(8);
    private final TextFlow valueTextFlow = new TextFlow();

    // Text parts
    private final Text integerPart = new Text();
    private final Text leadingZeros = new Text();
    private final Text significantDigits = new Text();

    /**
     * Creates a new BtcSatsText with default style.
     */
    public BtcSatsText() {
        this("", null, Style.DEFAULT);
    }

    /**
     * Creates a new BtcSatsText with the specified amount.
     */
    public BtcSatsText(String amount) {
        this(amount, null, Style.DEFAULT);
    }

    /**
     * Creates a new BtcSatsText with the specified configuration.
     */
    public BtcSatsText(String amount, String label, Style style) {
        // Setup
        setSpacing(4);

        // Initialize components
        this.label.getStyleClass().add("bisq-text-3");
        contentBox.setAlignment(Pos.CENTER_LEFT);
        valueTextFlow.getChildren().addAll(integerPart, leadingZeros, significantDigits);
        contentBox.getChildren().add(valueTextFlow);

        // Apply style
        if (style == Style.TEXT_FIELD) {
            setupTextFieldStyle();
        }

        // Add components to the view
        getChildren().addAll(this.label, contentBox);

        // Set initial values
        btcAmount.set(amount);
        if (label != null) {
            labelText.set(label);
            this.label.setText(label);
            this.label.setVisible(true);
            this.label.setManaged(true);
        } else {
            this.label.setVisible(false);
            this.label.setManaged(false);
        }

        // Setup listeners
        btcAmount.addListener((obs, old, newVal) -> updateDisplay());
        labelText.addListener((obs, old, newVal) -> {
            this.label.setText(newVal);
            boolean hasText = newVal != null && !newVal.isEmpty();
            this.label.setVisible(hasText);
            this.label.setManaged(hasText);
        });
        showBtcCode.addListener((obs, old, newVal) -> updateDisplay());

        // Initial update
        updateDisplay();
    }

    /**
     * Updates the display based on current property values.
     */
    private void updateDisplay() {
        String amount = btcAmount.get();
        if (amount == null || amount.isEmpty()) {
            valueTextFlow.setVisible(false);
            return;
        }

        valueTextFlow.setVisible(true);
        formatBtcAmount(amount, showBtcCode.get());
    }

    /**
     * Formats the Bitcoin amount for display.
     */
    private void formatBtcAmount(String amount, boolean showCode) {
        // Ensure we have a decimal point
        if (!amount.contains(".")) {
            amount = amount + ".0";
        }

        // Split into parts
        String[] parts = amount.split("\\.");
        String integerPart = parts[0];
        String fractionalPart = parts.length > 1 ? parts[1] : "";

        // Format fractional part with spaces
        StringBuilder formattedFractional = new StringBuilder();
        for (int i = 0; i < fractionalPart.length(); i++) {
            formattedFractional.append(fractionalPart.charAt(i));
            if ((i + 1) % 3 == 0 && i < fractionalPart.length() - 1) {
                formattedFractional.append(' ');
            }
        }

        // Find leading zeros and significant digits
        StringBuilder leadingZeros = new StringBuilder();
        int i = 0;
        while (i < formattedFractional.length() &&
                (formattedFractional.charAt(i) == '0' || formattedFractional.charAt(i) == ' ')) {
            leadingZeros.append(formattedFractional.charAt(i));
            i++;
        }

        String significantDigits = formattedFractional.substring(i);

        // Set colors based on value
        Color prefixColor = Integer.parseInt(integerPart) > 0 ? Color.WHITE : Color.GRAY;

        // Update text nodes
        this.integerPart.setText(integerPart + ".");
        this.integerPart.setFill(prefixColor);

        this.leadingZeros.setText(leadingZeros.toString());
        this.leadingZeros.setFill(prefixColor);

        this.significantDigits.setText(significantDigits + (showCode ? " BTC" : ""));
        this.significantDigits.setFill(Color.WHITE);
    }

    /**
     * Set up the text field style.
     */
    private void setupTextFieldStyle() {
        contentBox.getStyleClass().add("btc-text-field");
        contentBox.setPadding(new Insets(8, 12, 8, 12));
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
    }

    // Property getters and setters

    public String getBtcAmount() {
        return btcAmount.get();
    }

    public void setBtcAmount(String amount) {
        btcAmount.set(amount);
    }

    public StringProperty btcAmountProperty() {
        return btcAmount;
    }

    public String getLabelText() {
        return labelText.get();
    }

    public void setLabelText(String text) {
        labelText.set(text);
    }

    public StringProperty labelTextProperty() {
        return labelText;
    }

    public boolean getShowBtcCode() {
        return showBtcCode.get();
    }

    public void setShowBtcCode(boolean show) {
        showBtcCode.set(show);
    }

    public BooleanProperty showBtcCodeProperty() {
        return showBtcCode;
    }
}