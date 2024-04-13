package bisq.presentation.parser;

import bisq.common.util.StringUtils;
import bisq.presentation.formatters.DefaultNumberFormatter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoubleParser {
    public static double parse(String value) {
        value = DefaultNumberFormatter.reformat(value);
        value = StringUtils.removeAllWhitespaces(value);
        return Double.parseDouble(value);
    }
}
