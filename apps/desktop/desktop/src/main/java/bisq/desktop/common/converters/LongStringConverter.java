package bisq.desktop.common.converters;

import bisq.common.util.MathUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LongStringConverter extends DoubleStringConverter {
    public Number fromString(String value) {
        return MathUtils.roundDoubleToLong(super.fromString(value).doubleValue());
    }
}
