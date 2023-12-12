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

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

public class EqualTextsValidator extends ValidatorBase {

    private final SimpleObjectProperty<Node> other = new SimpleObjectProperty<>();

    public EqualTextsValidator(String message, Node other) {
        super(message);
        this.other.set(other);
    }

    @Override
    protected void eval() {
        var textField = (TextInputControl) srcControl.get();
        var otherTextField = (TextInputControl) other.get();
        hasErrors.set(!textField.getText().equals(otherTextField.getText()));
    }
}
