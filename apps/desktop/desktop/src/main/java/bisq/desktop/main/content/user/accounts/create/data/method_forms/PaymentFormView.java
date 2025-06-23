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

package bisq.desktop.main.content.user.accounts.create.data.method_forms;

import bisq.common.util.StringUtils;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class PaymentFormView extends View<VBox, PaymentFormModel, PaymentFormController> {

    protected final Map<String, Label> errorLabels = new HashMap<>();
    protected final Map<String, ChangeListener<?>> listeners = new HashMap<>();

    protected PaymentFormView(PaymentFormModel model, PaymentFormController controller) {
        super(new VBox(15), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(0, 20, 0, 20));
        root.getStyleClass().add("payment-method-form");

        setupForm();
    }

    public void showValidationErrors(Map<String, String> errors) {
        clearAllErrors();

        Map<String, Runnable> fieldErrorActions = getFieldErrorActions();

        errors.forEach((field, message) -> {
            showFieldError(field, message);
            Optional.ofNullable(fieldErrorActions.get(field)).ifPresent(Runnable::run);
        });
    }

    protected abstract void setupForm();

    protected Map<String, Runnable> getFieldErrorActions() {
        return Map.of();
    }

    protected Label createErrorLabel() {
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("form-error-label");
        errorLabel.setVisible(false);
        return errorLabel;
    }

    protected void showFieldError(String fieldName, String message) {
        Optional.ofNullable(errorLabels.get(fieldName))
                .ifPresent(errorLabel -> {
                    errorLabel.setText(message);
                    errorLabel.setVisible(true);
                });
    }

    protected void clearFieldError(String fieldName) {
        Optional.ofNullable(errorLabels.get(fieldName))
                .ifPresent(errorLabel -> {
                    errorLabel.setText("");
                    errorLabel.setVisible(false);
                });
    }

    protected void clearAllErrors() {
        errorLabels.values().forEach(label -> {
            label.setText("");
            label.setVisible(false);
        });
    }

    protected void setMaterialFieldText(MaterialTextField field, Optional<String> text,
                                        ChangeListener<String> listener) {
        Optional.ofNullable(field)
                .filter(f -> text.map(txt -> !txt.equals(f.getText())).orElse(!StringUtils.isEmpty(f.getText())))
                .ifPresent(f -> {
                    Optional.ofNullable(listener)
                            .ifPresent(l -> f.getTextInputControl().textProperty().removeListener(l));

                    f.setText(text.orElse(""));
                    Optional.ofNullable(listener)
                            .ifPresent(l -> f.getTextInputControl().textProperty().addListener(l));
                });
    }

    protected <T> ChangeListener<T> createWeakListener(ChangeListener<T> listener, String key) {
        registerListener(key, listener);
        return new WeakChangeListener<>(listener);
    }

    protected void registerListener(String key, ChangeListener<?> listener) {
        listeners.put(key, listener);
    }

    protected void cleanupListeners() {
        listeners.clear();
    }
}