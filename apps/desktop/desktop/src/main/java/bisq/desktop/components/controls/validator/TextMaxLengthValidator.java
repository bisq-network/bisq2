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

public class TextMaxLengthValidator extends ValidatorBase {
    private static final int DEFAULT_MAX_LENGTH = 100;

    private final int maxLength;

    public TextMaxLengthValidator() {
        this(DEFAULT_MAX_LENGTH);
    }

    public TextMaxLengthValidator(int maxLength) {
        this(Res.get("validation.tooLong", maxLength), maxLength);
    }

    public TextMaxLengthValidator(String message, int maxLength) {
        super(message);
        this.maxLength = maxLength;
    }

    @Override
    protected void eval() {
        // No text is not too long. Skipping it would keep the error of the text before.
        if (srcControl.get() instanceof TextInputControl textInputControl) {
            String text = textInputControl.getText();
            hasErrors.set(text != null && text.length() > maxLength);
        }
    }
}
