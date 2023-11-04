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

package bisq.desktop.components.controls.validator;

import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

public class NumberValidator extends ValidatorBase {

    private static final StringConverter<Number> NUMBER_STRING_CONVERTER = new NumberStringConverter();

    public NumberValidator(String message) {
        super(message);
    }

    @Override
    protected void eval() {
        var textField = (TextInputControl) srcControl.get();
        try {
            NUMBER_STRING_CONVERTER.fromString(textField.getText());
            hasErrors.set(false);
        } catch (Exception e) {
            hasErrors.set(true);
        }
    }
}
