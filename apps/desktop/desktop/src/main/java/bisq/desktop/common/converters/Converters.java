package bisq.desktop.common.converters;

import javafx.util.StringConverter;

public class Converters {
    public static final StringConverter<Number> DOUBLE_STRING_CONVERTER = new DoubleStringConverter();
    public static final StringConverter<Number> LONG_STRING_CONVERTER = new LongStringConverter();
}
