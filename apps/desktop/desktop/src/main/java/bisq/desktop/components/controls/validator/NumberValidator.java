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
import lombok.Getter;

import java.util.Optional;

public class NumberValidator extends ValidatorBase {

    private static final StringConverter<Number> NUMBER_STRING_CONVERTER = new NumberStringConverter();
    @Getter
    private Optional<Number> minValue = Optional.empty();
    @Getter
    private Optional<Number> maxValue = Optional.empty();
    @Getter
    private Optional<Number> numberValue = Optional.empty();

    public NumberValidator(String message) {
        super(message);
    }

    public NumberValidator(String message, Number minValue, Number maxValue) {
        super(message);
        this.minValue = Optional.of(minValue);
        this.maxValue = Optional.of(maxValue);
    }

    public void setMinValue(Number minValue) {
        this.minValue = Optional.of(minValue);
    }

    public void setMaxValue(Number maxValue) {
        this.maxValue = Optional.of(maxValue);
    }

    @Override
    protected void eval() {
        var textField = (TextInputControl) srcControl.get();
        try {
            Number value = NUMBER_STRING_CONVERTER.fromString(textField.getText());
            numberValue = Optional.of(value);

            if (minValue.isPresent()) {
                if (value instanceof Double) {
                    if (value.doubleValue() < minValue.get().doubleValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Float) {
                    if (value.floatValue() < minValue.get().floatValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Long) {
                    if (value.longValue() < minValue.get().longValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Integer) {
                    if (value.intValue() < minValue.get().intValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Short) {
                    if (value.shortValue() < minValue.get().shortValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Byte) {
                    if (value.byteValue() < minValue.get().byteValue()) {
                        hasErrors.set(true);
                        return;
                    }
                }
            }

            if (maxValue.isPresent()) {
                if (value instanceof Double) {
                    if (value.doubleValue() > maxValue.get().doubleValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Float) {
                    if (value.floatValue() > maxValue.get().floatValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Long) {
                    if (value.longValue() > maxValue.get().longValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Integer) {
                    if (value.intValue() > maxValue.get().intValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Short) {
                    if (value.shortValue() > maxValue.get().shortValue()) {
                        hasErrors.set(true);
                        return;
                    }
                } else if (value instanceof Byte) {
                    if (value.byteValue() > maxValue.get().byteValue()) {
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
}
