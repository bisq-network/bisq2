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

import bisq.common.util.StringUtils;
import javafx.scene.control.TextInputControl;

public class OptionalTextMinMaxLengthValidator extends TextMinMaxLengthValidator {
    public OptionalTextMinMaxLengthValidator() {
    }

    public OptionalTextMinMaxLengthValidator(String message) {
        super(message);
    }

    public OptionalTextMinMaxLengthValidator(int minLength, int maxLength) {
        super(minLength, maxLength);
    }

    public OptionalTextMinMaxLengthValidator(String message, int minLength, int maxLength) {
        super(message, minLength, maxLength);
    }

    @Override
    protected void eval() {
        if (srcControl.get() instanceof TextInputControl textInputControl) {
            String text = textInputControl.getText();
            if (StringUtils.isEmpty(text)) {
                hasErrors.set(false);
                return;
            }
            int length = text.length();
            hasErrors.set(length > maxLength || length < minLength);
        }
    }
}
