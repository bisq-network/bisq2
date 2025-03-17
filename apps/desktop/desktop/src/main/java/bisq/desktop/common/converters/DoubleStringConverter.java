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

package bisq.desktop.common.converters;

import bisq.common.util.MathUtils;
import bisq.presentation.formatters.DefaultNumberFormatter;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoubleStringConverter extends StringConverter<Number> {
    private final double defaultValue;

    public DoubleStringConverter() {
        defaultValue = 0;
    }

    public DoubleStringConverter(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Number fromString(String value) {
        try {
            return MathUtils.parseToDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String toString(Number numberValue) {
        if (numberValue == null) {
            return "";
        }

        return DefaultNumberFormatter.format(numberValue);
    }
}
