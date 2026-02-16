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

public class TextMinMaxLengthValidator extends ValidatorBase {
    private static final int DEFAULT_MIN_LENGTH = 8;
    private static final int DEFAULT_MAX_LENGTH = 100;

    protected final int minLength;
    protected final int maxLength;

    public TextMinMaxLengthValidator() {
        this(DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }

    public TextMinMaxLengthValidator(String message) {
        this(message, DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }

    public TextMinMaxLengthValidator(int minLength, int maxLength) {
        this(Res.get("validation.tooShortOrTooLong", minLength, maxLength), minLength, maxLength);
    }

    public TextMinMaxLengthValidator(String message, int minLength, int maxLength) {
        super(message);
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl textInputControl) {
            hasErrors.set(textInputControl.getText() == null ||
                    textInputControl.getText().length() > maxLength ||
                    textInputControl.getText().length() < minLength);
        }
    }
}
