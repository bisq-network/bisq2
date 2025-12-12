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

import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;

public class TextFixLengthValidator extends ValidatorBase {
    private final int length;

    public TextFixLengthValidator(int length) {
        this(Res.get("validation.notCorrectLength", length), length);
    }

    public TextFixLengthValidator(String message, int length) {
        super(message);
        this.length = length;
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl textInputControl && textInputControl.getText()!=null) {
            hasErrors.set(textInputControl.getText().length() != length);
        }
    }
}
