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
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.parser.PercentageParser;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PercentageStringConverter extends StringConverter<Number> {
    private final double defaultValue;
    // Percentage value of 100 % is 1.00. Precision is referring to this decimal value, thus a precision of 2 means
    // the percentage value gets rounded. E.g. 0.12 -> 12%
    private int precision = 2;

    public PercentageStringConverter() {
        defaultValue = 0;
    }

    public PercentageStringConverter(int precision) {
        this.precision = precision;
        defaultValue = 0;
    }

    public PercentageStringConverter(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public PercentageStringConverter(double defaultValue, int precision) {
        this.defaultValue = defaultValue;
        this.precision = precision;
    }

    public Number fromString(String value) {
        try {
            return PercentageParser.parse(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String toString(Number numberValue) {
        if (numberValue == null) {
            return "";
        }
        double value = MathUtils.roundDouble(numberValue.doubleValue(), precision);
        if (precision <= 2) {
            return PercentageFormatter.formatToPercentNoDecimalsWithSymbol(value);
        } else {
            return PercentageFormatter.formatToPercentWithSymbol(value);
        }
    }
}
