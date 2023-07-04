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


import bisq.desktop.common.utils.validation.InputValidator;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;

//TODO Add validation support
public class BisqInputTextField extends TextField {

    public BisqInputTextField() {
        super();

        textProperty().addListener((o, oldValue, newValue) -> refreshValidation());

        focusedProperty().addListener((o, oldValue, newValue) -> {
            if (validator != null) {
                if (!oldValue && newValue) {
                    this.validationResult.set(new InputValidator.ValidationResult(true));
                } else {
                    this.validationResult.set(validator.validate(getText()));
                }
            }
        });
    }


    public void resetValidation() {
        String input = getText();
        if (input.isEmpty()) {
            validationResult.set(new InputValidator.ValidationResult(true));
        } else {
            validationResult.set(validator.validate(input));
        }
    }

    public void refreshValidation() {
        if (validator != null) {
            this.validationResult.set(validator.validate(getText()));
        }
    }

    public ObjectProperty<InputValidator.ValidationResult> validationResultProperty() {
        return validationResult;
    }

    private final ObjectProperty<InputValidator.ValidationResult> validationResult = new SimpleObjectProperty<>
            (new InputValidator.ValidationResult(true));

    private InputValidator validator;
}
