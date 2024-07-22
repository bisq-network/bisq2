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

package bisq.desktop.main.content.bisq_easy.trade_wizard.payment_methods;

import bisq.desktop.common.utils.ImageUtil;
import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class AddCustomPaymentMethodBox extends HBox {
    private final TextField customPaymentMethodField;
    private final Button addIconButton;
    private final ImageView addIcon;
    private final ChangeListener<Boolean> focusedListener;
    private final String defaultIconId = "add-white";
    private final String activeIconId = "add";

    public AddCustomPaymentMethodBox() {
        customPaymentMethodField = new TextField();
        customPaymentMethodField.setPromptText(Res.get("bisqEasy.tradeWizard.paymentMethod.customMethod.prompt"));
        customPaymentMethodField.setFocusTraversable(false);
        customPaymentMethodField.getStyleClass().add("custom-payment-method-text-field");
        focusedListener = (obs, oldValue, newValue) -> applyStyle(newValue);

        addIcon = ImageUtil.getImageViewById(defaultIconId);
        addIconButton = new Button();
        addIconButton.setGraphic(addIcon);
        addIconButton.getStyleClass().add("custom-payment-method-button");

        getStyleClass().add("add-custom-payment-method-box");
        getChildren().addAll(addIconButton, customPaymentMethodField);
        initialize();
    }

    private void initialize() {
        customPaymentMethodField.focusedProperty().addListener(focusedListener);
    }

    public void dispose() {
        customPaymentMethodField.focusedProperty().removeListener(focusedListener);
    }

    public String getText() {
        return customPaymentMethodField.getText();
    }

    public StringProperty textProperty() {
        return customPaymentMethodField.textProperty();
    }

    private void applyStyle(boolean isFocused) {
        addIcon.setId(isFocused ? activeIconId : defaultIconId);
        addIconButton.setGraphic(addIcon);
    }
}
