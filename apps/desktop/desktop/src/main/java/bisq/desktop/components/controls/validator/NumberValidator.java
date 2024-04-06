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
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Optional;

public class NumberValidator extends ValidatorBase {
    private static final StringConverter<Number> NUMBER_STRING_CONVERTER = new NumberStringConverter();
    @Getter
    private Optional<Number> minValue = Optional.empty();
    @Getter
    private Optional<Number> maxValue = Optional.empty();
    @Getter
    private Optional<Number> numberValue = Optional.empty();
    private final boolean allowEmptyString;

    public NumberValidator(String message) {
        super(message);

        this.allowEmptyString = false;
    }

    public NumberValidator(String message, Number minValue, Number maxValue) {
        this(message, minValue, maxValue, true);
    }

    public NumberValidator(String message, Number minValue, Number maxValue, boolean allowEmptyString) {
        super(message);

        this.minValue = Optional.of(minValue);
        this.maxValue = Optional.of(maxValue);
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

            if (!isValidNumber(text)) {
                hasErrors.set(true);
                return;
            }

            Number value = NUMBER_STRING_CONVERTER.fromString(text);
            numberValue = Optional.of(value);

            if (minValue.isPresent()) {
                if (value instanceof Long) {
                    if (value.longValue() < minValue.get().longValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else {
                    // Fallback to double since the rest of the types can be evaluated within a double
                    if (value.doubleValue() < minValue.get().doubleValue()) {
                        hasErrors.set(true);
                        return;
                    }
                }
            }

            if (maxValue.isPresent()) {
                if (value instanceof Long) {
                    if (value.longValue() > maxValue.get().longValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else {
                    if (value.doubleValue() > maxValue.get().doubleValue()) {
                        hasErrors.set(true);
                        return;
                    }
                }
            }

            hasErrors.set(false);
        } catch (Exception e) {
            hasErrors.set(true);
        }
    }

    private static boolean isValidNumber(String inputText) {
        NumberFormat format = NumberFormat.getNumberInstance();
        try {
            format.parse(inputText);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
