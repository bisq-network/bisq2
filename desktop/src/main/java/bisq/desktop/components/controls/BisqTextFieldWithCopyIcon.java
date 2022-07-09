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

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;

public class BisqTextFieldWithCopyIcon extends AnchorPane {

    private final StringProperty textProperty = new SimpleStringProperty();
    private final TextField textField;
    private boolean copyWithoutCurrencyPostFix;
    private boolean copyTextAfterDelimiter;

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public BisqTextFieldWithCopyIcon(String text) {
        this(text, null);
    }

    public BisqTextFieldWithCopyIcon(String text, String customStyleClass) {
        textProperty.set(text);
        Label copyIcon = new Label();
        copyIcon.setLayoutY(3);
        copyIcon.getStyleClass().addAll("icon", "highlight");
        copyIcon.setTooltip(new Tooltip(Res.get("copyToClipboard")));
        AwesomeDude.setIcon(copyIcon, AwesomeIcon.COPY);
        copyIcon.setOnMouseClicked(e -> {
            if (text != null && text.length() > 0) {
                String copyText = text;
                if (copyWithoutCurrencyPostFix) {
                    String[] strings = text.split(" ");
                    if (strings.length > 1)
                        copyText = strings[0]; // exclude the BTC postfix
                } else if (copyTextAfterDelimiter) {
                    String[] strings = text.split(" ");
                    if (strings.length > 1)
                        copyText = strings[2]; // exclude the part before / (slash included)
                }
                ClipboardUtil.copyToClipboard(copyText);
            }
        });
        textField = new TextField();
        textField.setEditable(false);
        if (customStyleClass != null) textField.getStyleClass().add(customStyleClass);
        textField.textProperty().bindBidirectional(textProperty);
        AnchorPane.setRightAnchor(copyIcon, 5.0);
        AnchorPane.setRightAnchor(textField, 30.0);
        AnchorPane.setLeftAnchor(textField, 0.0);
        textField.focusTraversableProperty().set(focusTraversableProperty().get());
        getChildren().addAll(textField, copyIcon);
    }

    public void setPromptText(String value) {
        textField.setPromptText(value);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getter/Setter
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getTextProperty() {
        return textProperty.get();
    }

    public StringProperty textPropertyProperty() {
        return textProperty;
    }

    public void setTextProperty(String textProperty) {
        this.textProperty.set(textProperty);
    }

    public void setTooltip(Tooltip toolTip) {
        textField.setTooltip(toolTip);
    }

    public void setCopyWithoutCurrencyPostFix(boolean copyWithoutCurrencyPostFix) {
        this.copyWithoutCurrencyPostFix = copyWithoutCurrencyPostFix;
    }

    public void setCopyTextAfterDelimiter(boolean copyTextAfterDelimiter) {
        this.copyTextAfterDelimiter = copyTextAfterDelimiter;
    }

}
