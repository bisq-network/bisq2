package bisq.desktop.common.converters;

import bisq.presentation.formatters.DefaultNumberFormatter;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoubleStringConverter extends StringConverter<Number> {
    public DoubleStringConverter() {
    }

    public Number fromString(String value) {
        return DefaultNumberFormatter.parse(value);
    }

    public String toString(Number numberValue) {
        // If the specified value is null, return a zero-length String
        if (numberValue == null) {
            return "";
        }

        return DefaultNumberFormatter.format(numberValue);
    }
}
