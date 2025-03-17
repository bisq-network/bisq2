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

import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import javafx.scene.control.TextInputControl;
import lombok.Getter;

import java.util.Optional;

public class NumberValidator extends ValidatorBase {
    @Getter
    private Optional<Number> minValue;
    @Getter
    private Optional<Number> maxValue;
    @Getter
    private Optional<Number> numberValue = Optional.empty();
    private final boolean allowEmptyString;

    public NumberValidator() {
        this(Res.get("validation.invalidNumber"), false);
    }

    public NumberValidator(String message) {
        this(message, false);
    }

    public NumberValidator(String message, boolean allowEmptyString) {
        this(message, Optional.empty(), Optional.empty(), allowEmptyString);
    }

    public NumberValidator(String message, Number minValue, Number maxValue) {
        this(message, minValue, maxValue, true);
    }

    public NumberValidator(String message, Number minValue, Number maxValue, boolean allowEmptyString) {
        this(message, Optional.of(minValue), Optional.of(maxValue), allowEmptyString);
    }

    private NumberValidator(String message,
                            Optional<Number> minValue,
                            Optional<Number> maxValue,
                            boolean allowEmptyString) {
        super(message);

        this.minValue = minValue;
        this.maxValue = maxValue;
        this.allowEmptyString = allowEmptyString;
    }

    public void setMinValue(Number minValue) {
        this.minValue = Optional.of(minValue);
    }

    public void setMaxValue(Number maxValue) {
        this.maxValue = Optional.of(maxValue);
    }

    @Override
    protected void eval() {
        hasErrors.set(false);
        var textField = (TextInputControl) srcControl.get();
        try {
            String text = textField.getText();
            if (allowEmptyString && StringUtils.isEmpty(text)) {
                return;
            }

            double value = MathUtils.parseToDouble(text);
            numberValue = Optional.of(value);

            if (minValue.isPresent() && value < minValue.get().doubleValue()) {
                hasErrors.set(true);
                return;
            }

            if (maxValue.isPresent() && value > maxValue.get().doubleValue()) {
                hasErrors.set(true);
                return;
            }

            hasErrors.set(false);
        } catch (Exception e) {
            hasErrors.set(true);
        }
    }
}
